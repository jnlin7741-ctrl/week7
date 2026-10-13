package edu.macslab.board;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import static edu.macslab.board.ApiModels.*;
import java.util.*;
@RestController @RequestMapping("/api/posts")
public class PostController {
    private final PostService posts;
    PostController(PostService posts) { this.posts=posts; }
    @GetMapping @Operation(summary="Read the latest 50 public posts")
    public List<PostView> list() { return posts.list(); }
    @GetMapping("/{id}") @Operation(summary="Read one public post")
    public PostView get(@PathVariable UUID id) { return posts.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @SecurityRequirement(name="bearerAuth")
    @Operation(summary="Create a post as the signed-in user")
    public PostView create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody PostInput input) {
        return posts.create(UUID.fromString(jwt.getSubject()),input);
    }
    @PutMapping("/{id}") @SecurityRequirement(name="bearerAuth") @Operation(summary="Edit your own post")
    public PostView update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody PostInput input) {
        return posts.update(UUID.fromString(jwt.getSubject()),id,input);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @SecurityRequirement(name="bearerAuth")
    @Operation(summary="Delete your own post")
    public void delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        posts.delete(UUID.fromString(jwt.getSubject()),id);
    }
}
