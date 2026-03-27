package io.openfactory.api.attachment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.Workpack;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "attachments")
public class Attachment extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "workpack_id", nullable = false)
    @JsonIgnore
    public Workpack workpack;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    public User user;

    @Column(name = "file_name", nullable = false)
    public String fileName;

    @Column(name = "file_type", nullable = false)
    public String fileType;

    @Column(name = "storage_url", nullable = false)
    public String storageUrl;

    @Column(name = "content_text", columnDefinition = "TEXT")
    public String contentText;

    @Column(name = "created_at")
    public OffsetDateTime createdAt = OffsetDateTime.now();

    @JsonProperty("workpackId")
    public UUID getWorkpackId() { return workpack != null ? workpack.id : null; }

    @JsonProperty("userId")
    public UUID getUserId() { return user != null ? user.id : null; }

    public static List<Attachment> findByWorkpack(UUID workpackId) {
        return list("workpack.id = ?1 order by createdAt asc", workpackId);
    }
}
