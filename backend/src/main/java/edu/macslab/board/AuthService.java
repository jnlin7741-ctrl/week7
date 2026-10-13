package edu.macslab.board;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import static org.springframework.http.HttpStatus.*;
import static edu.macslab.board.ApiModels.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String issuer;
    private final long ttl;
    private final String dummyHash;
    AuthService(UserRepository users,PasswordEncoder passwords,JwtEncoder encoder,
                @Value("${app.jwt.issuer}") String issuer,@Value("${app.jwt.ttl-seconds}") long ttl) {
        this.users=users;this.passwords=passwords;this.encoder=encoder;this.issuer=issuer;this.ttl=ttl;
        this.dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    private String normalizedEmail(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    private void validatePassword(String password) {
        // BCrypt has a 72-byte input limit, which differs from 72 characters.
        if (password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new ApiException(BAD_REQUEST,"INVALID_INPUT","Use a password of at most 72 UTF-8 bytes.");
    }
    private String encodePassword(String password) {
        return passwords.encode(password);
    }
    @Transactional
    public UserView signup(Signup input) {
        validatePassword(input.password());
        String email=normalizedEmail(input.email());
        if(users.findByEmail(email).isPresent()) throw new ApiException(CONFLICT,"EMAIL_EXISTS","This email is already registered.");
        // Store a password hash. Never store the original password.
        String hash=encodePassword(input.password());
        return UserView.of(users.saveAndFlush(new AppUser(input.name().strip(),email,hash)));
    }
    @Transactional(readOnly=true)
    public LoginResult login(Login input) {
        validatePassword(input.password());
        var found=users.findByEmail(normalizedEmail(input.email()));
        // A missing email still goes through the password comparison step.
        boolean matches=passwords.matches(input.password(),found.map(u->u.passwordHash).orElse(dummyHash));
        if(found.isEmpty() || !matches) throw new ApiException(UNAUTHORIZED,"INVALID_CREDENTIALS","Email or password is incorrect.");
        AppUser user=found.get(); Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer(issuer).subject(user.id.toString())
            .issuedAt(now).expiresAt(now.plusSeconds(ttl)).build();
        String token=encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new LoginResult(token,"Bearer",ttl,UserView.of(user));
    }
    @Transactional(readOnly=true)
    public UserView current(UUID id) {
        return UserView.of(users.findById(id).orElseThrow(()->
            new ApiException(UNAUTHORIZED,"UNAUTHENTICATED","This account no longer exists.")));
    }
}
