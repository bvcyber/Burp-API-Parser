package com.bureauveritas.modelparser.control.file.handler.mcp;

import burp.api.montoya.core.ByteArray;
import com.bureauveritas.modelparser.BurpApi;
import com.bureauveritas.modelparser.control.file.handler.AbstractModelFileHandler;
import com.bureauveritas.modelparser.control.file.serializer.AbstractSerializer;
import com.bureauveritas.modelparser.control.file.serializer.mcp.McpSerializerFactory;
import com.bureauveritas.modelparser.control.file.serializer.mcp.SerializerContext;
import com.bureauveritas.modelparser.model.mcp.McpOperationNameServerName;
import com.bureauveritas.modelparser.model.mcp.McpServerType;
import com.bureauveritas.modelparser.model.mcp.McpServersJsonFileModel;
import com.bureauveritas.modelparser.model.Settings;
import com.bureauveritas.modelparser.model.mcp.McpServersJsonFileModel.McpServer;
import com.bureauveritas.modelparser.model.mcp.McpServersJsonFileModel.SseMcpServer;
import com.bureauveritas.modelparser.model.mcp.McpServersJsonFileModel.HttpMcpServer;
import com.bureauveritas.modelparser.view.FontColorUtils;
import com.bureauveritas.modelparser.view.render.HtmlTableCellRenderer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.InspectableHttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;

import static com.bureauveritas.modelparser.control.file.handler.mcp.McpUtils.*;
import static com.bureauveritas.modelparser.model.mcp.McpRequestBuilder.*;

public class McpServersJsonFileHandler extends AbstractModelFileHandler<McpServersJsonFileModel> {
    @Getter
    private final Map<String,Map<String,McpSchema.Tool>> serverToolsMap = new HashMap<>();
    private final Map<String,String> serverEndpointMap = new HashMap<>();
    private final Map<String,ServerCapabilities> serverCapabilitiesMap = new HashMap<>();
    private final Map<String,Map<String,String>> serverHeadersMap = new HashMap<>();

    public McpServersJsonFileHandler(McpServersJsonFileModel modelObject) {
        super(modelObject);
        model.getMcpServers().forEach((serverName, server) -> {
            if (server instanceof SseMcpServer sseServer) {
                initializeSseServer(serverName, sseServer);
            }
            else if (server instanceof HttpMcpServer httpServer) {
                initializeHttpServer(serverName, httpServer);
            }
            else {
                BurpApi.getInstance().logging().logToOutput(
                    "STDIO servers are not supported: " + serverName);
            }
        });
    }

    private void initializeSseServer(String serverName, SseMcpServer server) {
        try {
            BurpApi.getInstance().logging().logToOutput("Attempting sse server: " + server.getUrl());
            ProtocolDomainPathQuery pdpq = getProtocolDomainPathQuery(server.getUrl());
            if (pdpq == null) {
                throw new RuntimeException("No protocol domain path found for server: " + serverName);
            }
            BurpApi.getInstance().logging().logToOutput("Extracted baseUri: " + pdpq.getBaseUri());
            BurpApi.getInstance().logging().logToOutput("Extracted endpoint: " + pdpq.getEndpoint());

            InspectableHttpClientSseClientTransport transport =
                new InspectableHttpClientSseClientTransport(pdpq.getBaseUri(), pdpq.getEndpoint(), server.getHeaders());
            try {
                initializeClientAndGetTools(serverName, transport);
                String messageEndpoint = transport.getMessageEndpoint().block();
                if (messageEndpoint == null) {
                    throw new RuntimeException("No message endpoint found for server: " + serverName);
                }
                if (!messageEndpoint.startsWith("/")) {
                    messageEndpoint = "/" + messageEndpoint;
                }
                serverEndpointMap.put(serverName, messageEndpoint);
                serverHeadersMap.put(serverName, server.getHeaders());
                BurpApi.getInstance().logging().logToOutput("messageEndpoint: " + messageEndpoint);
                BurpApi.getInstance().logging().logToOutput("Success");
            }
            finally {
                transport.close();
            }
        } catch (Exception e) {
            BurpApi.getInstance().logging().logToOutput("Failed: " + server.getUrl());
            BurpApi.getInstance().logging().logToError(e);
        }
    }

