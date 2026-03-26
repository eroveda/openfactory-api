package io.openfactory.api.box;

import io.openfactory.api.box.model.Box;
import io.openfactory.api.box.model.BoxStatus;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Path("/api/workpacks/{workpackId}/boxes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BoxResource {

    @GET
    public List<Box> list(@PathParam("workpackId") UUID workpackId) {
        return Box.list("workpack.id = ?1 order by orderIndex", workpackId);
    }

    @GET
    @Path("/{id}")
    public Box get(@PathParam("workpackId") UUID workpackId, @PathParam("id") UUID id) {
        Box box = Box.findById(id);
        if (box == null || !box.workpack.id.equals(workpackId))
            throw new NotFoundException("Box not found: " + id);
        return box;
    }

    @PATCH
    @Path("/{id}")
    @Transactional
    public Box update(@PathParam("workpackId") UUID workpackId,
                      @PathParam("id") UUID id,
                      UpdateBoxRequest req) {
        Box box = Box.findById(id);
        if (box == null || !box.workpack.id.equals(workpackId))
            throw new NotFoundException("Box not found: " + id);

        if (req.title()              != null) box.title              = req.title();
        if (req.purpose()            != null) box.purpose            = req.purpose();
        if (req.inputContext()       != null) box.inputContext       = req.inputContext();
        if (req.expectedOutput()     != null) box.expectedOutput     = req.expectedOutput();
        if (req.instructions()       != null) box.instructions       = req.instructions();
        if (req.constraints()        != null) box.constraints        = req.constraints();
        if (req.acceptanceCriteria() != null) box.acceptanceCriteria = req.acceptanceCriteria();
        if (req.handoff()            != null) box.handoff            = req.handoff();
        if (req.status()             != null) box.status             = req.status();

        box.persist();
        return box;
    }

    @PATCH
    @Path("/{id}/status")
    @Transactional
    public Box updateStatus(@PathParam("workpackId") UUID workpackId,
                            @PathParam("id") UUID id,
                            UpdateStatusRequest req) {
        Box box = Box.findById(id);
        if (box == null || !box.workpack.id.equals(workpackId))
            throw new NotFoundException("Box not found: " + id);
        box.status = req.status();
        box.persist();
        return box;
    }

    public record UpdateBoxRequest(
        String title,
        String purpose,
        String inputContext,
        String expectedOutput,
        String instructions,
        String constraints,
        String acceptanceCriteria,
        String handoff,
        BoxStatus status
    ) {}

    public record UpdateStatusRequest(BoxStatus status) {}
}
