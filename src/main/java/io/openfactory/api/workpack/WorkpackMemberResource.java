package io.openfactory.api.workpack;

import io.openfactory.api.inbox.model.InboxItem;
import io.openfactory.api.inbox.model.InboxType;
import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.MemberRole;
import io.openfactory.api.workpack.model.WorkpackMember;
import io.openfactory.api.workpack.model.WorkpackMemberId;
import io.openfactory.api.workpack.model.Workpack;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Path("/api/workpacks/{workpackId}/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkpackMemberResource {

    @GET
    public List<WorkpackMember> list(@PathParam("workpackId") UUID workpackId) {
        return WorkpackMember.findByWorkpack(workpackId);
    }

    @POST
    @Transactional
    public WorkpackMember add(@PathParam("workpackId") UUID workpackId,
                               @Context ContainerRequestContext ctx,
                               AddMemberRequest req) {
        Workpack workpack = Workpack.findById(workpackId);
        if (workpack == null) throw new NotFoundException("Workpack not found: " + workpackId);

        User invited = User.findByEmail(req.email());
        if (invited == null) throw new NotFoundException("User not found: " + req.email());

        if (WorkpackMember.findByWorkpackAndUser(workpackId, invited.id) != null)
            throw new IllegalStateException("User is already a member of this workpack.");

        MemberRole role = req.role() != null ? req.role() : MemberRole.EDITOR;

        WorkpackMember member = new WorkpackMember();
        member.id       = new WorkpackMemberId(workpackId, invited.id);
        member.workpack = workpack;
        member.user     = invited;
        member.role     = role;
        member.persist();

        // Notify the invited user
        User currentUser = (User) ctx.getProperty("currentUser");
        InboxItem notification = new InboxItem();
        notification.user     = invited;
        notification.workpack = workpack;
        notification.type     = InboxType.WORKPACK_SHARED;
        notification.message  = currentUser.name + " shared workpack \"" + workpack.title + "\" with you.";
        notification.persist();

        return member;
    }

    @PATCH
    @Path("/{userId}")
    @Transactional
    public WorkpackMember updateRole(@PathParam("workpackId") UUID workpackId,
                                      @PathParam("userId") UUID userId,
                                      UpdateRoleRequest req) {
        WorkpackMember member = WorkpackMember.findByWorkpackAndUser(workpackId, userId);
        if (member == null) throw new NotFoundException("Member not found.");

        member.role = req.role();
        member.persist();
        return member;
    }

    @DELETE
    @Path("/{userId}")
    @Transactional
    public void remove(@PathParam("workpackId") UUID workpackId,
                       @PathParam("userId") UUID userId) {
        WorkpackMember member = WorkpackMember.findByWorkpackAndUser(workpackId, userId);
        if (member == null) throw new NotFoundException("Member not found.");
        member.delete();
    }

    public record AddMemberRequest(String email, MemberRole role) {}
    public record UpdateRoleRequest(MemberRole role) {}
}
