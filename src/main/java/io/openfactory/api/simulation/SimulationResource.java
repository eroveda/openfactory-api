package io.openfactory.api.simulation;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/workpacks/{workpackId}/simulate")
@Produces(MediaType.APPLICATION_JSON)
public class SimulationResource {

    @Inject
    SimulationService simulationService;

    @POST
    public SimulationService.SimulationResult simulate(@PathParam("workpackId") UUID workpackId) {
        return simulationService.simulate(workpackId);
    }
}
