package io.openfactory.api.inbox.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.Workpack;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inbox")
public class InboxItem extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    public User user;

    @ManyToOne
    @JoinColumn(name = "workpack_id")
    @JsonIgnore
    public Workpack workpack;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public InboxType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String message;

    public boolean read = false;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @JsonProperty("userId")
    public UUID getUserId() { return user != null ? user.id : null; }

    @JsonProperty("workpackId")
    public UUID getWorkpackId() { return workpack != null ? workpack.id : null; }

    public static List<InboxItem> findByUser(UUID userId) {
        return list("user.id = ?1 order by createdAt desc", userId);
    }

    public static List<InboxItem> findUnreadByUser(UUID userId) {
        return list("user.id = ?1 and read = false order by createdAt desc", userId);
    }

    public static long markAllRead(UUID userId) {
        return update("read = true where user.id = ?1 and read = false", userId);
    }
}
