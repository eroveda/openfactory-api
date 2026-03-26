package io.openfactory.api.auth;

import io.openfactory.api.user.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonObject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class UserSyncService {

    /**
     * Busca o crea el usuario en DB a partir del JWT de Supabase.
     * Si ya existe, actualiza name y avatarUrl en caso de que hayan cambiado.
     *
     * Supabase JWT structure:
     *   sub          → user UUID (supabase_id)
     *   email        → top-level claim
     *   user_metadata.full_name  → display name (Google + GitHub)
     *   user_metadata.avatar_url → avatar URL
     */
    @Transactional
    public User syncFromJwt(JsonWebToken jwt) {
        String supabaseId = jwt.getSubject();
        String email      = jwt.getClaim("email");

        JsonObject meta     = jwt.getClaim("user_metadata");
        String     fullName = extractName(meta);
        String     avatar   = meta != null ? meta.getString("avatar_url", null) : null;

        User user = User.findBySupabaseId(supabaseId);
        if (user == null) {
            user = new User();
            user.supabaseId = supabaseId;
            user.email      = email;
            user.name       = fullName;
            user.avatarUrl  = avatar;
            user.persist();
        } else {
            boolean changed = false;
            if (fullName != null && !fullName.equals(user.name)) {
                user.name  = fullName;
                changed    = true;
            }
            if (avatar != null && !avatar.equals(user.avatarUrl)) {
                user.avatarUrl = avatar;
                changed        = true;
            }
            if (changed) user.persist();
        }

        return user;
    }

    private String extractName(JsonObject meta) {
        if (meta == null) return null;
        // full_name está presente en Google y GitHub (normalizado por Supabase)
        String name = meta.getString("full_name", null);
        if (name == null || name.isBlank()) {
            name = meta.getString("name", null);
        }
        return name;
    }
}