    private void initializeHttpServer(String serverName, HttpMcpServer server) {
        try {
            BurpApi.getInstance().logging().logToOutput("Attempting http server: " + server.getUrl());

            ProtocolDomainPathQuery pdpq = getProtocolDomainPathQuery(server.getUrl());
            if (pdpq == null) {
                throw new RuntimeException("No protocol domain path found for server: " + serverName);
            }

            Map<String,String> headers = server.getHeaders();
            var builder = HttpClientStreamableHttpTransport.builder(pdpq.getBaseUri());

            if (headers != null && !headers.isEmpty()) {
                BurpApi.getInstance().logging().logToOutput("Applying headers: " + headers);
                var requestBuilder = HttpRequest.newBuilder();
                headers.forEach(requestBuilder::header);
                builder.requestBuilder(requestBuilder);
            }
            serverHeadersMap.put(serverName, server.getHeaders());

            HttpClientStreamableHttpTransport transport = builder.endpoint(pdpq.getEndpoint()).build();
            try {
                initializeClientAndGetTools(serverName, transport);
                BurpApi.getInstance().logging().logToOutput("Extracted endpoint: " + pdpq.getEndpoint());
                serverEndpointMap.put(serverName, pdpq.getEndpoint());
                BurpApi.getInstance().logging().logToOutput("Success");
            }
            finally {
                transport.close();
            }
        }
        catch (Exception e) {
            BurpApi.getInstance().logging().logToError("Failed: " + server.getUrl());
            BurpApi.getInstance().logging().logToError(e);
        }
    }

    private void initializeClientAndGetTools(String serverName, McpClientTransport transport) {
        try (McpSyncClient client = McpClient.sync(transport)
            .requestTimeout(Duration.ofSeconds(Settings.getMcpConnectionTimeoutSeconds()))
            .build()
        ) {
            BurpApi.getInstance().logging().logToOutput("Attempting to create tools for server: " + serverName);
            client.initialize();
            client.listTools().tools().forEach(tool ->
                serverToolsMap.computeIfAbsent(serverName, k -> new HashMap<>())
                    .put(tool.name(), tool)
            );
            serverCapabilitiesMap.put(serverName, client.getServerCapabilities());
            BurpApi.getInstance().logging().logToOutput("Success");
        }
        catch (Exception e) {
            BurpApi.getInstance().logging().logToOutput("Failed getting tools: " + serverName);
            throw new RuntimeException(e);
        }
    }

    public void debug() {
        System.out.println(JSON_MAPPER.writeValueAsString(serverToolsMap));
    }

    @Override
    public Object serializeOperation(String operationName, AbstractSerializer<?> serializer,
                                     boolean includeOptionalParameters) {
        try {
            if (!serializer.getOperationName().equals(operationName)) {
                BurpApi.getInstance().logging().logToError(
                    "Got operation name '%s', but serializer is '%s'".formatted(operationName, serializer.getOperationName()));
                return getSerializersForOperation(operationName).getFirst()
                    .serializeRequest(operationName, includeOptionalParameters);
            }
            return serializer.serializeRequest(operationName, includeOptionalParameters);
        }
        catch (Exception e) {
            BurpApi.getInstance().logging().logToError(e);
            return null;
        }
    }

    private List<AbstractSerializer<?>> getSerializersForOperation(String operationName) {
        List<AbstractSerializer<?>> operationSerializers = new ArrayList<>();
        McpOperationNameServerName parsed = parseOperationName(operationName);
        if (parsed == null) {
            return operationSerializers;
        }

        McpServer server = model.getMcpServers().get(parsed.serverName());
        String pathWithQuery = serverEndpointMap.get(parsed.serverName());
        Map<String, String> headers = serverHeadersMap.get(parsed.serverName());

        // Create context for serializer factory
        SerializerContext context = new SerializerContext(pathWithQuery, operationName, server.getType(), headers);
        McpSerializerFactory factory = new McpSerializerFactory(context);

        if (parsed.operationName().startsWith("tool/")) {
            String toolName = parsed.operationName().replace("tool/", "");
            McpSchema.Tool tool = serverToolsMap.get(parsed.serverName()).get(toolName);
            operationSerializers.addAll(factory.createToolSerializers(tool, toolName));
        }
        else {
            operationSerializers.addAll(createSerializersForOperation(parsed.operationName(), factory));
        }

        return operationSerializers;
    }

    private String extractPath(McpServer server) throws URISyntaxException {
        return switch(server.getType()) {
            case SSE -> new URI(((SseMcpServer) server).getUrl()).getPath();
            case HTTP -> new URI(((HttpMcpServer) server).getUrl()).getPath();
            case STDIO -> "stdio-todo";
        };
    }

