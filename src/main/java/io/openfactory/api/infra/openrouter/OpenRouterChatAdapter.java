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
        body.set("tools", buildToolDefinitions());

        return objectMapper.writeValueAsString(body);
    }

    /**
     * Tool definitions enviadas a OpenRouter en cada request.
     * Deben estar alineadas con los handlers en ChatToolExecutor.
     */
    private com.fasterxml.jackson.databind.node.ArrayNode buildToolDefinitions() {
        var tools = objectMapper.createArrayNode();

        tools.add(tool("save_context", "Persist something the user just clarified as a context pin",
                param("content", "string", "The content to save as a pin"),
                param("type", "string", "Pin type: intent | actor | constraint | scope | domain_fact")));

        tools.add(tool("update_brief", "Update a specific field of the workpack brief",
                param("field", "string", "Field name: title | mainIdea | objective | actors | scopeIncludes | scopeExcludes | constraints | successCriteria"),
                param("value", "string", "New value for the field (plain string or JSON array)")));

        tools.add(tool("mark_define_ready", "Mark the workpack as ready to proceed to Shape stage", new String[0]));

        tools.add(tool("update_box", "Update a specific field of a work box",
                param("boxId", "string", "UUID of the box to update"),
                param("field", "string", "Field: title | purpose | instructions | constraints | acceptanceCriteria"),
                param("value", "string", "New value")));

        tools.add(tool("suggest_split", "Suggest splitting a box that is too broad into two focused boxes",
                param("boxId", "string", "UUID of the box to split"),
                param("reason", "string", "Why this box should be split"),
                param("box1Title", "string", "Title of the first resulting box"),
                param("box2Title", "string", "Title of the second resulting box")));

        return tools;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode tool(String name, String description, String[]... params) {
        var props = objectMapper.createObjectNode();
        for (String[] p : params) {
            props.putObject(p[0]).put("type", p[1]).put("description", p[2]);
        }

        var required = objectMapper.createArrayNode();
        for (String[] p : params) required.add(p[0]);

        var fn = objectMapper.createObjectNode()
                .put("name", name)
                .put("description", description);
        var schema = objectMapper.createObjectNode().put("type", "object");
        schema.set("properties", props);
        schema.set("required", required);
        fn.set("parameters", schema);

        return objectMapper.createObjectNode().put("type", "function").set("function", fn);
    }

    private String[] param(String name, String type, String description) {
        return new String[]{name, type, description};
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
