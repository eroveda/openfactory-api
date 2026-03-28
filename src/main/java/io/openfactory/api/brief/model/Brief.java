package io.openfactory.api.brief.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.openfactory.api.workpack.model.Workpack;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public String actors = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope_includes", columnDefinition = "jsonb")
    public String scopeIncludes = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scope_excludes", columnDefinition = "jsonb")
    public String scopeExcludes = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public String constraints = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "success_criteria", columnDefinition = "jsonb")
    public String successCriteria = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "domain_facts", columnDefinition = "jsonb")
    public String domainFacts = "[]";

    @JsonRawValue
    @JdbcTypeCode(SqlTypes.JSON)
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
