package io.openfactory.api.workpack.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openfactory.api.user.model.User;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workpack_members")
public class WorkpackMember extends PanacheEntityBase {

    @EmbeddedId
    public WorkpackMemberId id;

    @ManyToOne
    @MapsId("workpackId")
    @JoinColumn(name = "workpack_id")
    @JsonIgnore
    public Workpack workpack;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @JsonIgnore
    public User user;

    @Enumerated(EnumType.STRING)
    public MemberRole role = MemberRole.EDITOR;

    @Column(name = "joined_at")
    public LocalDateTime joinedAt = LocalDateTime.now();

    @JsonProperty("workpackId")
    public UUID getWorkpackId() { return id != null ? id.workpackId : null; }

    @JsonProperty("userId")
    public UUID getUserId() { return id != null ? id.userId : null; }

    @JsonProperty("userEmail")
    public String getUserEmail() { return user != null ? user.email : null; }

    @JsonProperty("userName")
    public String getUserName() { return user != null ? user.name : null; }

    public static List<WorkpackMember> findByWorkpack(UUID workpackId) {
        return list("id.workpackId", workpackId);
    }

    public static WorkpackMember findByWorkpackAndUser(UUID workpackId, UUID userId) {
        return find("id.workpackId = ?1 and id.userId = ?2", workpackId, userId).firstResult();
    }
}
