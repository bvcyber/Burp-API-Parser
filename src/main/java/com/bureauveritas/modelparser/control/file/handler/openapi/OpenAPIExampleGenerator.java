package com.bureauveritas.modelparser.control.file.handler.openapi;

import com.bureauveritas.modelparser.BurpApi;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.utils.ModelUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class OpenAPIExampleGenerator {

    private static String getSampleParamValue(CodegenParameter param,
                                              String method,
                                              String path,
                                              String contentType,
                                              OpenAPI model,
                                              OpenAPI unresolvedModel,
                                              boolean isFormData) {
        String exampleValue = new OpenAPIExamplesExtractor(model, unresolvedModel)
            .getRandomParameterExample(path, method, param.baseName, null);
        if (exampleValue != null && !exampleValue.isEmpty()) {
            return exampleValue;
        }
//        if (contentType != null) {
//            // Generate sample value if no example found
//            String generatedSample = OpenAPIExampleGenerator.generate(
//                model,
//                unresolvedModel,
//                method,
//                path,
//                contentType,
//                isFormData
//            );
//            if (generatedSample != null && !generatedSample.isEmpty()) {
//                return generatedSample;
//            }
//        }
        return OpenAPISampleGenerator.getSampleValue(param);
    }

    public static String generate(OpenAPI model,
                                  OpenAPI unresolvedModel,
                                  String method,
                                  String path,
                                  String contentType,
                                  boolean isFormData) {
        try {
            BurpApi.getInstance().logging().logToOutput("Generating example for %s %s %s".formatted(
                method, path, contentType));
            if (isFormData) {
                // Use a different method for form data
                // TODO: fix form data generation
                CodegenOperation operation = OpenAPIUtils.getCodegenOperation(path, method, model);
                return operation.formParams.stream()
                    .map(param -> param.baseName + "=" +
                        BurpApi.getInstance().utilities().urlUtils().encode(
                            getSampleParamValue(param,
                                operation.httpMethod,
                                operation.path,
                                contentType,
                                model,
                                unresolvedModel,
                                isFormData)
                        ))
                    .collect(Collectors.joining("&"));
            }
            Content content = unresolvedModel.getPaths().get(path).readOperationsMap()
                .get(PathItem.HttpMethod.valueOf(method.toUpperCase())).getRequestBody()
                .getContent();
            if (content == null) {
                // Fallback to resolved model if unresolved model has no content (may be  issues in parser library)
                content = model.getPaths().get(path).readOperationsMap()
                    .get(PathItem.HttpMethod.valueOf(method.toUpperCase())).getRequestBody()
                    .getContent();
            }
            Schema<?> schema = content.get(contentType).getSchema();

            String generatedExample = new ExampleGenerator(
                model.getComponents().getSchemas(),
                unresolvedModel
            ).generate(
                null,
                new ArrayList<>(List.of("application/json")), // ExampleGenerator works best with JSON, then we convert later if needed
                schema
            ).getFirst().get("example");

            if (generatedExample.equals("null") || generatedExample.equals("\"{}\"")) {
                return contentType.contains("json") || contentType.equals("*/*") ? "{}" : "";
            }
            // TODO: improve sample generation; ExampleGenerator simply uses the param name as the value

            // If content type isn't explicitly JSON, try convert it
            if (!contentType.equals("application/json") && !generatedExample.isEmpty()) {
                String convertedBodyContent = OpenAPIUtils.convertObjectToStringByContentType(
                    Json.mapper().readTree(generatedExample),
                    contentType,
                    OpenAPIUtils.getRefFromRequestBody(
                        unresolvedModel,
                        path,
                        method,
                        contentType
                    )
                );
                return convertedBodyContent != null &&
                    !convertedBodyContent.isEmpty() &&
                    !convertedBodyContent.equals("null") ?
                    convertedBodyContent : generatedExample;
            }

            return generatedExample.equals("\"\"") ? "" : generatedExample;
        }
        catch (Exception e) {
            BurpApi.getInstance().logging().logToOutput(
                "Error generating example for %s %s %s".formatted(method, path, contentType));
            BurpApi.getInstance().logging().logToError(e.getMessage());
            return null;
        }
    }

    // TODO: maybe use com.securityinnovation.simodelparse.control.JsonSchemaPlaceholderGenerator instead
    // Copied class below from https://github.com/swagger-api/swagger-codegen/blob/master/modules/swagger-codegen/src/main/java/io/swagger/codegen/examples/ExampleGenerator.java
    // ^ not actively used. Updated here to work with certain examples

    /*
     * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
     * Copyright 2018 SmartBear Software
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *     https://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

    public static class ExampleGenerator {
//        private final Logger LOGGER = LoggerFactory.getLogger(org.openapitools.codegen.examples.ExampleGenerator.class);

        // TODO: move constants to more appropriate location
        private static final String MIME_TYPE_JSON = "application/json";

        protected final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

        private static final String EXAMPLE = "example";
        private static final String CONTENT_TYPE = "contentType";
        private static final String GENERATED_CONTENT_TYPE = "generatedContentType";
        private static final String OUTPUT = "output";
        private static final String NONE = "none";
        private static final String URL = "url";
        private static final String URI = "uri";

        protected Map<String, Schema> examples;
        private final OpenAPI openAPI;
        private final Random random;

        public ExampleGenerator(Map<String, Schema> examples, OpenAPI openAPI) {
            this.examples = examples;
            this.openAPI = openAPI;
            // use a fixed seed to make the "random" numbers reproducible.
            this.random = new Random("ExampleGenerator".hashCode());
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        public List<Map<String, String>> generate(Map<String, Object> examples, List<String> mediaTypes, Schema property) {
            // LOGGER.debug("debugging generate in ExampleGenerator");
            List<Map<String, String>> output = new ArrayList<>();
            Set<String> processedModels = new HashSet<>();
            if (examples == null) {
                if (mediaTypes == null) {
                    // assume application/json for this
                    mediaTypes = Collections.singletonList(MIME_TYPE_JSON); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
                }
                for (String mediaType : mediaTypes) {
                    Map<String, String> kv = new HashMap<>();
                    kv.put(CONTENT_TYPE, mediaType);
                    if (property != null && (mediaType.startsWith(MIME_TYPE_JSON) || mediaType.contains("*/*"))) {
                        String example = Json.pretty(resolvePropertyToExample("", mediaType, property, processedModels));
                        if (example != null) {
                            kv.put(EXAMPLE, example);
                            kv.put(GENERATED_CONTENT_TYPE, MIME_TYPE_JSON);
                            output.add(kv);
                        }
                    }
                }
            } else {
                for (Map.Entry<String, Object> entry : examples.entrySet()) {
                    final Map<String, String> kv = new HashMap<>();
                    kv.put(CONTENT_TYPE, entry.getKey());
                    kv.put(EXAMPLE, Json.pretty(entry.getValue()));
                    output.add(kv);
                }
            }

            if (output.isEmpty()) {
                Map<String, String> kv = new HashMap<>();
                kv.put(OUTPUT, NONE);
                output.add(kv);
            }
            return output;
        }

        private Object resolvePropertyToExample(String propertyName, String mediaType, Schema property, Set<String> processedModels) {
            if (property == null) {
                // LOGGER.error("Property schema shouldn't be null. Please report the issue to the openapi-generator team.");
                return null;
            }

            // LOGGER.debug("Resolving example for property {}...", property);
            if (property.getExample() != null) {
                // LOGGER.debug("Example set in openapi spec, returning example: '{}'", property.getExample().toString());
                // When a property is of type Date, we want to ensure that we're returning a formatted Date.
                // And not returning the Date object directly.
                if (property.getExample() instanceof Date) {
                    return dateFormat.format(property.getExample());
                }
                return property.getExample();
            } else if (ModelUtils.isBooleanSchema(property)) {
                Object defaultValue = property.getDefault();
                if (defaultValue != null) {
                    return defaultValue;
                }
                return Boolean.TRUE;
            } else if (ModelUtils.isArraySchema(property)) {
                Schema innerType = ModelUtils.getSchemaItems(property);
                if (innerType != null) {
                    int arrayLength = null == property.getMaxItems() ? 2 : property.getMaxItems();
                    // avoid memory issues by limiting to max. 5 items
                    arrayLength = Math.min(arrayLength, 5);
                    Object[] objectProperties = new Object[arrayLength];
                    Object objProperty = resolvePropertyToExample(propertyName, mediaType, innerType, processedModels);
                    for (int i = 0; i < arrayLength; i++) {
                        objectProperties[i] = objProperty;
                    }
                    return objectProperties;
                }
            } else if (ModelUtils.isDateSchema(property)) {
                return "2000-01-23";
            } else if (ModelUtils.isDateTimeSchema(property)) {
                return "2000-01-23T04:56:07.000+00:00";
            } else if (ModelUtils.isNumberSchema(property)) {
                Double min = getPropertyValue(property.getMinimum());
                Double max = getPropertyValue(property.getMaximum());
                if (ModelUtils.isFloatSchema(property)) { // float
                    return (float) randomNumber(min, max);
                } else if (ModelUtils.isDoubleSchema(property)) { // decimal/double
                    return BigDecimal.valueOf(randomNumber(min, max));
                } else { // no format defined
                    return randomNumber(min, max);
                }
            } else if (ModelUtils.isFileSchema(property)) {
                return "<file>";  // TODO

            } else if (ModelUtils.isIntegerSchema(property)) {
                Double min = getPropertyValue(property.getMinimum());
                Double max = getPropertyValue(property.getMaximum());
                if (ModelUtils.isLongSchema(property)) {
                    return (long) randomNumber(min, max);
                }
                return (int) randomNumber(min, max);
            } else if (ModelUtils.isMapSchema(property)) {
                Map<String, Object> mp = new HashMap<>();
                if (property.getName() != null) {
                    mp.put(property.getName(),
                        resolvePropertyToExample(propertyName, mediaType, ModelUtils.getAdditionalProperties(property), processedModels));
                } else {
                    mp.put("key",
                        resolvePropertyToExample(propertyName, mediaType, ModelUtils.getAdditionalProperties(property), processedModels));
                }
                return mp;
            } else if (ModelUtils.isUUIDSchema(property)) {
                return "01234567-89ab-cdef-ffed-cba9876543210";
            } else if (ModelUtils.isURISchema(property)) {
                return "https://example.com";
            } else if (ModelUtils.isStringSchema(property)) {
                // LOGGER.debug("String property");
                if (property.getDefault() != null) {
                    return String.valueOf(property.getDefault());
                }
                List<String> enumValues = property.getEnum();
                if (enumValues != null && !enumValues.isEmpty()) {
                    // LOGGER.debug("Enum value found: '{}'", enumValues.get(0));
                    return enumValues.getFirst();
                }
                String format = property.getFormat();
                if (URI.equals(format) || URL.equals(format)) {
                    // LOGGER.debug("URI or URL format, without default or enum, generating random one.");
                    return "http://example.com/aeiou";
                }
                // LOGGER.debug("No values found, using property name {} as example", propertyName);
                return propertyName;
            }
            else if (!StringUtils.isEmpty(property.get$ref())) { // model
                String simpleName = ModelUtils.getSimpleRef(property.get$ref());
                Schema schema = ModelUtils.getSchema(openAPI, simpleName);
                if (schema == null) { // couldn't find the model/schema
                    return "{}";
                }
                return resolveModelToExample(simpleName, mediaType, schema, processedModels);
            }
            else if (ModelUtils.isObjectSchema(property) ||
                ModelUtils.isAllOf(property) || ModelUtils.isAllOfWithProperties(property) ||
                ModelUtils.isAnyOf(property) || ModelUtils.isOneOf(property)
            ) {
                // FIXED: parse the ObjectSchema. Originally this always returned empty {}
                return resolveModelToExample(
                    property.getName() == null ?
                        propertyName :
                        property.getName(),
//                    property.getName() == null ? Integer.toHexString(property.hashCode()) : property.getName(),
                    mediaType,
                    property,
                    processedModels
                );
            }

            return null; // Don't use if could not identify a value
        }

        private Double getPropertyValue(BigDecimal propertyValue) {
            return propertyValue == null ? null : propertyValue.doubleValue();
        }

        private double randomNumber(Double min, Double max) {
            if (min != null && max != null) {
                double range = max - min;
                return random.nextDouble() * range + min;
            } else if (min != null) {
                return random.nextDouble() + min;
            } else if (max != null) {
                return random.nextDouble() * max;
            } else {
                return random.nextDouble() * 10;
            }
        }

        private Object resolveModelToExample(String name, String mediaType, Schema schema, Set<String> processedModels) {
            if (name == null) {
                name = Integer.toHexString(schema.hashCode());
            }
            if (processedModels.contains(name)) {
                return schema.getExample();
            }

            processedModels.add(name);
            Map<String, Object> values = new HashMap<>();
            // LOGGER.debug("Resolving model '{}' to example", name);
            if (schema.getExample() != null) {
                // LOGGER.debug("Using example from spec: {}", schema.getExample());
                return schema.getExample();
            } else if (schema.getProperties() != null) {
                // LOGGER.debug("Creating example from model values");
                traverseSchemaProperties(mediaType, schema, processedModels, values);
                schema.setExample(values);
                return schema.getExample();
            } else if (ModelUtils.isAllOf(schema) || ModelUtils.isAllOfWithProperties(schema)) {
                // LOGGER.debug("Resolving allOf model '{}' to example", name);

                Object primitiveResult = resolveAllOfToPrimitive(name, mediaType, schema, processedModels);
                if (primitiveResult != null) {
                    return primitiveResult;
                }

                resolveAllOfSchemaProperties(mediaType, schema, processedModels, values);
                schema.setExample(values);
                return schema.getExample();
            } else if (ModelUtils.isAnyOf(schema) || ModelUtils.isOneOf(schema)) {
                // LOGGER.debug("Resolving anyOf/oneOf model '{}' using the first schema to example", name);
                Optional<Schema> found = ModelUtils.getInterfaces(schema)
                    .stream()
                    .filter(this::hasValidRef)
                    .findFirst();

                if (found.isEmpty()) {
                    return null;
                }
                return resolvePropertyToExample(name, mediaType, found.get(), processedModels);
            } else if (ModelUtils.isArraySchema(schema) || ModelUtils.isEnumSchema(schema)) {
                return resolvePropertyToExample(schema.getName(), mediaType, schema, processedModels);
            } else {
                // TODO log an error message as the model does not have any properties
                return null;
            }
        }

        /**
         * Check if allOf composition results in a primitive type (string, number, boolean, etc.)
         * rather than an object with properties
         * return null if it does not resolve to a primitive
         */
        private Object resolveAllOfToPrimitive(String name, String mediaType, Schema schema, Set<String> processedModels) {
            if (schema.getAllOf() == null || schema.getAllOf().isEmpty()) {
                return null;
            }

            // Merge all schemas in allOf to determine final type
            Schema mergedSchema = new Schema();

            for (Object composed : schema.getAllOf()) {
                Schema composedSchema = (Schema) composed;

                // Resolve $ref if present
                if (composedSchema.get$ref() != null) {
                    String ref = ModelUtils.getSimpleRef(composedSchema.get$ref());
                    composedSchema = ModelUtils.getSchema(openAPI, ref);
                    if (composedSchema == null) {
                        continue;
                    }
                }

                // If any schema has properties, treat as object
                if (composedSchema.getProperties() != null && !composedSchema.getProperties().isEmpty()) {
                    return null;
                }

                // Merge type information
                if (composedSchema.getType() != null) {
                    mergedSchema.setType(composedSchema.getType());
                }
                if (composedSchema.getFormat() != null) {
                    mergedSchema.setFormat(composedSchema.getFormat());
                }
                if (composedSchema.getEnum() != null) {
                    mergedSchema.setEnum(composedSchema.getEnum());
                }
                if (composedSchema.getMinimum() != null) {
                    mergedSchema.setMinimum(composedSchema.getMinimum());
                }
                if (composedSchema.getMaximum() != null) {
                    mergedSchema.setMaximum(composedSchema.getMaximum());
                }
                if (composedSchema.getDefault() != null) {
                    mergedSchema.setDefault(composedSchema.getDefault());
                }
            }

            // If merged type is primitive, resolve as primitive
            if (mergedSchema.getType() != null && !mergedSchema.getType().equals("object")) {
                return resolvePropertyToExample(name, mediaType, mergedSchema, processedModels);
            }

            return null;
        }

        private void traverseSchemaProperties(String mediaType, Schema schema, Set<String> processedModels, Map<String, Object> values) {
            if (schema.getProperties() != null) {
                for (Object propertyName : schema.getProperties().keySet()) {
                    Schema property = (Schema) schema.getProperties().get(propertyName.toString());
                    Object propertyValue = resolvePropertyToExample(propertyName.toString(), mediaType, property, processedModels);
                    if (propertyValue != null) {
                        values.put(propertyName.toString(), propertyValue);
                    }
                }
            } else if (ModelUtils.isAllOf(schema) || ModelUtils.isAllOfWithProperties(schema)) {
                resolveAllOfSchemaProperties(mediaType, schema, processedModels, values);
            }
        }

        /**
         * Transverse and resolves all property examples for `allOf` composed schemas into `values` map object
         *
         * @param mediaType       MIME type
         * @param schema          OAS schema
         * @param processedModels Set containing all processed models
         * @param values          Example value map
         */
        private void resolveAllOfSchemaProperties(String mediaType, Schema schema, Set<String> processedModels, Map<String, Object> values) {
            List<Schema> interfaces = schema.getAllOf();
            for (Schema composed : interfaces) {
                traverseSchemaProperties(mediaType, composed, processedModels, values);
                if (composed.get$ref() != null) {
                    String ref = ModelUtils.getSimpleRef(composed.get$ref());
                    Schema resolved = ModelUtils.getSchema(openAPI, ref);
                    if (resolved != null) {
                        traverseSchemaProperties(mediaType, resolved, processedModels, values);
                    }
                }
            }
        }

        private boolean hasValidRef(Schema schema) {
            if (schema.get$ref() != null) {
                String ref = ModelUtils.getSimpleRef(schema.get$ref());
                Schema resolved = ModelUtils.getSchema(openAPI, ref);
                return resolved != null;
            }

            return true;
        }
    }
}
