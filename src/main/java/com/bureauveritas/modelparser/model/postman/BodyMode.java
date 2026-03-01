package com.bureauveritas.modelparser.model.postman;

import com.fasterxml.jackson.annotation.JsonProperty;

// Request body modes
public enum BodyMode {
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
