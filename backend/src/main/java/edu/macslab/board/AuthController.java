package edu.macslab.board;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import static edu.macslab.board.ApiModels.*;
import java.util.UUID;
@RestController @RequestMapping("/api")
public class AuthController {
    private final AuthService auth;
    AuthController(AuthService auth) { this.auth=auth; }
    @PostMapping("/auth/signup") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Create an account")
    public UserView signup(@Valid @RequestBody Signup input) { return auth.signup(input); }
    @PostMapping("/auth/login") @Operation(summary="Check credentials and issue a token")
    public LoginResult login(@Valid @RequestBody Login input) { return auth.login(input); }
    @GetMapping("/me") @SecurityRequirement(name="bearerAuth") @Operation(summary="Read the authenticated user")
    public UserView me(@AuthenticationPrincipal Jwt jwt) {
        return auth.current(UUID.fromString(jwt.getSubject()));
    }
}
