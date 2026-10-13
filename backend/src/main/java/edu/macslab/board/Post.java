package edu.macslab.board;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="post")
public class Post {
    @Id UUID id;
    @Column(name="owner_id", nullable=false) UUID ownerId;
    @Column(nullable=false, length=120) String title;
    @Column(nullable=false, length=4000) String content;
    @Column(name="created_at", nullable=false) Instant createdAt;
    @Column(name="updated_at", nullable=false) Instant updatedAt;
    @Version long version;
    protected Post() {}
    Post(UUID ownerId, String title, String content) {
        this.id=UUID.randomUUID(); this.ownerId=ownerId;
        this.title=title; this.content=content;
        this.createdAt=Instant.now(); this.updatedAt=this.createdAt;
    }
    void edit(String title, String content) {
        this.title=title; this.content=content; this.updatedAt=Instant.now();
    }
}
