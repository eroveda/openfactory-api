package io.openfactory.api.auth;

import io.openfactory.api.user.model.User;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
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

    @Inject
    UserSyncService userSyncService;

    @ConfigProperty(name = "openfactory.auth.bypass", defaultValue = "false")
    boolean authBypass;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();

        if (path.startsWith("/q/") || path.equals("/health")) return;

        if (LaunchMode.current().isDevOrTest() || authBypass) {
            User devUser = devUserService.getOrCreateDevUser();
            ctx.setProperty("currentUser", devUser);
            return;
        }

        String auth = ctx.getHeaderString("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            abort(ctx, "Missing authorization token");
            return;
        }

        try {
            String token = auth.substring(7);
            JsonWebToken jwt = jwtParser.parse(token);
            User user = userSyncService.syncFromJwt(jwt);
            ctx.setProperty("currentUser", user);

        } catch (Exception e) {
            abort(ctx, "Invalid token");
        }
    }

    private void abort(ContainerRequestContext ctx, String message) {
        String origin = ctx.getHeaderString("Origin");
        Response.ResponseBuilder rb = Response.status(401)
            .type(MediaType.APPLICATION_JSON)
            .entity("{\"error\":\"" + message + "\"}");
        if (origin != null) {
            rb.header("Access-Control-Allow-Origin",      origin)
              .header("Access-Control-Allow-Credentials", "true");
        }
        ctx.abortWith(rb.build());
    }
}
