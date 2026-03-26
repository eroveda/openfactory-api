package io.openfactory.api.workpack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openfactory.api.box.model.Box;
import io.openfactory.api.box.model.BoxStatus;
import io.openfactory.api.brief.model.Brief;
import io.openfactory.api.brief.model.BriefStatus;
import io.openfactory.api.handoff.model.Handoff;
import io.openfactory.api.plan.model.ExecutionPlanEntity;
import io.openfactory.api.plan.model.PlanStatus;
import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.api.workpack.model.WorkpackStage;
import io.openfactory.core.brief.model.IdeaBrief;
import io.openfactory.core.handoff.model.HandoffPackage;
import io.openfactory.core.plan.model.ExecutionPlan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class WorkpackMapper {

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public Workpack toEntity(String title, UUID ownerId, String sourceContent,
                              IdeaBrief brief,
                              ExecutionPlan plan,
                              List<io.openfactory.core.box.model.Box> boxes,
                              HandoffPackage handoff) {
        User owner = User.findById(ownerId);

        // 1 — Workpack
        Workpack w = new Workpack();
        w.title = title;
        w.owner = owner;
        w.stage = WorkpackStage.SHAPE;
        w.sourceContent = sourceContent;
        w.persist();

        // 2 — Brief
        if (brief != null) {
            Brief b = new Brief();
            b.workpack        = w;
            b.title           = brief.title();
            b.mainIdea        = brief.mainIdea();
            b.objective       = brief.objective();
            b.actors          = toJson(brief.actors());
            b.scopeIncludes   = toJson(brief.scope().includes());
            b.scopeExcludes   = toJson(brief.scope().excludes());
            b.constraints     = toJson(brief.constraints());
            b.successCriteria = toJson(brief.successCriteria());
            b.domainFacts     = toJson(brief.domainFacts());
            b.status          = brief.isReady() ? BriefStatus.READY : BriefStatus.DRAFT;
            b.persist();
        }

        // 3 — Boxes
        if (boxes != null) {
            for (int i = 0; i < boxes.size(); i++) {
                io.openfactory.core.box.model.Box cb = boxes.get(i);
                Box box = new Box();
                box.workpack          = w;
                box.nodeId            = cb.getNodeId();
                box.title             = cb.getTitle();
                box.purpose           = cb.getPurpose();
                box.inputContext      = cb.getInputContext();
                box.expectedOutput    = cb.getExpectedOutput();
                box.handoff           = cb.getHandoff();
                box.instructions      = toJson(cb.getInstructions());
                box.constraints       = toJson(cb.getConstraints());
                box.dependencies      = toJson(cb.getDependencies());
                box.acceptanceCriteria= toJson(cb.getAcceptanceCriteria());
                box.status            = BoxStatus.READY;
                box.orderIndex        = i;
                box.persist();
            }
        }

        // 4 — ExecutionPlan
        if (plan != null) {
            ExecutionPlanEntity ep = new ExecutionPlanEntity();
            ep.workpack         = w;
            ep.version          = plan.version();
            ep.status           = toPlanStatus(plan.validationStatus());
            ep.steps            = toJson(plan.steps());
            ep.weakDependencies = toJson(plan.weakDependencies());
            ep.findings         = toJson(plan.findings());
            ep.persist();
        }

        // 5 — Handoff
        if (handoff != null) {
            Handoff h = new Handoff();
            h.workpack         = w;
            h.owner            = owner;
            h.assumptions      = toJson(handoff.getAssumptions());
            h.handoffNotes     = handoff.getHandoffNotes();
            h.persist();
        }

        return w;
    }

    /**
     * Crea los hijos (Brief, Boxes, Plan, Handoff) para un workpack que ya existe en DB.
     * Usado por el pipeline asíncrono.
     */
    @Transactional
    public void buildChildren(Workpack w,
                               IdeaBrief brief,
                               ExecutionPlan plan,
                               List<io.openfactory.core.box.model.Box> boxes,
                               HandoffPackage handoff) {
        if (brief != null) {
            Brief b = new Brief();
            b.workpack        = w;
            b.title           = brief.title();
            b.mainIdea        = brief.mainIdea();
            b.objective       = brief.objective();
            b.actors          = toJson(brief.actors());
            b.scopeIncludes   = toJson(brief.scope().includes());
            b.scopeExcludes   = toJson(brief.scope().excludes());
            b.constraints     = toJson(brief.constraints());
            b.successCriteria = toJson(brief.successCriteria());
            b.domainFacts     = toJson(brief.domainFacts());
            b.status          = brief.isReady() ? BriefStatus.READY : BriefStatus.DRAFT;
            b.persist();
        }

        if (boxes != null) {
            for (int i = 0; i < boxes.size(); i++) {
                io.openfactory.core.box.model.Box cb = boxes.get(i);
                Box box = new Box();
                box.workpack          = w;
                box.nodeId            = cb.getNodeId();
                box.title             = cb.getTitle();
                box.purpose           = cb.getPurpose();
                box.inputContext      = cb.getInputContext();
                box.expectedOutput    = cb.getExpectedOutput();
                box.handoff           = cb.getHandoff();
                box.instructions      = toJson(cb.getInstructions());
                box.constraints       = toJson(cb.getConstraints());
                box.dependencies      = toJson(cb.getDependencies());
                box.acceptanceCriteria= toJson(cb.getAcceptanceCriteria());
                box.status            = BoxStatus.READY;
                box.orderIndex        = i;
                box.persist();
            }
        }

        if (plan != null) {
            ExecutionPlanEntity ep = new ExecutionPlanEntity();
            ep.workpack         = w;
            ep.version          = plan.version();
            ep.status           = toPlanStatus(plan.validationStatus());
            ep.steps            = toJson(plan.steps());
            ep.weakDependencies = toJson(plan.weakDependencies());
            ep.findings         = toJson(plan.findings());
            ep.persist();
        }

        if (handoff != null) {
            Handoff h = new Handoff();
            h.workpack         = w;
            h.owner            = w.owner;
            h.assumptions      = toJson(handoff.getAssumptions());
            h.handoffNotes     = handoff.getHandoffNotes();
            h.persist();
        }
    }

    /** Elimina los hijos existentes de un workpack y los recrea con nuevos datos del pipeline. */
    @Transactional
    public void replaceChildren(Workpack w,
                                 IdeaBrief brief,
                                 ExecutionPlan plan,
                                 List<io.openfactory.core.box.model.Box> boxes,
                                 HandoffPackage handoff) {
        io.openfactory.api.brief.model.Brief.delete("workpack.id = ?1", w.id);
        io.openfactory.api.box.model.Box.delete("workpack.id = ?1", w.id);
        io.openfactory.api.plan.model.ExecutionPlanEntity.delete("workpack.id = ?1", w.id);
        io.openfactory.api.handoff.model.Handoff.delete("workpack.id = ?1", w.id);

        if (brief != null) {
            io.openfactory.api.brief.model.Brief b = new io.openfactory.api.brief.model.Brief();
            b.workpack        = w;
            b.title           = brief.title();
            b.mainIdea        = brief.mainIdea();
            b.objective       = brief.objective();
            b.actors          = toJson(brief.actors());
            b.scopeIncludes   = toJson(brief.scope().includes());
            b.scopeExcludes   = toJson(brief.scope().excludes());
            b.constraints     = toJson(brief.constraints());
            b.successCriteria = toJson(brief.successCriteria());
            b.domainFacts     = toJson(brief.domainFacts());
            b.status          = brief.isReady()
                ? io.openfactory.api.brief.model.BriefStatus.READY
                : io.openfactory.api.brief.model.BriefStatus.DRAFT;
            b.persist();
        }

        if (boxes != null) {
            for (int i = 0; i < boxes.size(); i++) {
                io.openfactory.core.box.model.Box cb = boxes.get(i);
                io.openfactory.api.box.model.Box box = new io.openfactory.api.box.model.Box();
                box.workpack          = w;
                box.nodeId            = cb.getNodeId();
                box.title             = cb.getTitle();
                box.purpose           = cb.getPurpose();
                box.inputContext      = cb.getInputContext();
                box.expectedOutput    = cb.getExpectedOutput();
                box.handoff           = cb.getHandoff();
                box.instructions      = toJson(cb.getInstructions());
                box.constraints       = toJson(cb.getConstraints());
                box.dependencies      = toJson(cb.getDependencies());
                box.acceptanceCriteria= toJson(cb.getAcceptanceCriteria());
                box.status            = io.openfactory.api.box.model.BoxStatus.READY;
                box.orderIndex        = i;
                box.persist();
            }
        }

        if (plan != null) {
            io.openfactory.api.plan.model.ExecutionPlanEntity ep = new io.openfactory.api.plan.model.ExecutionPlanEntity();
            ep.workpack         = w;
            ep.version          = plan.version();
            ep.status           = toPlanStatus(plan.validationStatus());
            ep.steps            = toJson(plan.steps());
            ep.weakDependencies = toJson(plan.weakDependencies());
            ep.findings         = toJson(plan.findings());
            ep.persist();
        }

        if (handoff != null) {
            io.openfactory.api.handoff.model.Handoff h = new io.openfactory.api.handoff.model.Handoff();
            h.workpack         = w;
            h.owner            = w.owner;
            h.assumptions      = toJson(handoff.getAssumptions());
            h.handoffNotes     = handoff.getHandoffNotes();
            h.persist();
        }
    }

    // -----------------------------------------------------------------------

    private PlanStatus toPlanStatus(io.openfactory.core.plan.model.PlanValidationStatus s) {
        if (s == null) return PlanStatus.VALID;
        return switch (s) {
            case HAS_WARNINGS -> PlanStatus.HAS_WARNINGS;
            case INVALID      -> PlanStatus.INVALID;
            default           -> PlanStatus.VALID;
        };
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
