package io.openfactory.api.workpack;

import io.openfactory.api.box.model.Box;
import io.openfactory.api.brief.model.Brief;
import io.openfactory.api.handoff.model.Handoff;
import io.openfactory.api.plan.model.ExecutionPlanEntity;
import io.openfactory.api.workpack.model.ProcessingStatus;
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
import java.util.concurrent.CompletableFuture;

import io.openfactory.api.attachment.model.Attachment;
import io.openfactory.api.pin.model.Pin;
import java.util.ArrayList;
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

    /** Self-reference through the CDI proxy — ensures @Transactional interceptors fire
     *  when called from background threads (CompletableFuture / ForkJoinPool). */
    @Inject
    WorkpackService self;

    // -----------------------------------------------------------------------
    // Pipeline — ingest async
    // -----------------------------------------------------------------------

    /**
     * Crea el workpack en estado DONE/RAW sin correr el pipeline.
     * El usuario agrega pins en Raw/Define y luego dispara el pipeline
     * explícitamente con POST /shape.
     */
    @Transactional
    public Workpack ingest(String title, UUID ownerId, String content) {
        io.openfactory.api.user.model.User owner =
            io.openfactory.api.user.model.User.findById(ownerId);
        Workpack w = new Workpack();
        w.title            = title;
        w.owner            = owner;
        w.stage            = WorkpackStage.RAW;
        w.processingStatus = ProcessingStatus.DONE;
        w.sourceContent    = content;
        w.persist();
        return w;
    }

    void runPipelineAsync(UUID workpackId, String title, UUID ownerId, String content) {
        try {
            PipelineData data = runPipeline(workpackId, title, content);
            HandoffPackage handoff = coreHandoffService.create(
                data.snapshot.projectId(), data.brief, data.plan);
            self.finalizePipeline(workpackId, title, ownerId, content, data, handoff);
        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage()
                : e.getClass().getSimpleName() + " — check server logs";
            self.markFailed(workpackId, reason);
        }
    }

    @Transactional
    void finalizePipeline(UUID workpackId, String title, UUID ownerId, String content,
                           PipelineData data, HandoffPackage handoff) {
        Workpack w = Workpack.findById(workpackId);
        mapper.buildChildren(w, data.brief, data.plan, data.boxes, handoff);
        w.processingStatus = ProcessingStatus.DONE;
        w.stage            = WorkpackStage.SHAPE;
        w.persist();
    }

    @Transactional
    void markFailed(UUID workpackId, String reason) {
        Workpack w = Workpack.findById(workpackId);
        if (w != null) {
            w.processingStatus = ProcessingStatus.FAILED;
            w.failureReason    = reason;
            w.persist();
        }
    }

    // -----------------------------------------------------------------------
    // Pipeline — reshape (re-run sobre workpack existente)
    // -----------------------------------------------------------------------

    /**
     * Marca el workpack como PROCESSING y re-corre el pipeline en background.
     */
    @Transactional
    public Workpack reshape(UUID id) {
        Workpack w = findById(id);
        if (w.sourceContent == null || w.sourceContent.isBlank()) {
            throw new IllegalStateException(
                "Workpack " + id + " has no source content for reshaping.");
        }
        w.processingStatus = ProcessingStatus.PROCESSING;
        w.persist();
        String title   = w.title;
        String content = w.sourceContent;
        CompletableFuture.runAsync(() -> runReshapeAsync(id, title, content));
        return w;
    }

    void runReshapeAsync(UUID workpackId, String title, String content) {
        try {
            PipelineData data = runPipeline(workpackId, title, content);
            HandoffPackage handoff = coreHandoffService.create(
                data.snapshot.projectId(), data.brief, data.plan);
            self.finalizeReshape(workpackId, data, handoff);
        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage()
                : e.getClass().getSimpleName() + " — check server logs";
            self.markFailed(workpackId, reason);
        }
    }

    @Transactional
    void finalizeReshape(UUID workpackId, PipelineData data, HandoffPackage handoff) {
        Workpack w = Workpack.findById(workpackId);
        mapper.replaceChildren(w, data.brief, data.plan, data.boxes, handoff);
        w.processingStatus = ProcessingStatus.DONE;
        w.persist();
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
            w.processingStatus,
            w.failureReason,
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
        ProcessingStatus processingStatus,
        String failureReason,
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

    public List<Workpack> listForUser(UUID userId) {
        List<Workpack> owned  = Workpack.findByOwner(userId);
        List<Workpack> shared = Workpack.findSharedWith(userId);
        if (shared.isEmpty()) return owned;
        List<Workpack> result = new java.util.ArrayList<>(owned);
        shared.stream()
            .filter(s -> owned.stream().noneMatch(o -> o.id.equals(s.id)))
            .forEach(result::add);
        return result;
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

    /**
     * Construye la lista de fuentes para el pipeline.
     * Primer mensaje: sourceContent original del workpack.
     * Mensajes siguientes: pins del usuario en Raw/Define.
     * Preparado para recibir más tipos de fuente (archivos, links) en el futuro.
     */
    private List<SessionMessage> buildSources(UUID workpackId, String sourceContent) {
        List<SessionMessage> messages = new ArrayList<>();
        long ts = System.currentTimeMillis();

        if (sourceContent != null && !sourceContent.isBlank())
            messages.add(new SessionMessage(UUID.randomUUID().toString(), sourceContent, null, ts));

        List<Pin> pins = Pin.findByWorkpack(workpackId);
        for (Pin pin : pins) {
            if (pin.content != null && !pin.content.isBlank())
                messages.add(new SessionMessage(UUID.randomUUID().toString(), pin.content, null, ts));
        }

        // Text attachments (images/audio have no contentText yet)
        List<Attachment> attachments = Attachment.findByWorkpack(workpackId);
        for (Attachment a : attachments) {
            if (a.contentText != null && !a.contentText.isBlank()) {
                String msg = "## Attached file: " + a.fileName + "\n\n" + a.contentText;
                messages.add(new SessionMessage(UUID.randomUUID().toString(), msg, null, ts));
            }
        }

        return messages;
    }

    private PipelineData runPipeline(UUID workpackId, String title, String sourceContent) throws Exception {
        String sessionId = UUID.randomUUID().toString();
        String projectId = UUID.randomUUID().toString();

        List<SessionMessage> messages = buildSources(workpackId, sourceContent);

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
