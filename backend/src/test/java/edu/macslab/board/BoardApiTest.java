package edu.macslab.board;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class BoardApiTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired UserRepository users;
 @Autowired PostRepository posts;
 @Autowired PasswordEncoder passwords;
 @Autowired JwtEncoder encoder;
 final String password="PracticeOnly123!";
 @BeforeEach void clear() { posts.deleteAll(); users.deleteAll(); }
 String body(Object value) throws Exception { return json.writeValueAsString(value); }
 JsonNode read(MvcResult r) throws Exception { return json.readTree(r.getResponse().getContentAsString()); }
 Map<String,String> signupData(String email) { return Map.of("name","Alex","email",email,"password",password); }
 void signup(String email) throws Exception {
  mvc.perform(post("/api/auth/signup").contentType("application/json").content(body(signupData(email))))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist());
 }
 String login(String email) throws Exception {
  return read(mvc.perform(post("/api/auth/login").contentType("application/json")
   .content(body(Map.of("email",email,"password",password)))).andExpect(status().isOk()).andReturn()).get("accessToken").asText();
 }
 String account(String email) throws Exception { signup(email); return login(email); }
 String create(String token) throws Exception {
  return read(mvc.perform(post("/api/posts").header("Authorization","Bearer "+token).contentType("application/json")
   .content(body(Map.of("title","Study group","content","Library at 3 PM"))))
   .andExpect(status().isCreated()).andReturn()).get("id").asText();
 }
 @Test void passwordsAreHashedAndDuplicateEmailsConflict() throws Exception {
  signup("alex@example.com");var u=users.findByEmail("alex@example.com").orElseThrow();
  assertNotEquals(password,u.passwordHash);assertTrue(passwords.matches(password,u.passwordHash));
  mvc.perform(post("/api/auth/signup").contentType("application/json").content(body(signupData("ALEX@example.com"))))
   .andExpect(status().isConflict());
 }
 @Test void wrongPasswordFailsAndValidTokenIdentifiesUser() throws Exception {
  String token=account("alex@example.com");
  mvc.perform(post("/api/auth/login").contentType("application/json").content(body(Map.of("email","alex@example.com","password","incorrect"))))
   .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  mvc.perform(get("/api/me").header("Authorization","Bearer "+token)).andExpect(status().isOk())
   .andExpect(jsonPath("$.email").value("alex@example.com"));
 }
 @Test void anonymousReadAllowedButWritesAndMeRequireToken() throws Exception {
  mvc.perform(get("/api/posts")).andExpect(status().isOk());
  mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/posts").contentType("application/json").content(body(Map.of("title","x","content","y"))))
   .andExpect(status().isUnauthorized());
 }
 @Test void ownPostCanBeReadEditedAndDeleted() throws Exception {
  String token=account("alex@example.com"), id=create(token);
  mvc.perform(get("/api/posts/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Study group"));
  mvc.perform(put("/api/posts/"+id).header("Authorization","Bearer "+token).contentType("application/json")
   .content(body(Map.of("title","New title","content","Updated content")))).andExpect(status().isOk())
   .andExpect(jsonPath("$.title").value("New title"));
  mvc.perform(delete("/api/posts/"+id).header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
  mvc.perform(get("/api/posts/"+id)).andExpect(status().isNotFound());
 }
 @Test void otherAccountCannotEditOrDeleteEvenByCallingApiDirectly() throws Exception {
  String a=account("a@example.com"), id=create(a), b=account("b@example.com");
  mvc.perform(put("/api/posts/"+id).header("Authorization","Bearer "+b).contentType("application/json")
   .content(body(Map.of("title","Tampered","content","No")))).andExpect(status().isForbidden());
  mvc.perform(delete("/api/posts/"+id).header("Authorization","Bearer "+b)).andExpect(status().isForbidden());
  mvc.perform(get("/api/posts/"+id)).andExpect(jsonPath("$.title").value("Study group"));
 }
 @Test void ownerIdCannotBeInjectedAndInvalidInputIsRejected() throws Exception {
  String token=account("alex@example.com");
  mvc.perform(post("/api/posts").header("Authorization","Bearer "+token).contentType("application/json")
   .content(body(Map.of("title","x","content","y","ownerId",UUID.randomUUID())))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/posts").header("Authorization","Bearer "+token).contentType("application/json")
   .content(body(Map.of("title"," ","content","y")))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/posts/not-a-uuid")).andExpect(status().isBadRequest());
 }
 @Test void invalidSignatureExpiredTokenAndWrongIssuerFail() throws Exception {
  String token=account("alex@example.com");
  String[] bits=token.split("\\.");
  String bad=bits[0]+"."+bits[1]+"."+(bits[2].startsWith("A")?"B":"A")+bits[2].substring(1);
  mvc.perform(get("/api/me").header("Authorization","Bearer "+bad)).andExpect(status().isUnauthorized());
  String id=users.findByEmail("alex@example.com").orElseThrow().id.toString();
  for(String issuer:List.of("campus-board","wrong-issuer")) {
   Instant now=Instant.now();var c=JwtClaimsSet.builder().issuer(issuer).subject(id)
    .issuedAt(now.minusSeconds(3600)).expiresAt(issuer.equals("campus-board")?now.minusSeconds(120):now.plusSeconds(1800)).build();
   String signed=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),c)).getTokenValue();
   mvc.perform(get("/api/me").header("Authorization","Bearer "+signed)).andExpect(status().isUnauthorized());
  }
 }
 @Test void passwordByteLimitAndShortPasswordsFail() throws Exception {
  for(String p:List.of("short","界".repeat(25))) {
   mvc.perform(post("/api/auth/signup").contentType("application/json")
    .content(body(Map.of("name","Alex","email","alex@example.com","password",p))))
    .andExpect(status().isBadRequest());
  }
 }
 @Test void healthAndApiSpecificationAreAvailable() throws Exception {
  mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
  mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
   .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
 }
}
