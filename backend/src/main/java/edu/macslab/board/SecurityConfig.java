package edu.macslab.board;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.Base64;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import java.util.UUID;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
    @Bean SecretKey jwtKey(@Value("${app.jwt.secret:}") String configured, Environment env) {
        byte[] bytes;
        if (configured.isBlank()) {
            if (!env.acceptsProfiles(Profiles.of("local","test")))
                throw new IllegalStateException("JWT_SECRET must be set outside local/test.");
            bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        } else {
            try { bytes=Base64.getDecoder().decode(configured); }
            catch (IllegalArgumentException e) { throw new IllegalStateException("JWT_SECRET must be Base64."); }
            if (bytes.length<32) throw new IllegalStateException("JWT_SECRET requires at least 32 random bytes.");
        }
        return new SecretKeySpec(bytes,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKey key,@Value("${app.jwt.issuer}") String issuer) {
        var decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> shape=jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                if (jwt.getExpiresAt()==null) throw new IllegalArgumentException();
                return OAuth2TokenValidatorResult.success();
            } catch (Exception e) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),shape));
        return decoder;
    }
    @Bean SecurityFilterChain apiSecurity(HttpSecurity http,ObjectMapper json) throws Exception {
        var unauthenticated=(org.springframework.security.web.AuthenticationEntryPoint)(req,res,error)->{
            res.setStatus(401);res.setContentType("application/json");
            res.setHeader("WWW-Authenticate","Bearer");
            json.writeValue(res.getOutputStream(),new ApiModels.ErrorView("UNAUTHENTICATED","Sign in with a valid, unexpired token."));
        };
        var forbidden=(org.springframework.security.web.access.AccessDeniedHandler)(req,res,error)->{
            res.setStatus(403);res.setContentType("application/json");
            json.writeValue(res.getOutputStream(),new ApiModels.ErrorView("FORBIDDEN","You cannot perform this action."));
        };
        return http
            // This API uses an Authorization header, never a session cookie.
            .csrf(csrf->csrf.disable())
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(r->r
                .requestMatchers(HttpMethod.POST,"/api/auth/signup","/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/posts","/api/posts/*").permitAll()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/actuator/health","/actuator/health/**","/","/index.html","/assets/**",
                    "/favicon.ico","/swagger-ui.html","/swagger-ui/**","/v3/api-docs/**","/error").permitAll()
                .anyRequest().denyAll())
            .oauth2ResourceServer(o->o.jwt(j->{}).authenticationEntryPoint(unauthenticated).accessDeniedHandler(forbidden))
            .exceptionHandling(e->e.authenticationEntryPoint(unauthenticated).accessDeniedHandler(forbidden))
            .build();
    }
}
