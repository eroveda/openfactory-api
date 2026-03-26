package io.openfactory.api.plan.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openfactory.api.workpack.model.Workpack;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "execution_plans")
public class ExecutionPlanEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "workpack_id", nullable = false)
    @JsonIgnore
    public Workpack workpack;

    @JsonProperty("workpackId")
    public UUID getWorkpackId() { return workpack != null ? workpack.id : null; }

    public String version = "1.0";

    @Enumerated(EnumType.STRING)
    public PlanStatus status = PlanStatus.VALID;

    @Column(columnDefinition = "jsonb")
    public String steps = "[]";

    @Column(name = "weak_dependencies", columnDefinition = "jsonb")
    public String weakDependencies = "[]";

    @Column(columnDefinition = "jsonb")
    public String findings = "[]";

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    public static ExecutionPlanEntity findByWorkpack(UUID workpackId) {
        return find("workpack.id", workpackId).firstResult();
    }
}
