package io.openfactory.api.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openfactory.api.brief.model.Brief;
import io.openfactory.api.handoff.model.Handoff;
import io.openfactory.api.plan.model.ExecutionPlanEntity;
import io.openfactory.core.box.evaluator.BoxReadinessEvaluator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class SimulationService {

    @Inject
    BoxReadinessEvaluator boxEvaluator;

    @Inject
    ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Result types
    // -----------------------------------------------------------------------

    public record SimulationResult(
        String status,           // READY | NEEDS_FIXES | BLOCKED
        Completeness completeness,
        List<BoxSimulation> sequence,
        List<String> planFindings
    ) {}

    public record Completeness(
        boolean briefPresent,
        boolean boxesPresent,
        boolean planPresent,
        boolean handoffPresent
    ) {}

    public record BoxSimulation(
        String boxId,
        String title,
        int order,
        boolean parallel,
        boolean checkpoint,
        int readinessScore,   // 0-100
        boolean ready,
        List<String> gaps
    ) {}

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    public SimulationResult simulate(UUID workpackId) {
        List<io.openfactory.api.box.model.Box> dbBoxes =
            io.openfactory.api.box.model.Box.list("workpack.id = ?1 order by orderIndex", workpackId);

        Brief brief = Brief.findByWorkpack(workpackId);
        ExecutionPlanEntity plan = ExecutionPlanEntity.findByWorkpack(workpackId);
        Handoff handoff = Handoff.findByWorkpack(workpackId);

        Map<String, JsonNode> stepsByTitle = parseStepsByTitle(plan);
        List<String> planFindings = parsePlanFindings(plan);

        List<BoxSimulation> sequence = new ArrayList<>();
        for (io.openfactory.api.box.model.Box dbBox : dbBoxes) {
            io.openfactory.core.box.model.Box coreBox = toCoreBox(dbBox);
            BoxReadinessEvaluator.ReadinessScore score = boxEvaluator.evaluateBox(coreBox);

            JsonNode step = stepsByTitle.get(dbBox.title);
            boolean parallel   = step != null && step.path("parallel").asBoolean(false);
            boolean checkpoint = step != null && step.path("checkpoint").asBoolean(false);

            sequence.add(new BoxSimulation(
                dbBox.id.toString(),
                dbBox.title,
                dbBox.orderIndex,
                parallel,
                checkpoint,
                (int) Math.round(score.overall() * 100),
                score.isReady(),
                score.gaps()
            ));
        }

        Completeness completeness = new Completeness(
            brief != null,
            !dbBoxes.isEmpty(),
            plan != null,
            handoff != null && handoff.handoffNotes != null && !handoff.handoffNotes.isBlank()
        );

        long notReadyCount = sequence.stream().filter(s -> !s.ready()).count();
        boolean hasErrors   = planFindings.stream().anyMatch(f -> f.startsWith("[ERROR]"));

        String status;
        if (notReadyCount == 0 && !hasErrors)                                   status = "READY";
        else if (hasErrors || (sequence.size() > 0 && notReadyCount > sequence.size() / 2)) status = "BLOCKED";
        else                                                                     status = "NEEDS_FIXES";

        return new SimulationResult(status, completeness, sequence, planFindings);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private io.openfactory.core.box.model.Box toCoreBox(io.openfactory.api.box.model.Box dbBox) {
        return io.openfactory.core.box.model.Box.create("project", dbBox.nodeId != null ? dbBox.nodeId : "node")
            .withTitle(dbBox.title)
            .withPurpose(dbBox.purpose)
            .withInputContext(dbBox.inputContext)
            .withExpectedOutput(dbBox.expectedOutput)
            .withInstructions(parseJsonList(dbBox.instructions))
            .withAcceptanceCriteria(parseJsonList(dbBox.acceptanceCriteria))
            .withConstraints(parseJsonList(dbBox.constraints))
            .withHandoff(dbBox.handoff);
    }

    /** Build a title → step node map for matching plan metadata to DB boxes. */
    private Map<String, JsonNode> parseStepsByTitle(ExecutionPlanEntity plan) {
        Map<String, JsonNode> map = new HashMap<>();
        if (plan == null || plan.steps == null) return map;
        try {
            JsonNode arr = objectMapper.readTree(plan.steps);
            if (!arr.isArray()) return map;
            for (JsonNode step : arr) {
                String title = step.path("boxTitle").asText("");
                if (!title.isBlank()) map.put(title, step);
            }
        } catch (Exception ignored) {}
        return map;
    }

    private List<String> parsePlanFindings(ExecutionPlanEntity plan) {
        List<String> result = new ArrayList<>();
        if (plan == null || plan.findings == null) return result;
        try {
            JsonNode arr = objectMapper.readTree(plan.findings);
            if (!arr.isArray()) return result;
            for (JsonNode f : arr) {
                String sev = f.path("severity").asText("INFO");
                String msg = f.path("message").asText("");
                if (!msg.isBlank()) result.add("[" + sev + "] " + msg);
            }
        } catch (Exception ignored) {}
        return result;
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (!arr.isArray()) return List.of();
            List<String> result = new ArrayList<>();
            for (JsonNode item : arr) result.add(item.asText());
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }
}
