package io.openfactory.api.handoff.model;

import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.Workpack;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
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
    public Workpack workpack;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    public User owner;

    @Column(name = "intended_executor")
    public String intendedExecutor;

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
    public User approvedBy;

    @Column(name = "approved_at")
    public LocalDateTime approvedAt;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    public static Handoff findByWorkpack(UUID workpackId) {
        return find("workpack.id", workpackId).firstResult();
    }
}
