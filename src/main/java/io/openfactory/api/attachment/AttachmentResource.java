package io.openfactory.api.attachment;

import io.openfactory.api.attachment.model.Attachment;
import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.Workpack;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Path("/api/workpacks/{workpackId}/attachments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AttachmentResource {

    @GET
    public List<Attachment> list(@PathParam("workpackId") UUID workpackId) {
        return Attachment.findByWorkpack(workpackId);
    }

    @POST
    @Transactional
    public Attachment create(@PathParam("workpackId") UUID workpackId,
                             @Context ContainerRequestContext ctx,
                             CreateAttachmentRequest req) {
        Workpack workpack = Workpack.findById(workpackId);
        if (workpack == null) throw new NotFoundException("Workpack not found: " + workpackId);

        User user = (User) ctx.getProperty("currentUser");

        Attachment a = new Attachment();
        a.workpack     = workpack;
        a.user         = user;
        a.fileName     = req.fileName();
        a.fileType     = req.fileType();
        a.storageUrl   = req.storageUrl();
        a.contentText  = req.contentText();
        a.persist();
        return a;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("workpackId") UUID workpackId,
                       @PathParam("id") UUID id) {
        Attachment a = Attachment.findById(id);
        if (a == null || !a.workpack.id.equals(workpackId))
            throw new NotFoundException("Attachment not found: " + id);
        a.delete();
    }

    public record CreateAttachmentRequest(
        String fileName,
        String fileType,
        String storageUrl,
        String contentText   // null para imágenes/audio
    ) {}
}
