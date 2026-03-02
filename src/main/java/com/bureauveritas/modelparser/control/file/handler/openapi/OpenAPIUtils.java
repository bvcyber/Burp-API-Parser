package com.bureauveritas.modelparser.control.file.handler.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.bureauveritas.modelparser.BurpApi;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.utils.ModelUtils;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;

import java.util.Map;
import java.util.stream.Collectors;

public class OpenAPIUtils {

    private static final XmlMapper xmlMapper = XmlMapper.builder()
        .configure(XmlWriteFeature.WRITE_XML_DECLARATION, true)
        .build();

    public static CodegenOperation getCodegenOperation(String path, String method, OpenAPI model) {
        DefaultCodegen codegen = new DefaultCodegen();
        codegen.setOpenAPI(model);
        codegen.processOpts();
        return codegen.fromOperation(
            path,
            method,
            model.getPaths().get(path).readOperationsMap().get(PathItem.HttpMethod.valueOf(method.toUpperCase())),
            model.getServers()
        );
    }

    /**
     * Helper method to get operation from path and method
     */
    public static Operation getOperation(String path, String method, OpenAPI model) {
        if (model.getPaths() == null || !model.getPaths().containsKey(path)) {
            return null;
        }

        Map<PathItem.HttpMethod, Operation> operationsMap = model.getPaths().get(path).readOperationsMap();
        if (operationsMap.containsKey(PathItem.HttpMethod.valueOf(method.toUpperCase()))) {
            return operationsMap.get(PathItem.HttpMethod.valueOf(method.toUpperCase()));
        }
        return null;
    }

    public static String convertExampleToString(Example example, String contentType, String shapeName) {
        return convertExampleToString(example, contentType, shapeName, false);
    }

    /**
     * Convert Example object to string with content type awareness
     */
    public static String convertExampleToString(Example example, String contentType, String shapeName, boolean isFormData) {
        if (example.getValue() != null) {
            return convertObjectToStringByContentType(example.getValue(), contentType, shapeName, isFormData);
        }
        else if (example.getExternalValue() != null) {
            return "External: " + example.getExternalValue();
        }
        return "";
    }

    public static String convertObjectToStringByContentType(Object obj, String contentType, String shapeName) {
        return convertObjectToStringByContentType(obj, contentType, shapeName, false);

    }

    /**
     * Convert object to string based on content type
     */
    public static String convertObjectToStringByContentType(Object obj, String contentType, String shapeName, boolean isFormData) {
        BurpApi.getInstance().logging().logToOutput("convertObjectToStringByContentType called with contentType: " + contentType + ", shapeName: " + shapeName + ", isFormData: " + isFormData);
        try {
            // Already a string - return as-is
            if (obj instanceof String) {
                return (String) obj;
            }

            // Handle based on content type
            if (isFormData) {
                return convertToFormData(obj);
            }
            else if (contentType != null) {
                String contentTypeLowerCase = contentType.toLowerCase();

                // XML content types
                if (contentTypeLowerCase.contains("xml")) {
                    return convertToXML(obj, shapeName);
                }
                // YAML content types
                else if (contentTypeLowerCase.contains("yaml") || contentTypeLowerCase.contains("yml")) {
                    return convertToYAML(obj);
                }
                // Plain text
                else if (contentTypeLowerCase.contains("text/plain")) {
                    return obj.toString();
                }
                // Form data
                else if (contentTypeLowerCase.contains("application/x-www-form-urlencoded") ||
                        contentTypeLowerCase.contains("multipart/form-data") ||
                        contentTypeLowerCase.contains("multipart/mixed")) {
                    BurpApi.getInstance().logging().logToOutput("Converting object to form data: " + obj);
                    return convertToFormData(obj);
                }
            }

            // Default to JSON for application/json and unknown types
            return Json.mapper().writeValueAsString(obj);

        } catch (Exception e) {
            BurpApi.getInstance().logging().logToOutput(
                String.format("Error converting %s example to string", contentType));
            BurpApi.getInstance().logging().logToError(e);
            return obj.toString();
        }
    }

    /**
     * Convert object to XML string
     */
    public static String convertToXML(Object obj, String shapeName) {
        return xmlMapper.writer()
            .withRootName(shapeName != null ? shapeName : "root")
            .writeValueAsString(convertToMap(obj));
    }

    /**
     * Convert object to YAML string
     */
    public static String convertToYAML(Object obj) throws JsonProcessingException {
        return Yaml.mapper().writeValueAsString(convertToMap(obj));
    }

    public static Map<String, Object> convertToMap(Object obj) {
        return Json.mapper().convertValue(obj, Map.class);
    }

    /**
     * Convert object to form data string
     */
    public static String convertToFormData(Object obj) {
        try {
            if (obj instanceof String) {
                return (String) obj;
            }

            Map<String, Object> map = convertToMap(obj);
            BurpApi.getInstance().logging().logToOutput("Converting object to form data: " + map);

            return map.entrySet().stream()
                .map(entry ->
                    entry.getKey() +"=" +
                        BurpApi.getInstance().utilities().urlUtils().encode(entry.getValue().toString()))
                .collect(Collectors.joining("&"));
        } catch (Exception e) {
            BurpApi.getInstance().logging().logToError(e);
            return obj.toString();
        }
    }

    public static String getRefFromRequestBody(OpenAPI model, String path, String method, String contentType) {
        Operation op = getOperation(path, method, model);
        if (op == null || op.getRequestBody() == null || op.getRequestBody().getContent() == null
            || !op.getRequestBody().getContent().containsKey(contentType)
            || op.getRequestBody().getContent().get(contentType) == null
            || op.getRequestBody().getContent().get(contentType).getSchema() == null) {
            return null;
        }
        String ref = op.getRequestBody().getContent().get(contentType).getSchema().get$ref();
        if (ref == null) {
            return null;
        }
        return ModelUtils.getSimpleRef(ref);
    }
}
