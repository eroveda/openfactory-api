package io.openfactory.api.box.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.openfactory.api.workpack.model.Workpack;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "boxes")
public class Box extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne
    @JoinColumn(name = "workpack_id", nullable = false)
    @JsonIgnore
    public Workpack workpack;

    @JsonProperty("workpackId")
    public UUID getWorkpackId() { return workpack != null ? workpack.id : null; }

    @Column(name = "node_id")
    public String nodeId;

    @Column(nullable = false)
    public String title;

    @Column(columnDefinition = "TEXT")
    public String purpose;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public String scope = "{}";

    @Column(name = "input_context", columnDefinition = "TEXT")
    public String inputContext;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public String instructions = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public String constraints = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public String dependencies = "[]";

    @Column(name = "expected_output", columnDefinition = "TEXT")
    public String expectedOutput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "acceptance_criteria", columnDefinition = "jsonb")
    public String acceptanceCriteria = "[]";

    @Column(columnDefinition = "TEXT")
    public String handoff;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_hints", columnDefinition = "jsonb")
    public String executionHints = "{}";

    @Enumerated(EnumType.STRING)
    public BoxStatus status = BoxStatus.DRAFT;

    @Column(name = "order_index")
    public int orderIndex = 0;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    public static List<Box> findByWorkpack(UUID workpackId) {
        return list("workpack.id", workpackId);
    }
}