    private List<AbstractSerializer<?>> createSerializersForOperation(String operation, McpSerializerFactory factory) {
        return switch (operation) {
            case ESTABLISH_SSE -> factory.createEstablishSseSerializers();
            case INITIALIZE -> factory.createInitializeSerializers();
            case PING -> factory.createPingSerializers();
            case NOTIFICATIONS_INITIALIZED -> factory.createNotificationsInitializedSerializers();
            case LOGGING_SET_LEVEL -> factory.createLoggingSetLevelSerializers();
            case PROMPTS_LIST -> factory.createPromptsListSerializers();
            case PROMPTS_GET -> factory.createPromptsGetSerializers();
            case RESOURCES_LIST -> factory.createResourcesListSerializers();
            case RESOURCES_READ -> factory.createResourcesReadSerializers();
            case RESOURCES_SUBSCRIBE -> factory.createResourcesSubscribeSerializers();
            case RESOURCES_UNSUBSCRIBE -> factory.createResourcesUnsubscribeSerializers();
            default -> new ArrayList<>();
        };
    }

    /**
     *
     * @return true if the serializers list has changed
     */
    @Override
    public boolean updateSerializers(String operationName) {
        try {
            serializers.clear();
            addSerializers(getSerializersForOperation(operationName));
            return true;
        }
        catch (Exception e) {
            BurpApi.getInstance().logging().logToError(e);
            serializers.clear();
            return true;
        }
    }

    @Override
    public ByteArray applyHost(String host, ByteArray byteArray, String operationName) {
        try {
            String s = byteArray.toString();
            if (s != null && host != null && !host.isBlank() && s.contains("<URL>")) {
                McpOperationNameServerName parsed = parseOperationName(operationName);
                if (parsed != null) {
                    McpServer server = model.getMcpServers().get(parsed.serverName());
                    String path = extractPath(server);
                    return ByteArray.byteArray(s.replaceAll("<URL>", host + path));
                }
            }
        }
        catch (Exception e) {
            BurpApi.getInstance().logging().logToError(e);
        }
        return super.applyHost(host, byteArray, operationName);
    }

    @Override
    public List<String> getHosts(String operationName) {
        McpOperationNameServerName parsed = parseOperationName(operationName);
        if (parsed == null) {
            return super.getHosts(operationName);
        }

        List<String> hosts = new ArrayList<>();
        McpServer server = model.getMcpServers().get(parsed.serverName());
        String serverUrl = getServerUrl(server);
        ProtocolDomainPathQuery pdpq = getProtocolDomainPathQuery(serverUrl);

        if (pdpq != null) {
            hosts.add(pdpq.protocol() + pdpq.domain());
        }
        else {
            BurpApi.getInstance().logging().logToError("Warning - regex failed to match: " + serverUrl);
        }
        return hosts;
    }

    private record ProtocolDomainPathQuery(String protocol, String domain, String path, String query) {
        String getBaseUri() {
            return protocol + domain + path;
        }

        String getEndpoint() {
            return path + (query != null ? "?" + query : "");
        }
    }

