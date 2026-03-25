package io.openfactory.api.workpack;

import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.api.workpack.model.WorkpackStage;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.UUID;

@Path("/api/workpacks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkpackResource {

    @Inject
    WorkpackService service;

    @GET
    public Response list(@Context ContainerRequestContext ctx) {
        User user = (User) ctx.getProperty("currentUser");
        return Response.ok(Map.of(
            "mine", Workpack.findByOwner(user.id),
            "shared", Workpack.findSharedWith(user.id)
        )).build();
    }

    @POST
    @Transactional
    public Response create(@Context ContainerRequestContext ctx,
                            Map<String, String> body) {
        User user = (User) ctx.getProperty("currentUser");
        String title = body.getOrDefault("title", "New Workpack");

        Workpack wp = new Workpack();
        wp.title = title;
        wp.owner = user;
        wp.stage = WorkpackStage.RAW;
        wp.persist();

        return Response.status(201).entity(wp).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@Context ContainerRequestContext ctx,
                         @PathParam("id") UUID id) {
        User user = (User) ctx.getProperty("currentUser");
        Workpack wp = Workpack.findById(id);
        if (wp == null) return Response.status(404).build();
        if (!wp.owner.id.equals(user.id)) return Response.status(403).build();
        return Response.ok(wp).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@Context ContainerRequestContext ctx,
                            @PathParam("id") UUID id) {
        User user = (User) ctx.getProperty("currentUser");
        Workpack wp = Workpack.findById(id);
        if (wp == null) return Response.status(404).build();
        if (!wp.owner.id.equals(user.id)) return Response.status(403).build();
        wp.delete();
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/ingest")
    @Transactional
    public Response ingest(@Context ContainerRequestContext ctx,
                            @PathParam("id") UUID id,
                            Map<String, Object> body) {
        User user = (User) ctx.getProperty("currentUser");
        Workpack wp = Workpack.findById(id);
        if (wp == null) return Response.status(404).build();

        return Response.ok(
            service.ingest(wp, user, body)
        ).build();
    }

    @POST
    @Path("/{id}/shape")
    public Response shape(@Context ContainerRequestContext ctx,
                           @PathParam("id") UUID id) {
        User user = (User) ctx.getProperty("currentUser");
        Workpack wp = Workpack.findById(id);
        if (wp == null) return Response.status(404).build();

        return Response.ok(
            service.shape(wp, user)
        ).build();
    }

    @POST
    @Path("/{id}/export")
    public Response export(@Context ContainerRequestContext ctx,
                            @PathParam("id") UUID id) {
        User user = (User) ctx.getProperty("currentUser");
        Workpack wp = Workpack.findById(id);
        if (wp == null) return Response.status(404).build();

        return Response.ok(
            service.export(wp, user)
        ).build();
    }
}
