package io.openfactory.api.pin.model;

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
@Table(name = "pins")
public class Pin extends PanacheEntityBase {

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

    @Column(nullable = false, columnDefinition = "TEXT")
    public String content;

    @Enumerated(EnumType.STRING)
    public PinType type = PinType.UNKNOWN;

    public Double confidence;

    @Column(name = "order_index")
    public int orderIndex = 0;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @JsonProperty("workpackId")
    public UUID getWorkpackId() { return workpack != null ? workpack.id : null; }

    @JsonProperty("userId")
    public UUID getUserId() { return user != null ? user.id : null; }

    public static List<Pin> findByWorkpack(UUID workpackId) {
        return list("workpack.id = ?1 order by orderIndex", workpackId);
    }
}
