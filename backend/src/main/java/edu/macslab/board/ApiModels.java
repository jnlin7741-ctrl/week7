package edu.macslab.board;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public final class ApiModels {
    private ApiModels() {}
    public record Signup(@NotBlank @Size(max=60) String name,
                         @NotBlank @Email @Size(max=254) String email,
                         @NotBlank @Size(min=12,max=72) String password) {}
    public record Login(@NotBlank @Email @Size(max=254) String email,
                        @NotBlank @Size(max=72) String password) {}
    public record UserView(UUID id, String name, String email) {
        static UserView of(AppUser u) { return new UserView(u.id,u.name,u.email); }
    }
    public record LoginResult(String accessToken, String tokenType, long expiresIn, UserView user) {}
    // ownerId is deliberately absent. The server reads it from the verified JWT.
    public record PostInput(@NotBlank @Size(max=120) String title,
                            @NotBlank @Size(max=4000) String content) {}
    public record PostView(UUID id, UUID ownerId, String title, String content,
                           Instant createdAt, Instant updatedAt) {
        static PostView of(Post p) { return new PostView(p.id,p.ownerId,p.title,p.content,p.createdAt,p.updatedAt); }
    }
    public record ErrorView(String code, String message) {}
}
