package io.openfactory.api.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openfactory.api.box.model.Box;
import io.openfactory.api.brief.model.Brief;
import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.api.workpack.model.WorkpackStage;
import io.openfactory.core.port.ConversationalLlmPort;
import io.openfactory.core.port.ConversationalLlmPort.ChatMessage;
import io.openfactory.core.port.ConversationalLlmPort.ChatMessage.Role;
import io.openfactory.core.port.ConversationalLlmPort.ChatResponse;
import io.openfactory.core.port.ConversationalLlmPort.ToolCall;
import io.openfactory.core.prompt.PromptService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ChatService {

    @Inject ConversationalLlmPort chatAdapter;
    @Inject ObjectMapper objectMapper;
    @Inject ChatToolExecutor toolExecutor;
    @Inject PromptService prompts;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    @Transactional
    public ChatReply chat(UUID workpackId, String userMessage) throws Exception {
        Workpack workpack = Workpack.findById(workpackId);
        if (workpack == null) throw new IllegalArgumentException("Workpack not found");

        Brief brief = Brief.find("workpack.id", workpackId).firstResult();
        List<ChatMessage> history = loadHistory(workpack);

        // First message — inject stage-appropriate system prompt
        if (history.isEmpty()) {
            history.add(new ChatMessage(Role.system, buildSystemPrompt(workpack, brief)));
        }

        ChatResponse response = chatAdapter.chat(history, userMessage);

        // Execute tool calls
        List<String> toolsExecuted = new ArrayList<>();
        boolean briefUpdated = false;
        if (response.hasToolCalls()) {
            for (ToolCall tc : response.toolCalls()) {
                ChatToolExecutor.ToolResult result = toolExecutor.execute(workpackId, tc);
                toolsExecuted.add(tc.name());
                if (result.briefUpdated()) briefUpdated = true;
            }
        }

        history.add(new ChatMessage(Role.user, userMessage));
        history.add(new ChatMessage(Role.assistant, response.content()));
        persistHistory(workpack, history);

        return new ChatReply(response.content(), toolsExecuted, briefUpdated);
    }

    @Transactional
    public void clearHistory(UUID workpackId) {
        Workpack w = Workpack.findById(workpackId);
        if (w != null) {
            w.chatHistory = "[]";
            w.persist();
        }
    }

    // -----------------------------------------------------------------------
    // System prompt — loaded from /prompts/chat_*.txt, rendered with context
    // -----------------------------------------------------------------------

    private String buildSystemPrompt(Workpack workpack, Brief brief) throws Exception {
        boolean isShape = workpack.stage == WorkpackStage.SHAPE || workpack.stage == WorkpackStage.BOX;
        String templateFile = isShape ? "chat_shape.txt" : "chat_define.txt";

        String title         = workpack.title != null ? workpack.title : "Untitled";
        String stage         = workpack.stage != null ? workpack.stage.name() : "RAW";
        String briefStatus   = brief != null ? brief.status.name() : "NONE";
        String missingSignals = brief != null ? extractMissingSignals(brief.readinessSignals) : "all";
        String currentBrief  = brief != null ? summarizeBrief(brief) : "No brief yet.";

        if (isShape) {
            String boxesSummary = buildBoxesSummary(workpack.id);
            return prompts.loadAndRender(templateFile,
                    "{{TITLE}}", title,
                    "{{CURRENT_BRIEF}}", currentBrief,
                    "{{BOXES_SUMMARY}}", boxesSummary);
        }

        return prompts.loadAndRender(templateFile,
                "{{TITLE}}", title,
                "{{STAGE}}", stage,
                "{{BRIEF_STATUS}}", briefStatus,
                "{{MISSING_SIGNALS}}", missingSignals,
                "{{CURRENT_BRIEF}}", currentBrief);
    }

    private String buildBoxesSummary(UUID workpackId) {
        List<Box> boxes = Box.list("workpack.id", workpackId);
        if (boxes.isEmpty()) return "No boxes yet.";
        return boxes.stream()
                .map(b -> "- [" + b.id + "] " + b.title + ": " + (b.purpose != null ? b.purpose : "no purpose"))
                .collect(Collectors.joining("\n"));
    }

    // -----------------------------------------------------------------------
    // History persistence
    // -----------------------------------------------------------------------

    private List<ChatMessage> loadHistory(Workpack workpack) {
        try {
            var type = objectMapper.getTypeFactory()
                    .constructCollectionType(ArrayList.class, ChatMessage.class);
            List<ChatMessage> history = objectMapper.readValue(
                    workpack.chatHistory != null ? workpack.chatHistory : "[]", type);
            return new ArrayList<>(history);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void persistHistory(Workpack workpack, List<ChatMessage> history) {
        try {
            workpack.chatHistory = objectMapper.writeValueAsString(history);
            workpack.persist();
        } catch (Exception e) {
            // best-effort
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String summarizeBrief(Brief brief) {
        StringBuilder sb = new StringBuilder();
        if (brief.mainIdea != null)     sb.append("- Main idea: ").append(brief.mainIdea).append("\n");
        if (brief.objective != null)    sb.append("- Objective: ").append(brief.objective).append("\n");
        if (brief.actors != null && !brief.actors.equals("[]"))
            sb.append("- Actors: ").append(brief.actors).append("\n");
        if (brief.scopeIncludes != null && !brief.scopeIncludes.equals("[]"))
            sb.append("- Scope includes: ").append(brief.scopeIncludes).append("\n");
        if (brief.constraints != null && !brief.constraints.equals("[]"))
            sb.append("- Constraints: ").append(brief.constraints).append("\n");
        return sb.isEmpty() ? "Brief is empty." : sb.toString();
    }

    private String extractMissingSignals(String readinessSignalsJson) {
        if (readinessSignalsJson == null) return "all";
        try {
            var node = objectMapper.readTree(readinessSignalsJson);
            List<String> missing = new ArrayList<>();
            node.fields().forEachRemaining(e -> { if (!e.getValue().asBoolean()) missing.add(e.getKey()); });
            return missing.isEmpty() ? "none" : String.join(", ", missing);
        } catch (Exception e) {
            return "unknown";
        }
    }

    // -----------------------------------------------------------------------
    // Response type
    // -----------------------------------------------------------------------

    public record ChatReply(String reply, List<String> toolsExecuted, boolean briefUpdated) {}
}
