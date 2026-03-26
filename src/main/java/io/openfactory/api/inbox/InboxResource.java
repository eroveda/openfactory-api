package io.openfactory.api.inbox;

import io.openfactory.api.inbox.model.InboxItem;
import io.openfactory.api.user.model.User;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Path("/api/inbox")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InboxResource {

    @GET
    public List<InboxItem> list(@Context ContainerRequestContext ctx,
                                @QueryParam("unread") boolean unreadOnly) {
        User user = (User) ctx.getProperty("currentUser");
        return unreadOnly
            ? InboxItem.findUnreadByUser(user.id)
            : InboxItem.findByUser(user.id);
    }

    @POST
    @Path("/{id}/read")
    @Transactional
    public InboxItem markRead(@Context ContainerRequestContext ctx,
                              @PathParam("id") UUID id) {
        User user = (User) ctx.getProperty("currentUser");
        InboxItem item = InboxItem.findById(id);
        if (item == null || !item.user.id.equals(user.id))
            throw new NotFoundException("Inbox item not found: " + id);
        item.read = true;
        item.persist();
        return item;
    }

    @POST
    @Path("/read-all")
    @Transactional
    public ReadAllResponse markAllRead(@Context ContainerRequestContext ctx) {
        User user = (User) ctx.getProperty("currentUser");
        long updated = InboxItem.markAllRead(user.id);
        return new ReadAllResponse(updated);
    }

    public record ReadAllResponse(long marked) {}
}
