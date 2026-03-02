package com.bureauveritas.modelparser.control.file.handler;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.bureauveritas.modelparser.BurpApi;
import com.bureauveritas.modelparser.control.JMESPathUtils;
import com.bureauveritas.modelparser.control.file.serializer.AbstractSerializer;
import com.bureauveritas.modelparser.control.file.serializer.SerializerType;
import lombok.Getter;
import lombok.Setter;

import javax.swing.table.DefaultTableCellRenderer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractModelFileHandler<T> implements iModelFileHandler {

    private JMESPathUtils jmesPathFileContent;
    @Getter
    protected T model;
    protected LinkedHashMap<String, AbstractSerializer<?>> serializers = new LinkedHashMap<>();
    @Getter
    private String fileContent;
    @Setter
    @Getter
    protected Map<String,Object> additionalProperties = new HashMap<>();
    private static final Pattern httpPattern = Pattern.compile("^(https?://)?([^/:]+)(?::(\\d+))?(.*)");
    private static final String DEFAULT_HOST = "fixme";
    private static final String DEFAULT_HOST_URL = "http://" + DEFAULT_HOST;

    protected AbstractModelFileHandler(T modelObject) {
        model = modelObject;
    }

    protected void addSerializers(AbstractSerializer<?>... serializer) {
        for (AbstractSerializer<?> s : serializer) {
            addSerializer(s);
        }
    }

    protected void addSerializers(Collection<AbstractSerializer<?>> serializers) {
        for (AbstractSerializer<?> s : serializers) {
            addSerializer(s);
        }
    }

    protected void addSerializer(AbstractSerializer<?> serializer) {
        serializers.put(serializer.toString(), serializer);
    }

    public void setFileContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("fileContent cannot be null");
        }
        if (fileContent == null) {
            fileContent = content;
            jmesPathFileContent = new JMESPathUtils(getFileContent());
        }
        else {
            throw new RuntimeException("File content should only be set once");
        }
    }

    protected String queryJMESPathOnFileContent(String query) {
        if (jmesPathFileContent == null) {
            throw new RuntimeException(
                "JMESPathUtils instance is not initialized. Ensure that file content is set before querying.");
        }
        return jmesPathFileContent.executeQuery(query);
    }

    @Override
    public List<AbstractSerializer<?>> getSerializers() {
        return serializers.values().stream().toList();
    }

    @Override
    public SerializerType getSerializerType(String serializerName) {
        return serializers.get(serializerName).getSerializerType();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public Object serializeOperation(
        String operationName,
        AbstractSerializer<?> serializer,
        boolean includeOptionalParameters
    ) {
        return serializer.serializeRequest(operationName, includeOptionalParameters);
    }

    @Override
    public List<String> getOperations() {
        return List.of();
    }

    @Override
    public List<String> getShapeNames() {
        return List.of();
    }

    @Override
    public String getServiceName() {
        return "";
    }

    @Override
    public String getModelType() {
        return "";
    }

    @Override
    public String getOperationMethod(String operationName) {
        return "";
    }

    @Override
    public String getOperationDefinition(String operationName) {
        return "";
    }

    @Override
    public String getOperationPath(String operationName) {
        return "";
    }

    @Override
    public String getOperationShapeName(String operationName) {
        return "";
    }

    @Override
    public String getShapeDefinition(String shapeName) {
        return "";
    }

    /**
     *
     * @return true if the serializers list has changed
     */
    @Override
    public boolean updateSerializers(String operationName) {
        return true;
    }

    @Override
    public List<String> getHosts(String operationName) {
        return List.of();
    }

    @Override
    public HttpRequest applyAuth(String auth, HttpRequest httpRequest) {
        return httpRequest;
    }

    @Override
    public HttpRequest applyHost(String host, HttpRequest httpRequest) {
        if (host == null || host.isBlank() || host.equals("/")) {
            host = DEFAULT_HOST_URL; // missing Host makes burp unhappy
        }
        Matcher matcher = httpPattern.matcher(host);
        if (matcher.find()) {
            String extractedHttp = matcher.group(1);
            String extractedHost = matcher.group(2);
            String extractedPort = matcher.group(3);
            String extractedPath = matcher.group(4);
            HttpService httpService;
            try {
                if (extractedHttp != null && extractedHost != null && extractedPath != null) {
                    httpService = HttpService.httpService(host);
                }
                else if (extractedPort != null) {
                    // if it's just the hostname, we have to define the protocol. Default to http://
                    httpService = HttpService.httpService(host, Integer.parseInt(extractedPort), false);
                }
                else {
                    httpService = HttpService.httpService(host, false);
                }
                httpRequest = httpRequest
                    .withService(httpService)
                    .withRemovedHeader("host") // aws, botocore sets a blank "host" header
                    .withHeader("Host", extractedHost);
                // Prepend base path if present
                if (extractedPath != null && !extractedPath.isEmpty()) {
                    httpRequest = httpRequest.withPath(extractedPath + httpRequest.path());
                }
            }
            catch (Exception e) {
                BurpApi.getInstance().logging().logToOutput("Failed to apply host: " + host);
                BurpApi.getInstance().logging().logToError(e);
                httpRequest = httpRequest
                    .withService(HttpService.httpService(DEFAULT_HOST_URL))
                    .withRemovedHeader("host") // aws, botocore sets a blank "host" header
                    .withHeader("Host", DEFAULT_HOST);
            }
        }
        return httpRequest;
    }

    @Override
    public ByteArray applyHost(String host, ByteArray byteArray, String operationName) {
        // For SerializerType.RAW
        return byteArray;
    }

    @Override
    public List<String> getAuthOptions(String operationName) {
        return List.of();
    }

    @Override
    public DefaultTableCellRenderer getOperationsTableCellRenderer() {
        return new DefaultTableCellRenderer();
    }

    @Override
    public String getFormattedOperationName(String operationName) {
        return operationName;
    }
}
