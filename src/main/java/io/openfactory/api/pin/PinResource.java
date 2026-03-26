package io.openfactory.api.pin;

import io.openfactory.api.pin.model.Pin;
import io.openfactory.api.pin.model.PinType;
import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.Workpack;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Path("/api/workpacks/{workpackId}/pins")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PinResource {

    @GET
    public List<Pin> list(@PathParam("workpackId") UUID workpackId) {
        return Pin.findByWorkpack(workpackId);
    }

    @POST
    @Transactional
    public Pin create(@PathParam("workpackId") UUID workpackId,
                      @Context ContainerRequestContext ctx,
                      CreatePinRequest req) {
        Workpack workpack = Workpack.findById(workpackId);
        if (workpack == null) throw new NotFoundException("Workpack not found: " + workpackId);

        User user = (User) ctx.getProperty("currentUser");

        Pin pin = new Pin();
        pin.workpack    = workpack;
        pin.user        = user;
        pin.content     = req.content();
        pin.type        = req.type() != null ? req.type() : PinType.UNKNOWN;
        pin.confidence  = req.confidence();
        pin.orderIndex  = req.orderIndex() != null ? req.orderIndex() : 0;
        pin.persist();
        return pin;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("workpackId") UUID workpackId,
                       @PathParam("id") UUID id) {
        Pin pin = Pin.findById(id);
        if (pin == null || !pin.workpack.id.equals(workpackId))
            throw new NotFoundException("Pin not found: " + id);
        pin.delete();
    }

    public record CreatePinRequest(
        String content,
        PinType type,
        Double confidence,
        Integer orderIndex
    ) {}
}
