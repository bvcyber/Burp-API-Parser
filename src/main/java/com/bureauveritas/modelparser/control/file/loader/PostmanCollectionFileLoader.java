package com.bureauveritas.modelparser.control.file.loader;

import com.bureauveritas.modelparser.BurpApi;
import com.bureauveritas.modelparser.control.file.handler.openapi.OpenAPIFileHandler;
import com.bureauveritas.modelparser.control.file.handler.postman.PostmanCollectionFileHandler;
import com.bureauveritas.modelparser.model.postman.FormFieldType;
import com.bureauveritas.modelparser.model.postman.PostmanCollectionModel;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import com.bureauveritas.modelparser.control.file.util.UrlParsingUtils;
import com.bureauveritas.modelparser.control.file.util.UrlParsingUtils.ProtocolDomainPathQuery;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import tools.jackson.dataformat.xml.XmlWriteFeature;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PostmanCollectionFileLoader extends AbstractModelFileLoaderChain<OpenAPI, PostmanCollectionFileHandler> {
    private static final ObjectMapper jsonMapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();
    private static final ObjectMapper rawJsonMapper = JsonMapper.builder().build();
    private static final XmlMapper rawXmlMapper = XmlMapper.builder()
        .configure(XmlWriteFeature.WRITE_XML_DECLARATION, true)
        .build();
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

    public PostmanCollectionFileLoader() {
        super(PostmanCollectionFileHandler::new);
    }

    @Override
    public OpenAPI loadModel(File file) throws Exception {
        PostmanCollectionModel collection = jsonMapper.readValue(file, PostmanCollectionModel.class);
        validateCollection(collection);

        OpenAPI openAPI = new OpenAPI();
        openAPI.setOpenapi("3.0.3");
        openAPI.setInfo(buildInfo(collection));
        openAPI.setPaths(new Paths());
        openAPI.setServers(new ArrayList<>());
        openAPI.setComponents(new Components());

        Map<String, String> variables = toVariableMap(collection.getVariable());
        Set<String> serverUrls = new LinkedHashSet<>();
        addItems(openAPI, collection.getItem(), new ArrayList<>(), variables, serverUrls);

        if (!serverUrls.isEmpty()) {
            openAPI.setServers(serverUrls.stream().map(url -> new Server().url(url)).toList());
        }

        model = openAPI;
        additionalProperties.put(OpenAPIFileHandler.UNRESOLVED_MODEL, model);
        return model;
    }

    private void validateCollection(PostmanCollectionModel collection) throws Exception {
        if (collection == null || collection.getInfo() == null || collection.getItem() == null) {
            throw new Exception("Not a valid Postman collection");
        }
    }

    private Info buildInfo(PostmanCollectionModel collection) {
        Info info = new Info();
        String title = collection.getInfo().getName();
        info.setTitle(title == null || title.isBlank() ? "Postman Collection" : title);
        String version = collection.getInfo().getVersion();
        info.setVersion(version == null || version.isBlank() ? "1.0.0" : version);
        info.setDescription(collection.getInfo().getDescription());
        return info;
    }

    private void addItems(OpenAPI openAPI,
                          List<PostmanCollectionModel.Item> items,
                          List<String> folderPath,
                          Map<String, String> variables,
                          Set<String> serverUrls) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (PostmanCollectionModel.Item item : items) {
            if (item == null) {
                continue;
            }
            if (item.isFolder()) {
                List<String> nextFolderPath = new ArrayList<>(folderPath);
                if (item.getName() != null && !item.getName().isBlank()) {
                    nextFolderPath.add(item.getName());
                }
                addItems(openAPI, item.getItem(), nextFolderPath, variables, serverUrls);
            }
            else if (item.getRequest() != null) {
                addRequest(openAPI, item, folderPath, variables, serverUrls);
            }
        }
    }

    private void addRequest(OpenAPI openAPI,
                            PostmanCollectionModel.Item item,
                            List<String> folderPath,
                            Map<String, String> variables,
                            Set<String> serverUrls) {
        PostmanCollectionModel.Request request = item.getRequest();
        String path = buildPath(request.getUrl(), variables);
        if (path == null || path.isBlank()) {
            return;
        }
        Object methodValue = request.getMethod();
        String method = methodValue != null ? methodValue.toString() : "GET";
        method = method.toUpperCase(Locale.ROOT);

        String serverUrl = buildServerUrl(request.getUrl(), variables);
        if (serverUrl != null && !serverUrl.isBlank()) {
            serverUrls.add(serverUrl);
        }

        PathItem pathItem = openAPI.getPaths().computeIfAbsent(path, ignored -> new PathItem());
        Operation operation = new io.swagger.v3.oas.models.Operation();
        if (item.getName() != null && !item.getName().isBlank()) {
            operation.setSummary(item.getName());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            operation.setDescription(request.getDescription());
        }
        if (!folderPath.isEmpty()) {
            operation.setTags(folderPath);
        }
        operation.setOperationId(buildOperationId(method, path, item.getName(), folderPath));

        List<Parameter> parameters = buildParameters(request.getUrl(), path);
        if (!parameters.isEmpty()) {
            operation.setParameters(parameters);
        }

        RequestBody requestBody = buildRequestBody(request.getBody(), request.getHeader());
        if (requestBody != null) {
            operation.setRequestBody(requestBody);
        }

        operation.setResponses(buildResponses(item.getResponse()));

        switch (method) {
            case "POST" -> pathItem.setPost(operation);
            case "PUT" -> pathItem.setPut(operation);
            case "PATCH" -> pathItem.setPatch(operation);
            case "DELETE" -> pathItem.setDelete(operation);
            case "HEAD" -> pathItem.setHead(operation);
            case "OPTIONS" -> pathItem.setOptions(operation);
            case "TRACE", "CONNECT" -> pathItem.setTrace(operation);
            default -> pathItem.setGet(operation);
        }
    }

    private String buildOperationId(String method, String path, String name, List<String> tags) {
        String base = name != null && !name.isBlank() ? name : String.format("%s %s", method, path);
        String tagPrefix = tags.isEmpty() ? "" : String.join(" ", tags) + " ";
        String raw = (tagPrefix + base)
            .replaceAll("[^A-Za-z0-9]+", " ")
            .trim();
        if (raw.isBlank()) {
            return "";
        }
        String[] parts = raw.split("\\s+");
        // build string camelCase
        StringBuilder result = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isBlank()) {
                result.append(part.substring(0, 1).toUpperCase());
                if (part.length() > 1) {
                    result.append(part.substring(1));
                }
            }
        }
        return result.toString();
    }

    private List<Parameter> buildParameters(PostmanCollectionModel.Url url, String path) {
        List<Parameter> parameters = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        if (path != null) {
            Matcher matcher = PATH_PARAM_PATTERN.matcher(path);
            while (matcher.find()) {
                String name = matcher.group(1);
                if (name != null && !name.isBlank() && seen.add(name)) {
                    Parameter param = new Parameter();
                    param.setIn("path");
                    param.setRequired(true);
                    param.setName(name);
                    param.setSchema(new StringSchema());
                    parameters.add(param);
                }
            }
        }

        if (url != null && url.getQuery() != null) {
            for (PostmanCollectionModel.QueryParam query : url.getQuery()) {
                if (query == null || Boolean.TRUE.equals(query.getDisabled())) {
                    continue;
                }
                String name = query.getKey();
                if (name == null || name.isBlank() || !seen.add("query::" + name)) {
                    continue;
                }
                Parameter param = new Parameter();
                param.setIn("query");
                param.setRequired(false);
                param.setName(name);
                param.setSchema(new StringSchema());
                if (query.getValue() != null) {
                    param.setExample(query.getValue());
                }
                if (query.getDescription() != null) {
                    param.setDescription(query.getDescription());
                }
                parameters.add(param);
            }
        }

        return parameters;
    }

    private RequestBody buildRequestBody(PostmanCollectionModel.Body body,
                                         List<PostmanCollectionModel.Header> headers) {
        if (body == null) {
            return null;
        }
        String contentType = resolveContentType(body, headers);
        Schema<?> schema = buildBodySchema(body);

        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);
        if (body.getRaw() != null && !body.getRaw().isBlank()) {
            mediaType.setExample(body.getRaw());
        }

        Content content = new Content();
        content.addMediaType(contentType, mediaType);

        RequestBody requestBody = new RequestBody();
        requestBody.setContent(content);
        return requestBody;
    }

    private Schema<?> buildBodySchema(PostmanCollectionModel.Body body) {
        String mode = getBodyMode(body);
        if ("URLENCODED".equals(mode) && body.getUrlencoded() != null) {
            return buildFormSchema(body.getUrlencoded());
        }
        if ("FORMDATA".equals(mode) && body.getFormdata() != null) {
            return buildFormSchema(body.getFormdata());
        }
        if ("GRAPHQL".equals(mode)) {
            ObjectSchema schema = new ObjectSchema();
            schema.addProperty("query", new StringSchema());
            schema.addProperty("variables", new ObjectSchema());
            return schema;
        }
        if ("RAW".equals(mode)) {
            return buildRawSchema(body);
        }
        return new StringSchema();
    }

    private Schema<?> buildRawSchema(PostmanCollectionModel.Body body) {
        String raw = body.getRaw();
        if (raw == null || raw.isBlank()) {
            return new StringSchema();
        }
        String rawType = getRawBodyType(body);
        if ("json".equals(rawType)) {
            try {
                JsonNode root = rawJsonMapper.readTree(raw);
                return buildSchemaFromJsonNode(root);
            }
            catch (Exception ignored) {
                return new StringSchema();
            }
        }
        if ("xml".equals(rawType)) {
            try {
                JsonNode root = rawXmlMapper.readTree(raw);
                return buildSchemaFromJsonNode(root).format("xml");
            }
            catch (Exception ignored) {
                return new StringSchema().format("xml");
            }
        }
        return new StringSchema();
    }

    private Schema<?> buildSchemaFromJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return new Schema<>().nullable(true);
        }
        if (node.isObject()) {
            ObjectSchema schema = new ObjectSchema();
            node.properties().forEach(entry ->
                schema.addProperty(entry.getKey(), buildSchemaFromJsonNode(entry.getValue())));
            return schema;
        }
        if (node.isArray()) {
            ArraySchema schema = new ArraySchema();
            if (!node.isEmpty()) {
                schema.setItems(buildSchemaFromJsonNode(node.get(0)));
            }
            else {
                schema.setItems(new Schema<>());
            }
            return schema;
        }
        if (node.isBoolean()) {
            return new BooleanSchema();
        }
        if (node.isNumber()) {
            return node.isIntegralNumber() ? new IntegerSchema() : new NumberSchema();
        }
        if (node.isString()) {
            return new StringSchema();
        }
        return new StringSchema();
    }

    private String getRawBodyType(PostmanCollectionModel.Body body) {
        String language = Optional.ofNullable(body.getOptions())
            .map(PostmanCollectionModel.BodyOptions::getRaw)
            .map(PostmanCollectionModel.BodyOptions.RawOptions::getLanguage)
            .orElse(null);
        if (language != null) {
            String normalized = language.toLowerCase(Locale.ROOT);
            if (normalized.contains("json")) {
                return "json";
            }
            if (normalized.contains("xml")) {
                return "xml";
            }
        }
        String raw = body.getRaw();
        if (raw != null && BurpApi.getInstance().utilities().jsonUtils().isValidJson(raw)) {
            return "json";
        }
        if (raw != null && raw.trim().startsWith("<")) {
            return "xml";
        }
        return "text";
    }

    private Schema<?> buildFormSchema(List<PostmanCollectionModel.FormParam> params) {
        ObjectSchema schema = new ObjectSchema();
        for (PostmanCollectionModel.FormParam param : params) {
            if (param == null || Boolean.TRUE.equals(param.getDisabled())) {
                continue;
            }
            String name = param.getKey();
            if (name == null || name.isBlank()) {
                continue;
            }
            schema.addProperty(name, FormFieldType.FILE.equals(param.getType()) ?
                new BinarySchema() : new StringSchema());
        }
        return schema.getProperties() == null ? new StringSchema() : schema;
    }

    private String resolveContentType(PostmanCollectionModel.Body body, List<PostmanCollectionModel.Header> headers) {
        String headerContentType = extractContentType(headers);
        if (headerContentType != null && !headerContentType.isBlank()) {
            return headerContentType;
        }
        String mode = getBodyMode(body);
        if (mode == null) {
            return "application/octet-stream";
        }
        return switch (mode) {
            case "RAW" -> {
                String lang = Optional.ofNullable(body.getOptions())
                    .map(PostmanCollectionModel.BodyOptions::getRaw)
                    .map(PostmanCollectionModel.BodyOptions.RawOptions::getLanguage)
                    .orElse(null);
                yield lang != null ? switch (lang.toLowerCase()) {
                    case "json" -> "application/json";
                    case "xml" -> "application/xml";
                    case "html" -> "text/html";
                    case "javascript" -> "application/javascript";
                    default -> "text/plain";
                } : BurpApi.getInstance().utilities().jsonUtils().isValidJson(body.getRaw()) ?
                    "application/json" : "text/plain";
            }
            case "URLENCODED" -> "application/x-www-form-urlencoded";
            case "FORMDATA" -> "multipart/form-data";
            case "FILE" -> "application/octet-stream";
            case "GRAPHQL" -> "application/json";
            default -> "application/octet-stream";
        };
    }

    private String getBodyMode(PostmanCollectionModel.Body body) {
        if (body == null) {
            return null;
        }
        Object modeValue = body.getMode();
        return modeValue == null ? null : modeValue.toString();
    }

    private String extractContentType(List<PostmanCollectionModel.Header> headers) {
        if (headers == null) {
            return null;
        }
        for (PostmanCollectionModel.Header header : headers) {
            if (header == null || Boolean.TRUE.equals(header.getDisabled())) {
                continue;
            }
            String key = header.getKey();
            if (key != null && key.equalsIgnoreCase("Content-Type")) {
                return header.getValue();
            }
        }
        return null;
    }

    private ApiResponses buildResponses(List<PostmanCollectionModel.Response> responses) {
        ApiResponses apiResponses = new ApiResponses();
        if (responses == null || responses.isEmpty()) {
            apiResponses.addApiResponse("200", new ApiResponse().description("Success"));
            return apiResponses;
        }
        for (PostmanCollectionModel.Response response : responses) {
            if (response == null || response.getCode() == null) {
                continue;
            }
            String code = String.valueOf(response.getCode());
            String description = response.getStatus() != null ? response.getStatus() : "Response";
            apiResponses.addApiResponse(code, new ApiResponse().description(description));
        }
        if (apiResponses.isEmpty()) {
            apiResponses.addApiResponse("200", new ApiResponse().description("Success"));
        }
        return apiResponses;
    }

    private String buildPath(PostmanCollectionModel.Url url, Map<String, String> variables) {
        if (url == null) {
            return null;
        }
        if (url.getPath() != null && !url.getPath().isEmpty()) {
            String path = "/" + String.join("/", url.getPath());
            return normalizePath(path);
        }
        else {
            String raw = url.getRaw();
            if (raw != null && !raw.isBlank()) {
                raw = substituteVariables(raw, variables);
                ProtocolDomainPathQuery pdpq = UrlParsingUtils.parseProtocolDomainPathQuery(raw);
                return normalizePath(pdpq != null ? pdpq.path() : null);
            }
        }
        return "/";
    }


    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        // Convert :param to {param} and {{var}} to {var} (Postman to OpenAPI path variable syntax)
        return normalized
            .replaceAll(":([A-Za-z0-9_-]+)", "{$1}")
            .replaceAll("\\{\\{([^}]+)}}", "{$1}");
    }

    private String buildServerUrl(PostmanCollectionModel.Url url, Map<String, String> variables) {
        if (url == null) {
            return null;
        }
        if (url.getProtocol() != null && url.getHost() != null && !url.getHost().isEmpty()) {
            String host = String.join(".", url.getHost());
            StringBuilder server = new StringBuilder();
            server.append(url.getProtocol()).append("://").append(host);
            if (url.getPort() != null && !url.getPort().isBlank()) {
                server.append(":").append(url.getPort());
            }
            return server.toString();
        }
        else {
            String raw = url.getRaw();
            if (raw != null && !raw.isBlank()) {
                raw = substituteVariables(raw, variables);
                ProtocolDomainPathQuery pdpq = UrlParsingUtils.parseProtocolDomainPathQuery(raw);
                if (pdpq != null && pdpq.protocol() != null) {
                    return pdpq.protocol() + pdpq.domain();
                }
            }
        }
        return null;
    }


    private Map<String, String> toVariableMap(List<PostmanCollectionModel.Variable> variables) {
        if (variables == null || variables.isEmpty()) {
            return Collections.emptyMap();
        }
        return variables.stream()
            .filter(v -> v != null && v.getKey() != null && v.getValue() != null)
            .collect(Collectors.toMap(
                PostmanCollectionModel.Variable::getKey,
                v -> String.valueOf(v.getValue())
            ));
    }

    private String substituteVariables(String input, Map<String, String> variables) {
        if (input == null || variables.isEmpty()) {
            return input;
        }
        String result = input;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String token = "{{" + entry.getKey() + "}}";
            if (result.contains(token)) {
                result = result.replace(token, entry.getValue());
            }
        }
        return result;
    }
}
