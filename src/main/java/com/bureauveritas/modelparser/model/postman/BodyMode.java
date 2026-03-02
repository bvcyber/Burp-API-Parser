package com.bureauveritas.modelparser.model.postman;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

// Request body modes
public enum BodyMode {
    @JsonEnumDefaultValue
    @JsonProperty("raw")
    RAW,
    @JsonProperty("urlencoded")
    URLENCODED,
    @JsonProperty("formdata")
    FORMDATA,
    @JsonProperty("file")
    FILE,
    @JsonProperty("graphql")
    GRAPHQL
}
