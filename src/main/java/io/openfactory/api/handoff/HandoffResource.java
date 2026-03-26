package io.openfactory.api.handoff;

import io.openfactory.api.handoff.model.ApprovalStatus;
import io.openfactory.api.handoff.model.Handoff;
import io.openfactory.api.user.model.User;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.UUID;

@Path("/api/workpacks/{workpackId}/handoff")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HandoffResource {

    @GET
    public Handoff get(@PathParam("workpackId") UUID workpackId) {
        Handoff handoff = Handoff.findByWorkpack(workpackId);
        if (handoff == null) throw new NotFoundException("Handoff not found for workpack: " + workpackId);
        return handoff;
    }

    @PATCH
    @Transactional
    public Handoff update(@PathParam("workpackId") UUID workpackId, UpdateHandoffRequest req) {
        Handoff handoff = Handoff.findByWorkpack(workpackId);
        if (handoff == null) throw new NotFoundException("Handoff not found for workpack: " + workpackId);

        if (req.intendedExecutor() != null) handoff.intendedExecutor = req.intendedExecutor();
        if (req.assumptions()      != null) handoff.assumptions      = req.assumptions();
        if (req.handoffNotes()     != null) handoff.handoffNotes     = req.handoffNotes();

        handoff.persist();
        return handoff;
    }

    @POST
    @Path("/approve")
    @Transactional
    public Handoff approve(@PathParam("workpackId") UUID workpackId,
                           @Context ContainerRequestContext ctx) {
        Handoff handoff = Handoff.findByWorkpack(workpackId);
        if (handoff == null) throw new NotFoundException("Handoff not found for workpack: " + workpackId);

        User currentUser = (User) ctx.getProperty("currentUser");
        handoff.approvalStatus = ApprovalStatus.APPROVED;
        handoff.approvedBy     = currentUser;
        handoff.approvedAt     = LocalDateTime.now();
        handoff.reviewNotes    = null;
        handoff.persist();
        return handoff;
    }

    @POST
    @Path("/request-changes")
    @Transactional
    public Handoff requestChanges(@PathParam("workpackId") UUID workpackId,
                                  RequestChangesRequest req) {
        Handoff handoff = Handoff.findByWorkpack(workpackId);
        if (handoff == null) throw new NotFoundException("Handoff not found for workpack: " + workpackId);

        handoff.approvalStatus = ApprovalStatus.CHANGES_REQUESTED;
        handoff.reviewNotes    = req.reviewNotes();
        handoff.persist();
        return handoff;
    }

    public record UpdateHandoffRequest(
        String intendedExecutor,
        String assumptions,
        String handoffNotes
    ) {}

    public record RequestChangesRequest(String reviewNotes) {}
}
