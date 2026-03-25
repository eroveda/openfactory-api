package io.openfactory.api.workpack.model;

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
    public User owner;

    @Enumerated(EnumType.STRING)
    public WorkpackStage stage = WorkpackStage.RAW;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();

    public static List<Workpack> findByOwner(UUID ownerId) {
        return list("owner.id", ownerId);
    }

    public static List<Workpack> findSharedWith(UUID userId) {
        return list(
            "id IN (SELECT workpack_id FROM workpack_members WHERE user_id = ?1)",
            userId);
    }
}
