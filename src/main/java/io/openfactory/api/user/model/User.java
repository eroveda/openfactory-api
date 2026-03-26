package io.openfactory.api.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(name = "supabase_id", unique = true, nullable = false)
    @JsonIgnore
    public String supabaseId;

    @Column(unique = true, nullable = false)
    public String email;

    public String name;

    @Column(name = "avatar_url")
    public String avatarUrl;

    public String plan = "free";

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();

    public static User findBySupabaseId(String supabaseId) {
        return find("supabaseId", supabaseId).firstResult();
    }

    public static User findByEmail(String email) {
        return find("email", email).firstResult();
    }
}
