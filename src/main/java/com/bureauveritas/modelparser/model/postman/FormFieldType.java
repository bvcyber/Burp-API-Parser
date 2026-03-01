package com.bureauveritas.modelparser.model.postman;

import com.fasterxml.jackson.annotation.JsonProperty;

// Form field types
public enum FormFieldType {
    @JsonProperty("text")
    TEXT,
    @JsonProperty("file")
    FILE
}
