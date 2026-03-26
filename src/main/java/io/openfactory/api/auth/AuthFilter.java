package io.openfactory.api.auth;

import io.openfactory.api.user.model.User;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Provider
public class AuthFilter implements ContainerRequestFilter {

    @Inject
    JWTParser jwtParser;

    @Inject
    DevUserService devUserService;

    @ConfigProperty(name = "quarkus.profile")
    String profile;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();

        if (path.startsWith("/q/") || path.equals("/health")) return;

        if ("dev".equals(profile)) {
            User devUser = devUserService.getOrCreateDevUser();
            ctx.setProperty("currentUser", devUser);
            return;
        }

        String auth = ctx.getHeaderString("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            ctx.abortWith(Response.status(401)
                .entity("{\"error\":\"Missing authorization token\"}")
                .build());
            return;
        }

        try {
            String token = auth.substring(7);
            JsonWebToken jwt = jwtParser.parse(token);
            String supabaseId = jwt.getSubject();
            String email = jwt.getClaim("email");

            User user = User.findBySupabaseId(supabaseId);
            if (user == null) {
                user = new User();
                user.supabaseId = supabaseId;
                user.email = email;
                user.name = jwt.getClaim("full_name");
                user.avatarUrl = jwt.getClaim("avatar_url");
                user.persist();
            }

            ctx.setProperty("currentUser", user);

        } catch (Exception e) {
            ctx.abortWith(Response.status(401)
                .entity("{\"error\":\"Invalid token\"}")
                .build());
        }
    }
}
