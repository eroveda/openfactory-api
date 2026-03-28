package io.openfactory.api.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openfactory.api.box.model.Box;
import io.openfactory.api.brief.model.Brief;
import io.openfactory.api.pin.model.Pin;
import io.openfactory.api.pin.model.PinType;
import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.api.workpack.model.WorkpackStage;
import io.openfactory.core.port.ConversationalLlmPort.ToolCall;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

/**
 * Ejecuta las tool calls que el modelo emite durante el chat.
 * Cada tool modifica el estado del workpack/brief/box en base al argumento JSON.
 */
@ApplicationScoped
public class ChatToolExecutor {

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public ToolResult execute(UUID workpackId, ToolCall toolCall) {
        try {
            JsonNode args = objectMapper.readTree(toolCall.argumentsJson());
            return switch (toolCall.name()) {
                case "save_context"       -> saveContext(workpackId, args);
                case "update_brief"       -> updateBrief(workpackId, args);
                case "mark_define_ready"  -> markDefineReady(workpackId);
                case "update_box"         -> updateBox(args);
                case "suggest_split"      -> suggestSplit(workpackId, args);
                default -> ToolResult.noop(toolCall.name());
            };
        } catch (Exception e) {
            System.err.println("⚠️  Tool execution failed [" + toolCall.name() + "]: " + e.getMessage());
            return ToolResult.noop(toolCall.name());
        }
    }

    // -----------------------------------------------------------------------
    // Tool implementations
    // -----------------------------------------------------------------------

    private ToolResult saveContext(UUID workpackId, JsonNode args) {
        Workpack w = Workpack.findById(workpackId);
        if (w == null) return ToolResult.noop("save_context");

        String content = args.path("content").asText("");
        String typeStr = args.path("type").asText("UNKNOWN");

        PinType pinType;
        try {
            pinType = switch (typeStr.toLowerCase()) {
                case "intent"      -> PinType.INTENT;
                case "actor"       -> PinType.ACTOR;
                case "constraint"  -> PinType.SCOPE_CONSTRAINT;
                case "scope"       -> PinType.SCOPE_CONSTRAINT;
                case "domain_fact" -> PinType.DOMAIN_FACT;
                default            -> PinType.UNKNOWN;
            };
        } catch (Exception e) {
            pinType = PinType.UNKNOWN;
        }

        Pin pin = new Pin();
        pin.workpack = w;
        pin.content = content;
        pin.type = pinType;
        pin.confidence = 0.9;
        pin.persist();

        System.out.println("📌 Chat saved pin [" + pinType + "]: " + content);
        return new ToolResult("save_context", false);
    }

    private ToolResult updateBrief(UUID workpackId, JsonNode args) {
        Brief brief = Brief.find("workpack.id", workpackId).firstResult();
        if (brief == null) return ToolResult.noop("update_brief");

        String field = args.path("field").asText("");
        String value = args.path("value").asText("");

        switch (field) {
            case "title"            -> brief.title = value;
            case "mainIdea"         -> brief.mainIdea = value;
            case "objective"        -> brief.objective = value;
            case "actors"           -> brief.actors = toJsonArray(value);
            case "scopeIncludes"    -> brief.scopeIncludes = toJsonArray(value);
            case "scopeExcludes"    -> brief.scopeExcludes = toJsonArray(value);
            case "constraints"      -> brief.constraints = toJsonArray(value);
            case "successCriteria"  -> brief.successCriteria = toJsonArray(value);
            default -> { return ToolResult.noop("update_brief"); }
        }

        brief.updatedAt = java.time.LocalDateTime.now();
        brief.persist();

        System.out.println("✏️  Chat updated brief field [" + field + "]");
        return new ToolResult("update_brief", true);
    }

    private ToolResult markDefineReady(UUID workpackId) {
        Workpack w = Workpack.findById(workpackId);
        if (w == null) return ToolResult.noop("mark_define_ready");
        w.stage = WorkpackStage.DEFINE;
        w.persist();
        System.out.println("✅ Chat marked workpack define-ready: " + workpackId);
        return new ToolResult("mark_define_ready", false);
    }

    private ToolResult updateBox(JsonNode args) {
        String boxIdStr = args.path("boxId").asText("");
        if (boxIdStr.isBlank()) return ToolResult.noop("update_box");

        UUID boxId;
        try { boxId = UUID.fromString(boxIdStr); }
        catch (Exception e) { return ToolResult.noop("update_box"); }

        Box box = Box.findById(boxId);
        if (box == null) return ToolResult.noop("update_box");

        String field = args.path("field").asText("");
        String value = args.path("value").asText("");

        switch (field) {
            case "title"              -> box.title = value;
            case "purpose"            -> box.purpose = value;
            case "instructions"       -> box.instructions = toJsonArray(value);
            case "constraints"        -> box.constraints = toJsonArray(value);
            case "acceptanceCriteria" -> box.acceptanceCriteria = toJsonArray(value);
            default -> { return ToolResult.noop("update_box"); }
        }

        box.persist();

        System.out.println("📦 Chat updated box [" + field + "]: " + boxId);
        return new ToolResult("update_box", false);
    }

    private ToolResult suggestSplit(UUID workpackId, JsonNode args) {
        // Split suggestion — stored as a pin so the user can review and confirm
        Workpack w = Workpack.findById(workpackId);
        if (w == null) return ToolResult.noop("suggest_split");

        String boxId  = args.path("boxId").asText("");
        String reason = args.path("reason").asText("");
        String box1   = args.path("box1Title").asText("");
        String box2   = args.path("box2Title").asText("");

        String content = "SPLIT_SUGGESTION box=%s reason=%s → [%s] + [%s]"
                .formatted(boxId, reason, box1, box2);

        Pin pin = new Pin();
        pin.workpack = w;
        pin.content = content;
        pin.type = PinType.UNKNOWN;
        pin.confidence = 0.8;
        pin.persist();

        System.out.println("✂️  Chat split suggestion for box: " + boxId);
        return new ToolResult("suggest_split", false);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Convierte un valor en array JSON: si ya es array lo deja, si es texto lo envuelve. */
    private String toJsonArray(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("[")) return trimmed;
        try {
            objectMapper.readTree(trimmed); // valid JSON?
            return "[" + trimmed + "]";
        } catch (Exception e) {
            // Plain string — wrap as single-element array
            try {
                return objectMapper.writeValueAsString(new String[]{value});
            } catch (Exception ex) {
                return "[\"" + value.replace("\"", "\\\"") + "\"]";
            }
        }
    }

    // -----------------------------------------------------------------------
    // Result type
    // -----------------------------------------------------------------------

    public record ToolResult(String toolName, boolean briefUpdated) {
        public static ToolResult noop(String name) { return new ToolResult(name, false); }
    }
}
