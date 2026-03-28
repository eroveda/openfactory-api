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
@Table(name = "workpacks")
public class Workpack extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(nullable = false)
    public String title;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    public User owner;

    @JsonProperty("owner")
    public OwnerView getOwnerView() {
        if (owner == null) return null;
        return new OwnerView(owner.id, owner.name, owner.email, owner.avatarUrl);
    }

    public record OwnerView(UUID id, String name, String email, String avatarUrl) {}

    @Enumerated(EnumType.STRING)
    public WorkpackStage stage = WorkpackStage.RAW;

    /** ExecutionPlan serializado como JSON — poblado por core-lib */
    @Column(name = "execution_plan", columnDefinition = "TEXT")
    public String executionPlan;

    /** Contenido original ingresado por el usuario — necesario para reshape */
    @Column(name = "source_content", columnDefinition = "TEXT")
    @JsonIgnore
    public String sourceContent;

    @Column(name = "step_count")
    public Integer stepCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status")
    public ProcessingStatus processingStatus = ProcessingStatus.DONE;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    public String failureReason;

    @Column(name = "pipeline_step", columnDefinition = "TEXT")
    public String pipelineStep;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();

    public static List<Workpack> findByOwner(UUID ownerId) {
        return list("owner.id", ownerId);
    }

    public static List<Workpack> findSharedWith(UUID userId) {
        return list(
            "id IN (SELECT wm.id.workpackId FROM WorkpackMember wm WHERE wm.id.userId = ?1)",
            userId);
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
