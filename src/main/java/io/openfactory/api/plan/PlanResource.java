package io.openfactory.api.plan;

import io.openfactory.api.plan.model.ExecutionPlanEntity;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/workpacks/{workpackId}/plan")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlanResource {

    @GET
    public ExecutionPlanEntity get(@PathParam("workpackId") UUID workpackId) {
        ExecutionPlanEntity plan = ExecutionPlanEntity.findByWorkpack(workpackId);
        if (plan == null) throw new NotFoundException("Plan not found for workpack: " + workpackId);
        return plan;
    }
}
