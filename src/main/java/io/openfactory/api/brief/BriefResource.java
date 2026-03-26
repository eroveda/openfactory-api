package io.openfactory.api.brief;

import io.openfactory.api.brief.model.Brief;
import io.openfactory.api.brief.model.BriefStatus;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/workpacks/{workpackId}/brief")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BriefResource {

    @GET
    public Brief get(@PathParam("workpackId") UUID workpackId) {
        Brief brief = Brief.findByWorkpack(workpackId);
        if (brief == null) throw new NotFoundException("Brief not found for workpack: " + workpackId);
        return brief;
    }

    @PATCH
    @Transactional
    public Brief update(@PathParam("workpackId") UUID workpackId, UpdateBriefRequest req) {
        Brief brief = Brief.findByWorkpack(workpackId);
        if (brief == null) throw new NotFoundException("Brief not found for workpack: " + workpackId);

        if (req.title()           != null) brief.title           = req.title();
        if (req.mainIdea()        != null) brief.mainIdea        = req.mainIdea();
        if (req.objective()       != null) brief.objective       = req.objective();
        if (req.actors()          != null) brief.actors          = req.actors();
        if (req.scopeIncludes()   != null) brief.scopeIncludes   = req.scopeIncludes();
        if (req.scopeExcludes()   != null) brief.scopeExcludes   = req.scopeExcludes();
        if (req.constraints()     != null) brief.constraints     = req.constraints();
        if (req.successCriteria() != null) brief.successCriteria = req.successCriteria();
        if (req.domainFacts()     != null) brief.domainFacts     = req.domainFacts();
        if (req.status()          != null) brief.status          = req.status();

        brief.persist();
        return brief;
    }

    public record UpdateBriefRequest(
        String title,
        String mainIdea,
        String objective,
        String actors,
        String scopeIncludes,
        String scopeExcludes,
        String constraints,
        String successCriteria,
        String domainFacts,
        BriefStatus status
    ) {}
}
