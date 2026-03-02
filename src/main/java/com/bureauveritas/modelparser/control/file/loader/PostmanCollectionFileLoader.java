package com.bureauveritas.modelparser.control.file.loader;

import com.bureauveritas.modelparser.BurpApi;
import com.bureauveritas.modelparser.control.file.handler.openapi.OpenAPIFileHandler;
import com.bureauveritas.modelparser.control.file.handler.postman.PostmanCollectionFileHandler;
import com.bureauveritas.modelparser.model.postman.FormFieldType;
import com.bureauveritas.modelparser.model.postman.PostmanCollectionModel;
import org.snakeyaml.engine.v2.api.LoadSettings;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
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
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PostmanCollectionFileLoader extends AbstractModelFileLoaderChain<OpenAPI, PostmanCollectionFileHandler> {
    private static final ObjectMapper jsonMapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        .build();
    private static final ObjectMapper rawJsonMapper = JsonMapper.builder().build();
    private static final XmlMapper rawXmlMapper = XmlMapper.builder()
        .configure(XmlWriteFeature.WRITE_XML_DECLARATION, true)
        .build();
    private static final YAMLMapper yamlMapper = YAMLMapper.builder(
        YAMLFactory.builder()
            .loadSettings(LoadSettings.builder().setCodePointLimit(1024 * 1024 * 1024).build())
            .build())
        .build();
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");
    private final Map<String, PostmanCollectionModel.Item> operationItemMap = new HashMap<>();

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
        applyCollectionAuth(openAPI, collection.getAuth());
        addItems(openAPI, collection.getItem(), new ArrayList<>(), variables, serverUrls, collection.getAuth());

        if (!serverUrls.isEmpty()) {
            openAPI.setServers(serverUrls.stream().map(url -> new Server().url(url)).toList());
        }

        model = openAPI;
        additionalProperties.put(OpenAPIFileHandler.UNRESOLVED_MODEL, model);
        additionalProperties.put(PostmanCollectionFileHandler.POSTMAN_MODEL, collection);
        additionalProperties.put(PostmanCollectionFileHandler.OPERATION_ITEM_MAP, operationItemMap);
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
                          Set<String> serverUrls,
                          PostmanCollectionModel.Auth inheritedAuth) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (PostmanCollectionModel.Item item : items) {
            if (item == null) {
                continue;
            }
            PostmanCollectionModel.Auth itemAuth = item.getAuth() != null ? item.getAuth() : inheritedAuth;
            // Traverse folders recursively, applying auth inheritance and collecting server URLs along the way
            if (item.isFolder()) {
                List<String> nextFolderPath = new ArrayList<>(folderPath);
                if (item.getName() != null && !item.getName().isBlank()) {
                    nextFolderPath.add(item.getName());
                }
                addItems(openAPI, item.getItem(), nextFolderPath, variables, serverUrls, itemAuth);
            }
            else if (item.getRequest() != null) {
                addRequest(openAPI, item, folderPath, variables, serverUrls, itemAuth);
            }
        }
    }

    private void addRequest(OpenAPI openAPI,
                            PostmanCollectionModel.Item item,
                            List<String> folderPath,
                            Map<String, String> variables,
                            Set<String> serverUrls,
                            PostmanCollectionModel.Auth inheritedAuth) {
        PostmanCollectionModel.Request request = item.getRequest();
        String path = buildPath(request.getUrl(), variables);
        if (path == null || path.isBlank()) {
            return;
        }
        Object methodValue = request.getMethod();
        String method = methodValue != null ? methodValue.toString() : "GET";
        method = method.toUpperCase();

        String serverUrl = buildServerUrl(request.getUrl(), variables);
        if (serverUrl != null && !serverUrl.isBlank()) {
            serverUrls.add(serverUrl);
        }

        PathItem pathItem = openAPI.getPaths().computeIfAbsent(path, ignored -> new PathItem());
        Operation operation = new Operation();
        if (item.getName() != null && !item.getName().isBlank()) {
            operation.setSummary(item.getName());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            operation.setDescription(request.getDescription());
        }
        if (!folderPath.isEmpty()) {
            operation.setTags(folderPath);
        }

        String operationId = buildOperationId(method, path, item.getName(), folderPath);
        operation.setOperationId(operationId);
        operationItemMap.put(operationId, item);

        List<Parameter> parameters = buildParameters(request.getUrl(), path);
        List<Parameter> headerParams = buildHeaderParameters(request.getHeader(), variables);
        if (!headerParams.isEmpty()) {
            parameters.addAll(headerParams);
        }
        if (!parameters.isEmpty()) {
            operation.setParameters(parameters);
        }

        RequestBody requestBody = buildRequestBody(request.getBody(), request.getHeader());
        if (requestBody != null) {
            operation.setRequestBody(requestBody);
        }

        PostmanCollectionModel.Auth effectiveAuth = resolveAuth(request.getAuth(), item.getAuth(), inheritedAuth);
        applyAuthToOperation(openAPI, operation, effectiveAuth);

        operation.setResponses(buildResponses(item.getResponse()));

        switch (method) {
            case "POST" -> pathItem.setPost(operation);
            case "PUT" -> pathItem.setPut(operation);
            case "PATCH" -> pathItem.setPatch(operation);
            case "DELETE" -> pathItem.setDelete(operation);
            case "HEAD" -> pathItem.setHead(operation);
            case "OPTIONS" -> pathItem.setOptions(operation);
            case "TRACE", "CONNECT" -> pathItem.setTrace(operation);
            case "GET" -> pathItem.setGet(operation);
        }
    }

    private List<Parameter> buildHeaderParameters(List<PostmanCollectionModel.Header> headers, Map<String, String> variables) {
        if (headers == null || headers.isEmpty()) {
            return new ArrayList<>();
        }
        List<Parameter> parameters = new ArrayList<>();
        for (PostmanCollectionModel.Header header : headers) {
            if (header == null) {
                continue;
            }
            String key = header.getKey();
            if (key == null || key.isBlank() || key.equalsIgnoreCase("Content-Type")) {
                continue;
            }
            Parameter param = new Parameter();
            param.setIn("header");
            param.setName(key);
            param.setSchema(new StringSchema());
            if (header.getValue() != null) {
                String val = substituteVariables(header.getValue(), variables);
                param.setExample(val);
            }
            if (header.getDescription() != null) {
                param.setDescription(header.getDescription());
            }
            parameters.add(param);
        }
        return parameters;
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
                if (query == null) {
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
        if ("RAW".equals(mode)) {
            return buildRawSchema(body);
        }
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
        return new StringSchema();
    }

    private Schema<?> buildRawSchema(PostmanCollectionModel.Body body) {
        String raw = body.getRaw();
        if (raw == null || raw.isBlank()) {
            return new StringSchema();
        }
        String rawType = getRawBodyType(body);
        if ("json".equalsIgnoreCase(rawType)) {
            try {
                JsonNode root = rawJsonMapper.readTree(raw);
                return buildSchemaFromJsonNode(root);
            }
            catch (Exception ignored) {
                return new StringSchema();
            }
        }
        else if ("xml".equalsIgnoreCase(rawType)) {
            try {
                JsonNode root = rawXmlMapper.readTree(raw);
                return buildSchemaFromJsonNode(root).format("xml");
            }
            catch (Exception ignored) {
                return new StringSchema().format("xml");
            }
        }
        else if ("yaml".equalsIgnoreCase(rawType)) {
            try {
                JsonNode root = yamlMapper.readTree(raw);
                return buildSchemaFromJsonNode(root).format("yaml");
            }
            catch (Exception ignored) {
                return new StringSchema().format("yaml");
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
            if (normalized.contains("yaml")) {
                return "yaml";
            }
        }
        String raw = body.getRaw();
        if (raw != null) {
            if (BurpApi.getInstance().utilities().jsonUtils().isValidJson(raw)) {
                return "json";
            }
            if (raw.trim().startsWith("<")) {
                return "xml";
            }
            if (raw.contains(":") || raw.contains("-")) {
                try {
                    yamlMapper.readTree(raw);
                    return "yaml";
                } catch (Exception ignored) {
                }
            }
        }
        return "text";
    }

    private Schema<?> buildFormSchema(List<PostmanCollectionModel.FormParam> params) {
        ObjectSchema schema = new ObjectSchema();
        for (PostmanCollectionModel.FormParam param : params) {
            if (param == null) {
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
                    case "yaml" -> "application/yaml";
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
        if (headers != null) {
            for (PostmanCollectionModel.Header header : headers) {
                if (header == null) {
                    continue;
                }
                String key = header.getKey();
                if (key != null && key.equalsIgnoreCase("Content-Type")) {
                    return header.getValue();
                }
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

    private void applyCollectionAuth(OpenAPI openAPI, PostmanCollectionModel.Auth auth) {
        if (auth == null || auth.getType() == null || isNoAuth(auth)) {
            return;
        }
        SecurityScheme scheme = buildSecurityScheme(auth);
        if (scheme == null) {
            return;
        }
        Components components = openAPI.getComponents();
        if (components == null) {
            components = new Components();
            openAPI.setComponents(components);
        }
        Map<String, SecurityScheme> schemes = components.getSecuritySchemes();
        if (schemes == null) {
            schemes = new LinkedHashMap<>();
            components.setSecuritySchemes(schemes);
        }
        String schemeName = buildSchemeName(auth, scheme);
        schemes.putIfAbsent(schemeName, scheme);

        SecurityRequirement requirement = new SecurityRequirement();
        requirement.addList(schemeName, Collections.emptyList());
        openAPI.addSecurityItem(requirement);
    }

    private PostmanCollectionModel.Auth resolveAuth(PostmanCollectionModel.Auth requestAuth,
                                                    PostmanCollectionModel.Auth itemAuth,
                                                    PostmanCollectionModel.Auth inheritedAuth) {
        if (requestAuth != null) {
            return requestAuth;
        }
        if (itemAuth != null) {
            return itemAuth;
        }
        return inheritedAuth;
    }

    private void applyAuthToOperation(OpenAPI openAPI, Operation operation, PostmanCollectionModel.Auth auth) {
        if (auth == null || auth.getType() == null) {
            return;
        }
        if (isNoAuth(auth)) {
            operation.setSecurity(List.of());
            return;
        }
        SecurityScheme scheme = buildSecurityScheme(auth);
        if (scheme == null) {
            return;
        }
        Components components = openAPI.getComponents();
        if (components == null) {
            components = new Components();
            openAPI.setComponents(components);
        }
        Map<String, SecurityScheme> schemes = components.getSecuritySchemes();
        if (schemes == null) {
            schemes = new LinkedHashMap<>();
            components.setSecuritySchemes(schemes);
        }
        String schemeName = buildSchemeName(auth, scheme);
        schemes.putIfAbsent(schemeName, scheme);

        SecurityRequirement requirement = new SecurityRequirement();
        requirement.addList(schemeName, Collections.emptyList());
        operation.addSecurityItem(requirement);
    }

    private boolean isNoAuth(PostmanCollectionModel.Auth auth) {
        return auth != null && auth.getType() != null &&
            "noauth".equalsIgnoreCase(auth.getType().toString());
    }

    private SecurityScheme buildSecurityScheme(PostmanCollectionModel.Auth auth) {
        String type = auth.getType().toString().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "apikey" -> buildApiKeyScheme(auth);
            case "bearer" -> new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer");
            case "basic" -> new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic");
            case "oauth2" -> new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new io.swagger.v3.oas.models.security.OAuthFlows());
            case "awsv4" -> new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("aws4");
            case "digest" -> new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("digest");
            case "hawk" -> new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("hawk");
            case "ntlm" -> new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("ntlm");
            default -> null;
        };
    }

    private SecurityScheme buildApiKeyScheme(PostmanCollectionModel.Auth auth) {
        Map<String, String> attrs = authAttributesToMap(auth.getApikey());
        String inValue = attrs.getOrDefault("in", "header").toLowerCase(Locale.ROOT);
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .name(attrs.getOrDefault("key", "X-API-KEY"))
            .in("query".equals(inValue) ? SecurityScheme.In.QUERY : SecurityScheme.In.HEADER);
    }

    private Map<String, String> authAttributesToMap(List<PostmanCollectionModel.AuthAttribute> attrs) {
        if (attrs == null) {
            return new HashMap<>();
        }
        return attrs.stream()
            .filter(attr -> attr != null && attr.getKey() != null)
            .collect(Collectors.toMap(
                PostmanCollectionModel.AuthAttribute::getKey,
                attr -> attr.getValue() == null ? "" : String.valueOf(attr.getValue())
            ));
    }

    private String buildSchemeName(PostmanCollectionModel.Auth auth, SecurityScheme scheme) {
        String type = auth.getType().toString().toLowerCase(Locale.ROOT);
        if (scheme.getType() == SecurityScheme.Type.APIKEY) {
            String in = scheme.getIn() != null ? scheme.getIn().name().toLowerCase(Locale.ROOT) : "header";
            return "postman_" + type + "_" + in + "_" + scheme.getName();
        }
        return "postman_" + type;
    }
}
