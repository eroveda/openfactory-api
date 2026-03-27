package io.openfactory.api.handoff;

import io.openfactory.api.handoff.model.ApprovalStatus;
import io.openfactory.api.handoff.model.Handoff;
import io.openfactory.api.inbox.model.InboxItem;
import io.openfactory.api.inbox.model.InboxType;
import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.api.workpack.model.WorkpackMember;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.List;
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
    @Path("/request-approval")
    @Transactional
    public Handoff requestApproval(@PathParam("workpackId") UUID workpackId,
                                   @Context ContainerRequestContext ctx) {
        Handoff handoff = Handoff.findByWorkpack(workpackId);
        if (handoff == null) throw new NotFoundException("Handoff not found for workpack: " + workpackId);

        Workpack workpack = handoff.workpack;
        User currentUser = (User) ctx.getProperty("currentUser");

        List<WorkpackMember> members = WorkpackMember.findByWorkpack(workpackId);
        for (WorkpackMember member : members) {
            if (member.user.id.equals(currentUser.id)) continue;
            InboxItem notification = new InboxItem();
            notification.user     = member.user;
            notification.workpack = workpack;
            notification.type     = InboxType.APPROVAL_REQUESTED;
            notification.message  = currentUser.name + " requested your approval on \"" + workpack.title + "\".";
            notification.persist();
        }

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
