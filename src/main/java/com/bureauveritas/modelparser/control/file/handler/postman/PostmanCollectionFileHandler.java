package com.bureauveritas.modelparser.control.file.handler.postman;

import com.bureauveritas.modelparser.control.file.handler.openapi.OpenAPIFileHandler;
import com.bureauveritas.modelparser.model.postman.PostmanCollectionModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.models.OpenAPI;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostmanCollectionFileHandler extends OpenAPIFileHandler {
    public static final String POSTMAN_MODEL = "POSTMAN_MODEL";
    public static final String OPERATION_ITEM_MAP = "OPERATION_ITEM_MAP";
    private static final ObjectMapper jsonMapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
        .build();
    private final Pattern operationIdPattern = Pattern.compile("\\(\"(.+)\"\\)");
    private PostmanCollectionModel collection;
    private Map<String, PostmanCollectionModel.Item> operationItemMap;

    public PostmanCollectionFileHandler(OpenAPI modelObject) {
        super(modelObject);
    }

    @Override
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        super.setAdditionalProperties(additionalProperties);
        if (additionalProperties.containsKey(POSTMAN_MODEL) &&
            additionalProperties.get(POSTMAN_MODEL) instanceof PostmanCollectionModel
        ) {
            collection = (PostmanCollectionModel) additionalProperties.get(POSTMAN_MODEL);
        }
        if (additionalProperties.containsKey(OPERATION_ITEM_MAP) &&
            additionalProperties.get(OPERATION_ITEM_MAP) instanceof Map
        ) {
            operationItemMap = (Map<String, PostmanCollectionModel.Item>) additionalProperties.get(OPERATION_ITEM_MAP);
        }
    }

    @Override
    public String getModelType() {
        return "Postman Collection";
    }

    @Override
    public String getOperationDefinition(String operationName) {
        if (operationItemMap != null) {
            Matcher matcher = operationIdPattern.matcher(operationName);
            if (matcher.find()) {
                String operationId = matcher.group(1);
                return jsonMapper.writeValueAsString(operationItemMap.get(operationId));
            }
        }
        return null;
    }
}
