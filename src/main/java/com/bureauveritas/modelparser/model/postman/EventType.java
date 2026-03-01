package com.bureauveritas.modelparser.model.postman;

import com.fasterxml.jackson.annotation.JsonProperty;

// Event listener types
public enum EventType {
    @JsonProperty("prerequest")
    PREREQUEST,
    @JsonProperty("test")
    TEST
}
