package io.openfactory.api.workpack;

import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.api.workpack.model.WorkpackStage;
import io.openfactory.core.box.model.Box;
import io.openfactory.core.brief.model.IdeaBrief;
import io.openfactory.core.handoff.HandoffService;
import io.openfactory.core.handoff.model.HandoffPackage;
import io.openfactory.core.plan.model.ExecutionPlan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class WorkpackService {

    @Inject
    HandoffService coreHandoffService;

    @Inject
    WorkpackMapper mapper;

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------

    @Transactional
    public Workpack createFromPipeline(String title, UUID ownerId,
                                        IdeaBrief brief,
                                        ExecutionPlan plan,
                                        List<Box> boxes) {
        HandoffPackage handoff = coreHandoffService.create(
            brief.projectId(), brief, plan);
        return mapper.toEntity(title, ownerId, brief, plan, boxes, handoff);
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    public Workpack findById(UUID id) {
        Workpack w = Workpack.findById(id);
        if (w == null) throw new NotFoundException("Workpack not found: " + id);
        return w;
    }

    public List<Workpack> listAll() {
        return Workpack.listAll();
    }

    public List<Workpack> listByOwner(UUID ownerId) {
        return Workpack.findByOwner(ownerId);
    }

    // -----------------------------------------------------------------------
    // Stage transitions
    // -----------------------------------------------------------------------

    @Transactional
    public Workpack advanceStage(UUID id) {
        Workpack w = findById(id);
        w.stage = switch (w.stage) {
            case RAW    -> WorkpackStage.DEFINE;
            case DEFINE -> WorkpackStage.SHAPE;
            case SHAPE  -> WorkpackStage.BOX;
            case BOX    -> throw new IllegalStateException(
                "Workpack " + id + " is already at BOX stage.");
        };
        w.persist();
        return w;
    }

    @Transactional
    public Workpack updateTitle(UUID id, String title) {
        Workpack w = findById(id);
        w.title = title;
        w.persist();
        return w;
    }

    // -----------------------------------------------------------------------
    // Value types
    // -----------------------------------------------------------------------

    public record StepSummary(
        String stepId,
        String boxId,
        String boxTitle,
        int order,
        boolean parallel,
        boolean requiresApproval
    ) {}
}
