package io.openfactory.api.workpack;

import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.api.user.model.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.container.ContainerRequestContext;

import java.util.List;
import java.util.UUID;

@Path("/api/workpacks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkpackResource {

    @Inject
    WorkpackService workpackService;

    @GET
    public List<Workpack> listAll(@Context ContainerRequestContext ctx) {
        User user = (User) ctx.getProperty("currentUser");
        return workpackService.listByOwner(user.id);
    }

    @GET
    @Path("/{id}")
    public Workpack findById(@PathParam("id") UUID id) {
        return workpackService.findById(id);
    }

    @POST
    @Path("/{id}/advance")
    public Workpack advance(@PathParam("id") UUID id) {
        return workpackService.advanceStage(id);
    }

    @PATCH
    @Path("/{id}/title")
    public Workpack updateTitle(@PathParam("id") UUID id, UpdateTitleRequest req) {
        return workpackService.updateTitle(id, req.title());
    }

    public record UpdateTitleRequest(String title) {}
}
