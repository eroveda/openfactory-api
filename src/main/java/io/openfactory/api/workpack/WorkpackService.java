package io.openfactory.api.workpack;

import io.openfactory.api.box.model.Box;
import io.openfactory.api.brief.model.Brief;
import io.openfactory.api.handoff.model.Handoff;
import io.openfactory.api.plan.model.ExecutionPlanEntity;
import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.api.workpack.model.WorkpackStage;
import io.openfactory.core.box.BoxGenerator;
import io.openfactory.core.box.BoxGenerator.BoxGenerationResult;
import io.openfactory.core.brief.BriefBuilder;
import io.openfactory.core.brief.model.IdeaBrief;
import io.openfactory.core.handoff.HandoffService;
import io.openfactory.core.handoff.model.HandoffPackage;
import io.openfactory.core.ingestion.SessionIngestionService;
import io.openfactory.core.ingestion.model.IngestionSnapshot;
import io.openfactory.core.ingestion.model.SessionMessage;
import io.openfactory.core.ingestion.model.SourceDocument;
import io.openfactory.core.plan.ExecutionPlanner;
import io.openfactory.core.plan.model.ExecutionPlan;
import io.openfactory.core.tree.OutlineService;
import io.openfactory.core.tree.model.NodeTree;
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

    @Inject
    SessionIngestionService ingestionService;

    @Inject
    BriefBuilder briefBuilder;

    @Inject
    OutlineService outlineService;

    @Inject
    BoxGenerator boxGenerator;

    @Inject
    ExecutionPlanner executionPlanner;

    // -----------------------------------------------------------------------
    // Pipeline — ingest (full run)
    // -----------------------------------------------------------------------

    /**
     * Corre el pipeline completo sobre el contenido raw y persiste el resultado.
     * Los llamados LLM ocurren fuera de transacción; la persistencia es atómica.
     */
    public Workpack ingest(String title, UUID ownerId, String content) throws Exception {
        PipelineData data = runPipeline(title, content);
        HandoffPackage handoff = coreHandoffService.create(
            data.snapshot.projectId(), data.brief, data.plan);
        return mapper.toEntity(title, ownerId, content, data.brief, data.plan, data.boxes, handoff);
    }

    // -----------------------------------------------------------------------
    // Pipeline — reshape (re-run sobre workpack existente)
    // -----------------------------------------------------------------------

    /**
     * Re-corre el pipeline usando el source_content almacenado, reemplaza
     * brief, boxes, plan y handoff del workpack.
     */
    public Workpack reshape(UUID id) throws Exception {
        Workpack w = findById(id);
        if (w.sourceContent == null || w.sourceContent.isBlank()) {
            throw new IllegalStateException(
                "Workpack " + id + " no tiene source_content para reshape.");
        }
        PipelineData data = runPipeline(w.title, w.sourceContent);
        HandoffPackage handoff = coreHandoffService.create(
            data.snapshot.projectId(), data.brief, data.plan);
        mapper.replaceChildren(w, data.brief, data.plan, data.boxes, handoff);
        return w;
    }

    // -----------------------------------------------------------------------
    // Export (lectura estructurada de datos persistidos)
    // -----------------------------------------------------------------------

    public ExportView getExport(UUID id) {
        Workpack w = findById(id);
        Brief brief = Brief.findByWorkpack(w.id);
        List<Box> boxes = Box.list("workpack.id = ?1 order by orderIndex", w.id);
        ExecutionPlanEntity plan = ExecutionPlanEntity.findByWorkpack(w.id);
        Handoff handoff = Handoff.findByWorkpack(w.id);

        return new ExportView(
            w.id,
            w.title,
            w.stage,
            brief == null ? null : new ExportView.BriefData(
                brief.id, brief.title, brief.mainIdea, brief.objective, brief.status.name()),
            boxes.stream().map(b -> new ExportView.BoxData(
                b.id, b.nodeId, b.title, b.purpose, b.expectedOutput, b.orderIndex)).toList(),
            plan == null ? null : new ExportView.PlanData(
                plan.id, plan.version, plan.status.name(), plan.steps),
            handoff == null ? null : new ExportView.HandoffData(
                handoff.id, handoff.handoffNotes, handoff.approvalStatus.name())
        );
    }

    public record ExportView(
        UUID workpackId,
        String title,
        WorkpackStage stage,
        BriefData brief,
        List<BoxData> boxes,
        PlanData plan,
        HandoffData handoff
    ) {
        public record BriefData(UUID id, String title, String mainIdea, String objective, String status) {}
        public record BoxData(UUID id, String nodeId, String title, String purpose, String expectedOutput, int orderIndex) {}
        public record PlanData(UUID id, String version, String status, String steps) {}
        public record HandoffData(UUID id, String notes, String approvalStatus) {}
    }

    // -----------------------------------------------------------------------
    // Create (API interna — recibe datos ya construidos por el llamante)
    // -----------------------------------------------------------------------

    @Transactional
    public Workpack createFromPipeline(String title, UUID ownerId,
                                        IdeaBrief brief,
                                        ExecutionPlan plan,
                                        List<io.openfactory.core.box.model.Box> boxes) {
        HandoffPackage handoff = coreHandoffService.create(
            brief.projectId(), brief, plan);
        return mapper.toEntity(title, ownerId, null, brief, plan, boxes, handoff);
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

    // -----------------------------------------------------------------------
    // Pipeline helper (sin transacción — LLM calls)
    // -----------------------------------------------------------------------

    private record PipelineData(
        IngestionSnapshot snapshot,
        SourceDocument sourceDoc,
        IdeaBrief brief,
        NodeTree outline,
        List<io.openfactory.core.box.model.Box> boxes,
        ExecutionPlan plan
    ) {}

    private PipelineData runPipeline(String title, String content) throws Exception {
        String sessionId  = UUID.randomUUID().toString();
        String projectId  = UUID.randomUUID().toString();

        List<SessionMessage> messages = List.of(
            new SessionMessage(UUID.randomUUID().toString(), content, null,
                System.currentTimeMillis())
        );

        IngestionSnapshot snapshot  = ingestionService.buildSnapshot(sessionId, projectId, messages);
        SourceDocument    sourceDoc = ingestionService.buildSourceDocument(snapshot);
        IdeaBrief         brief     = briefBuilder.build(snapshot, sourceDoc);
        NodeTree          outline   = outlineService.generateOutline(sourceDoc);
        BoxGenerationResult boxResult = boxGenerator.generateFromTree(outline, sourceDoc);
        ExecutionPlan     plan      = executionPlanner.plan(
            boxResult.boxes(), outline, snapshot.projectId());

        return new PipelineData(snapshot, sourceDoc, brief, outline, boxResult.boxes(), plan);
    }
}
