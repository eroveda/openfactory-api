package io.openfactory.api.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openfactory.api.brief.model.Brief;
import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.core.port.ConversationalLlmPort;
import io.openfactory.core.port.ConversationalLlmPort.ChatMessage;
import io.openfactory.core.port.ConversationalLlmPort.ChatMessage.Role;
import io.openfactory.core.port.ConversationalLlmPort.ChatResponse;
import io.openfactory.core.port.ConversationalLlmPort.ToolCall;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ChatService {

    @Inject
    ConversationalLlmPort chatAdapter;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ChatToolExecutor toolExecutor;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    @Transactional
    public ChatReply chat(UUID workpackId, String userMessage) throws Exception {
        Workpack workpack = Workpack.findById(workpackId);
        if (workpack == null) throw new IllegalArgumentException("Workpack not found");

        Brief brief = Brief.find("workpack.id", workpackId).firstResult();

        List<ChatMessage> history = loadHistory(workpack);

        // First message in the conversation — inject system prompt
        if (history.isEmpty()) {
            history.add(new ChatMessage(Role.system, buildSystemPrompt(workpack, brief)));
        }

        ChatResponse response = chatAdapter.chat(history, userMessage);

        // Execute tool calls before replying
        List<String> toolsExecuted = new ArrayList<>();
        boolean briefUpdated = false;
        if (response.hasToolCalls()) {
            for (ToolCall tc : response.toolCalls()) {
                ChatToolExecutor.ToolResult result = toolExecutor.execute(workpackId, tc);
                toolsExecuted.add(tc.name());
                if (result.briefUpdated()) briefUpdated = true;
            }
        }

        // Persist new messages
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
    // Internal
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
            // best-effort — never fail the response over history persistence
        }
    }

    private String buildSystemPrompt(Workpack workpack, Brief brief) {
        String stage = workpack.stage != null ? workpack.stage.name() : "RAW";
        String briefStatus = brief != null ? brief.status.name() : "NONE";
        String title = workpack.title != null ? workpack.title : "Untitled";

        String missingSignals = "";
        if (brief != null && brief.readinessSignals != null) {
            missingSignals = extractMissingSignals(brief.readinessSignals);
        }

        String currentBrief = brief != null ? summarizeBrief(brief) : "No brief yet.";

        return """
                You are an openFactory assistant helping a user clarify and refine their work.

                Current workpack:
                - Title: %s
                - Stage: %s
                - Brief status: %s
                - Missing signals: %s

                Current brief state:
                %s

                Your goals depend on the stage:
                - RAW / DEFINE: Help the user clarify their idea until all readiness signals are green.
                  Ask ONE question at a time. When you learn something concrete, call save_context.
                  When you can improve a brief field, call update_brief.
                  When all signals are clear, call mark_define_ready.
                - SHAPE: Help the user refine boxes. When asked about a specific box,
                  call update_box to persist improvements. Suggest splits with suggest_split.

                Rules:
                - Always respond in the same language the user writes in.
                - Be concise and conversational. Never generate bullet lists of questions.
                - When you have enough to call a tool, call it — don't just describe what you would do.
                """.formatted(title, stage, briefStatus, missingSignals, currentBrief);
    }

    private String summarizeBrief(Brief brief) {
        StringBuilder sb = new StringBuilder();
        if (brief.mainIdea != null) sb.append("- Main idea: ").append(brief.mainIdea).append("\n");
        if (brief.objective != null) sb.append("- Objective: ").append(brief.objective).append("\n");
        if (brief.actors != null && !brief.actors.equals("[]")) sb.append("- Actors: ").append(brief.actors).append("\n");
        if (brief.scopeIncludes != null && !brief.scopeIncludes.equals("[]")) sb.append("- Scope includes: ").append(brief.scopeIncludes).append("\n");
        if (brief.constraints != null && !brief.constraints.equals("[]")) sb.append("- Constraints: ").append(brief.constraints).append("\n");
        return sb.isEmpty() ? "Brief is empty." : sb.toString();
    }

    private String extractMissingSignals(String readinessSignalsJson) {
        try {
            var node = objectMapper.readTree(readinessSignalsJson);
            List<String> missing = new ArrayList<>();
            node.fields().forEachRemaining(entry -> {
                if (!entry.getValue().asBoolean()) {
                    missing.add(entry.getKey());
                }
            });
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