    private ProtocolDomainPathQuery getProtocolDomainPathQuery(String url) {
        Matcher matcher = HTTP_PATTERN.matcher(url);
        return matcher.find() ?
            new ProtocolDomainPathQuery(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)) : null;
    }

    private String getServerUrl(McpServer server) {
        return switch(server.getType()) {
            case SSE -> ((SseMcpServer) server).getUrl();
            case HTTP -> ((HttpMcpServer) server).getUrl();
            case STDIO -> "stdio-todo";
        };
    }

    @Override
    public List<String> getOperations() {
        List<String> result = new ArrayList<>();

        // And tools and standard operations
        serverToolsMap.keySet().forEach(serverName -> {
            Function<String,String> formatter = op -> "%s (%s)".formatted(op, serverName);
            McpServerType serverType = model.getMcpServers().get(serverName).getType();

            if (serverType == McpServerType.SSE) {
                result.add(formatter.apply(ESTABLISH_SSE));
            }
            result.add(formatter.apply(INITIALIZE));
            result.add(formatter.apply(NOTIFICATIONS_INITIALIZED));
            result.add(formatter.apply(PING));

            serverToolsMap.get(serverName).keySet().stream()
                .map(toolName -> formatter.apply(TOOL_PREFIX + toolName))
                .forEach(result::add);
        });
        // Add operations based on server capabilities
        serverCapabilitiesMap.forEach((serverName, capabilities) -> {
            Function<String,String> formatter = op -> "%s (%s)".formatted(op, serverName);

            if (capabilities.prompts() != null) {
                result.add(formatter.apply(PROMPTS_LIST));
                result.add(formatter.apply(PROMPTS_GET));
            }
            if (capabilities.resources() != null) {
                result.add(formatter.apply(RESOURCES_LIST));
                result.add(formatter.apply(RESOURCES_READ));
                if (Boolean.TRUE.equals(capabilities.resources().subscribe())) {
                    result.add(formatter.apply(RESOURCES_SUBSCRIBE));
                    result.add(formatter.apply(RESOURCES_UNSUBSCRIBE));
                }
            }
            if (capabilities.logging() != null) {
                result.add(formatter.apply(LOGGING_SET_LEVEL));
            }
        });
        return result;
    }

    @Override
    public List<String> getShapeNames() {
        return new ArrayList<>(model.getMcpServers().keySet());
    }

    @Override
    public String getShapeDefinition(String shapeName) {
        return JSON_MAPPER.writeValueAsString(model.getMcpServers().get(shapeName));
    }

    @Override
    public String getOperationDefinition(String operationName) {
        McpOperationNameServerName parsed = parseOperationName(operationName);
        if (parsed == null) {
            BurpApi.getInstance().logging().logToError("Warning - operationName did not match regex");
            return null;
        }

        if (parsed.operationName().startsWith("tool/")) {
            String toolName = parsed.operationName().replace("tool/", "");
            Map<String, McpSchema.Tool> tools = serverToolsMap.get(parsed.serverName());
            return tools != null ? JSON_MAPPER.writeValueAsString(tools.get(toolName)) : null;
        }

        return switch(parsed.operationName()) {
            case ESTABLISH_SSE -> "GET call required by the MCP SSE spec to start a new SSE streaming session.\n" +
                "Tip: For Repeater, configure \"Streaming response timeout\" in Burp settings.";
            case INITIALIZE -> "Method required by the MCP spec. Send client capabilities.\n" +
                "For \"http\" servers, use this to start a new session.";
            case NOTIFICATIONS_INITIALIZED -> "Notification required by the MCP spec. " +
                "Notify the server that the client is initialized.";
            case PING -> "Method defined by the MCP spec. Check connectivity.";
            case PROMPTS_LIST -> "Method defined by the MCP spec. List available prompts (optionally with a cursor).";
            case PROMPTS_GET -> "Method defined by the MCP spec. Get a prompt by name (optionally with arguments).";
            case RESOURCES_LIST -> "Method defined by the MCP spec. List available resources (optionally with a cursor).";
            case RESOURCES_READ -> "Method defined by the MCP spec. Read a resource by URI.";
            case RESOURCES_SUBSCRIBE -> "Method defined by the MCP spec. Subscribe to resource updates by URI.";
            case RESOURCES_UNSUBSCRIBE -> "Method defined by the MCP spec. Unsubscribe from resource updates by URI.";
            case LOGGING_SET_LEVEL -> "Method defined by the MCP spec. Set logging level on the server.\n" +
                "Note: Only works if the server declares logging capability AND implements this method.";
            default -> {
                BurpApi.getInstance().logging().logToError("Warning - operationName did not match regex");
                yield null;
            }
        };
    }

    @Override
    public DefaultTableCellRenderer getOperationsTableCellRenderer() {
        return new HtmlTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                          boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, getFormattedOperationName((String) value),
                    isSelected, hasFocus, row, column);
                return this;
            }
        };
    }

    @Override
    public String getFormattedOperationName(String operationName) {
        McpOperationNameServerName parsed = parseOperationName(operationName);
        if (parsed != null) {
            return "<html>%s &nbsp;<span color=\"%s\"><i>%s</i></span></html>".formatted(
                parsed.operationName(),
                FontColorUtils.lighterHex(UIManager.getColor("Label.foreground"), 0.5f),
                parsed.serverName()
            );
        }
        return operationName;
    }

    @Override
    public String getOperationShapeName(String operationName) {
        McpOperationNameServerName parsed = parseOperationName(operationName);
        return parsed != null ? parsed.serverName() : "";
    }

    @Override
    public String getModelType() {
        return "MCP Servers JSON";
    }

    private McpOperationNameServerName parseOperationName(String operationName) {
        Matcher matcher = OPERATION_NAME_PATTERN.matcher(operationName);
        return matcher.find() ? new McpOperationNameServerName(matcher.group(1), matcher.group(2)) : null;
    }
}
