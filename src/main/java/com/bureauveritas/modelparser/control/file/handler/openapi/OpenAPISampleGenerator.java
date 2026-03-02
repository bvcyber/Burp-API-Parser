package com.bureauveritas.modelparser.control.file.handler.openapi;

import org.openapitools.codegen.CodegenParameter;

public class OpenAPISampleGenerator {
    private static final java.util.Random random = new java.util.Random();

    public static String getSampleValue(CodegenParameter param) {
        if (param.defaultValue != null && !param.defaultValue.isEmpty() && !param.defaultValue.equals("null")) {
            return param.defaultValue;
        }

        // Note: example and examples can be inconsistent in the Codegen library
        if (param.example != null && !param.example.isEmpty()) {
            return param.example;
        }
        if (param.examples != null && !param.examples.isEmpty()) {
            // Get a random example
            return param.examples.entrySet().stream().toList()
                .get(random.nextInt(param.examples.size())).getValue().toString();
        }
        if (param.getContent() != null && !param.getContent().isEmpty()) {
            // Get a random content type example
            Object example = param.getContent().entrySet().stream().toList()
                .get(random.nextInt(param.getContent().size())).getValue().getExample();
            if (example != null && !example.toString().isEmpty()) {
                return example.toString();
            }
        }

        // If no default values, create a placeholder
        String paramName = param.paramName != null ? param.paramName : "value"; // Use parameter name as hint
        if (param.isString) {
            return "<string>";
        }
        if (param.isInteger || param.isLong) {
            return "0";
        }
        if (param.isNumber || param.isFloat || param.isDouble) {
            return "0.0";
        }
        if (param.isBoolean) {
            return "<boolean>";
        }
        if (param.isDate) {
            return "<date>";
        }
        if (param.isDateTime) {
            return "<datetime>";
        }
        if (param.isUuid) {
            return "<uuid>";
        }
        if (param.isArray) {
            return "[]";
        }
        if (param.isMap) {
            return "{}";
        }

        // Generic fallback
        return String.format("<%s>", paramName);
    }
}
