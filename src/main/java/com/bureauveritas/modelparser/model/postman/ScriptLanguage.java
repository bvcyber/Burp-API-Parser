package com.bureauveritas.modelparser.model.postman;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

// Script language types
public enum ScriptLanguage {
    @JsonEnumDefaultValue
    @JsonProperty("text/javascript")
    JAVASCRIPT
}
