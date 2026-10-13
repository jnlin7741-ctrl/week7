package edu.macslab.board;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.HttpStatus.*;
import static edu.macslab.board.ApiModels.*;
import java.util.*;
@Service
public class PostService {
    private final PostRepository posts;
    private final AuthService auth;
    PostService(PostRepository posts,AuthService auth) { this.posts=posts;this.auth=auth; }
    private Post existing(UUID id) {
        return posts.findById(id).orElseThrow(()->new ApiException(NOT_FOUND,"NOT_FOUND","This post does not exist."));
    }
    private void requireOwner(Post post,UUID userId) {
        // Compare the saved owner with the verified request identity.
        if(!post.ownerId.equals(userId))
            throw new ApiException(FORBIDDEN,"FORBIDDEN","Only the author can change this post.");
    }
    @Transactional(readOnly=true)
    public List<PostView> list() { return posts.findTop50ByOrderByCreatedAtDesc().stream().map(PostView::of).toList(); }
    @Transactional(readOnly=true)
    public PostView get(UUID id) { return PostView.of(existing(id)); }
    @Transactional
    public PostView create(UUID userId,PostInput input) {
        auth.current(userId);
        return PostView.of(posts.saveAndFlush(new Post(userId,input.title().strip(),input.content().strip())));
    }
    @Transactional
    public PostView update(UUID userId,UUID id,PostInput input) {
        auth.current(userId);Post post=existing(id);requireOwner(post,userId);
        post.edit(input.title().strip(),input.content().strip());
        return PostView.of(posts.saveAndFlush(post));
    }
    @Transactional
    public void delete(UUID userId,UUID id) {
        auth.current(userId);Post post=existing(id);requireOwner(post,userId);
        posts.delete(post);
    }
}
