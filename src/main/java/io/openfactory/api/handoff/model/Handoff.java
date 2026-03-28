package io.openfactory.api.handoff.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.Workpack;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "handoffs")
public class Handoff extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "workpack_id", nullable = false)
    @JsonIgnore
    public Workpack workpack;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    public User owner;

    @JsonProperty("workpackId")
    public UUID getWorkpackId() { return workpack != null ? workpack.id : null; }

    @JsonProperty("ownerId")
    public UUID getOwnerId() { return owner != null ? owner.id : null; }

    @Column(name = "intended_executor")
    public String intendedExecutor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public String assumptions = "[]";

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    public ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "handoff_notes", columnDefinition = "TEXT")
    public String handoffNotes;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    public String reviewNotes;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    @JsonIgnore
    public User approvedBy;

    @JsonProperty("approvedById")
    public UUID getApprovedById() { return approvedBy != null ? approvedBy.id : null; }

    @Column(name = "approved_at")
    public LocalDateTime approvedAt;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    public static Handoff findByWorkpack(UUID workpackId) {
        return find("workpack.id", workpackId).firstResult();
    }
}
