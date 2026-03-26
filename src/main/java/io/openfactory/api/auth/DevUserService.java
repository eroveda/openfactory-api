package io.openfactory.api.auth;

import io.openfactory.api.user.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DevUserService {

    @Transactional
    public User getOrCreateDevUser() {
        User user = User.findByEmail("dev@openfactory.io");
        if (user == null) {
            user = new User();
            user.supabaseId = "dev-user";
            user.email = "dev@openfactory.io";
            user.name = "Dev User";
            user.persist();
        }
        return user;
    }
}
