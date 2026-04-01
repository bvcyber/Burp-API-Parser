package io.modelcontextprotocol.client.transport;

import com.bureauveritas.modelparser.BurpApi;
import com.bureauveritas.modelparser.model.Settings;
import io.modelcontextprotocol.client.transport.customizer.McpAsyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

/**
 * We need the messageEndpoint retrieved after initialize. But HttpClientSseClientTransport has it protected
 * Use this class to make it accessible
 */
public class InspectableHttpClientSseClientTransport extends HttpClientSseClientTransport {
    public InspectableHttpClientSseClientTransport(String baseUri, String sseEndpoint, Map<String, String> headers) {
        super(
            HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(Settings.getMcpConnectionTimeoutSeconds()))
                .build(),
            applyHeaders(HttpRequest.newBuilder(), headers),
            baseUri,
            sseEndpoint,
            new JacksonMcpJsonMapper(new JsonMapper()),
            McpAsyncHttpClientRequestCustomizer.NOOP
        );
    }

    private static HttpRequest.Builder applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            BurpApi.getInstance().logging().logToOutput("Applying headers: " + headers);
            headers.forEach(builder::header);
        }
        return builder;
    }

    public Mono<String> getMessageEndpoint() {
        return this.messageEndpointSink.asMono();
    }
}
