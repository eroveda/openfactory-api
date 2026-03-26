package io.openfactory.api.user;

import io.openfactory.api.user.model.User;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @GET
    public User me(@Context ContainerRequestContext ctx) {
        return (User) ctx.getProperty("currentUser");
    }

    @PATCH
    @Transactional
    public User update(@Context ContainerRequestContext ctx, UpdateProfileRequest req) {
        User user = (User) ctx.getProperty("currentUser");
        if (req.name() != null && !req.name().isBlank()) user.name = req.name();
        user.persist();
        return user;
    }

    public record UpdateProfileRequest(String name) {}
}
