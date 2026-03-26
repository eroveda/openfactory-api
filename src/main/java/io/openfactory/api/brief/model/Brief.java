package io.openfactory.api.brief.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openfactory.api.workpack.model.Workpack;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "briefs")
public class Brief extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "workpack_id", nullable = false)
    @JsonIgnore
    public Workpack workpack;

    @JsonProperty("workpackId")
    public UUID getWorkpackId() { return workpack != null ? workpack.id : null; }

    public String title;

    @Column(name = "main_idea", columnDefinition = "TEXT")
    public String mainIdea;

    @Column(columnDefinition = "TEXT")
    public String objective;

    @Column(columnDefinition = "jsonb")
    public String actors = "[]";

    @Column(name = "scope_includes", columnDefinition = "jsonb")
    public String scopeIncludes = "[]";

    @Column(name = "scope_excludes", columnDefinition = "jsonb")
    public String scopeExcludes = "[]";

    @Column(columnDefinition = "jsonb")
    public String constraints = "[]";

    @Column(name = "success_criteria", columnDefinition = "jsonb")
    public String successCriteria = "[]";

    @Column(name = "domain_facts", columnDefinition = "jsonb")
    public String domainFacts = "[]";

    @Column(name = "readiness_signals", columnDefinition = "jsonb")
    public String readinessSignals = "{}";

    @Enumerated(EnumType.STRING)
    public BriefStatus status = BriefStatus.DRAFT;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();

    public static Brief findByWorkpack(UUID workpackId) {
        return find("workpack.id", workpackId).firstResult();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
