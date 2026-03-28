package io.openfactory.api.infra.anthropic;

import io.openfactory.core.port.LlmPort;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Adaptador Anthropic para el puerto LlmPort.
 * Usado por el pipeline batch (BriefBuilder, OutlineService, BoxGenerator, etc.).
 */
@ApplicationScoped
public class AnthropicLlmAdapter implements LlmPort {

    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";

    @ConfigProperty(name = "provider.model", defaultValue = "claude-haiku-4-5-20251001")
    String model;

    @ConfigProperty(name = "provider.max-tokens", defaultValue = "2500")
    int maxTokens;

    @ConfigProperty(name = "provider.timeout-seconds", defaultValue = "30")
    int timeoutSeconds;

    @ConfigProperty(name = "provider.max-retries", defaultValue = "3")
    int maxRetries;

    private String apiKey;
    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        this.apiKey = System.getenv("ANTHROPIC_API_KEY");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        System.out.println("🤖 LLM adapter: Anthropic | model: " + model);
        System.out.println("⚙️  max-tokens: " + maxTokens + " | timeout: " + timeoutSeconds + "s | retries: " + maxRetries);
    }

    @Override
    public String complete(String prompt) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("ANTHROPIC_API_KEY not set");
        }

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return doComplete(prompt);
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long waitMs = 1000L * attempt;
                    System.err.printf("⚠️  Anthropic attempt %d failed: %s — retrying in %dms%n",
                            attempt, e.getMessage(), waitMs);
                    Thread.sleep(waitMs);
                }
            }
        }
        throw new RuntimeException("All " + maxRetries + " attempts failed", lastException);
    }

    private String doComplete(String prompt) throws Exception {
        String body = """
                {
                  "model": "%s",
                  "max_tokens": %d,
                  "messages": [
                    { "role": "user", "content": %s }
                  ]
                }
                """.formatted(model, maxTokens, toJsonString(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Anthropic error " + response.statusCode()
                    + ": " + response.body());
        }

        return extractContent(response.body());
    }

    private String extractContent(String json) {
        int start = json.indexOf("\"text\":") + 7;
        start = json.indexOf("\"", start) + 1;
        int end = json.indexOf("\"", start);
        while (json.charAt(end - 1) == '\\') {
            end = json.indexOf("\"", end + 1);
        }
        String raw = json.substring(start, end);
        return raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String toJsonString(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
