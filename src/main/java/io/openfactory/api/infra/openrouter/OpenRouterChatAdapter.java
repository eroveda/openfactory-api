package io.openfactory.api.infra.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openfactory.core.port.ConversationalLlmPort;
import static io.openfactory.core.port.ConversationalLlmPort.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador OpenRouter para el puerto ConversationalLlmPort.
 * Usa la API OpenAI-compatible de OpenRouter para chat multi-turno con tool calling.
 */
@ApplicationScoped
public class OpenRouterChatAdapter implements ConversationalLlmPort {

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

    @ConfigProperty(name = "openrouter.model", defaultValue = "anthropic/claude-haiku-4-5")
    String model;

    @ConfigProperty(name = "openrouter.max-tokens", defaultValue = "2000")
    int maxTokens;

    @ConfigProperty(name = "openrouter.timeout-seconds", defaultValue = "60")
    int timeoutSeconds;

    @Inject
    ObjectMapper objectMapper;

    private String apiKey;
    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        this.apiKey = System.getenv("OPENROUTER_API_KEY");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        System.out.println("💬 Chat adapter: OpenRouter | model: " + model);
    }

    @Override
    public ChatResponse chat(List<ChatMessage> history, String userMessage) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("OPENROUTER_API_KEY not set");
        }

        List<ChatMessage> messages = new ArrayList<>(history);
        messages.add(new ChatMessage(ChatMessage.Role.user, userMessage));

        String body = buildRequestBody(messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENROUTER_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://openfactory.io")
                .header("X-Title", "openFactory")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenRouter error " + response.statusCode()
                    + ": " + response.body());
        }

        return parseResponse(response.body());
    }

    private String buildRequestBody(List<ChatMessage> messages) throws Exception {
        var messagesNode = objectMapper.createArrayNode();
        for (ChatMessage m : messages) {
            messagesNode.addObject()
                    .put("role", m.role().name())
                    .put("content", m.content());
        }

        var body = objectMapper.createObjectNode()
                .put("model", model)
                .put("max_tokens", maxTokens);
        body.set("messages", messagesNode);

        return objectMapper.writeValueAsString(body);
    }

    private ChatResponse parseResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");

        String content = message.path("content").asText("");

        // tool_calls support — populated when the model invokes a registered tool
        List<ToolCall> toolCalls = new ArrayList<>();
        JsonNode toolCallsNode = message.path("tool_calls");
        if (toolCallsNode.isArray()) {
            for (JsonNode tc : toolCallsNode) {
                String name = tc.path("function").path("name").asText();
                String args = tc.path("function").path("arguments").asText();
                toolCalls.add(new ToolCall(name, args));
            }
        }

        return new ChatResponse(content, toolCalls);
    }
}
