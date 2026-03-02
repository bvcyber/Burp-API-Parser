package com.bureauveritas.modelparser.model.postman;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

// Variable/Field types
public enum VariableType {
    @JsonProperty("number")
    NUMBER,
    @JsonProperty("boolean")
    BOOLEAN,
    @JsonProperty("string")
    STRING,
    @JsonEnumDefaultValue
    @JsonProperty("any")
    ANY
}
