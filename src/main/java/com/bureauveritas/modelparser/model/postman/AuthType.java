package com.bureauveritas.modelparser.model.postman;

import com.fasterxml.jackson.annotation.JsonProperty;

// Auth types
public enum AuthType {
    @JsonProperty("apikey")
    APIKEY,
    @JsonProperty("bearer")
    BEARER,
    @JsonProperty("basic")
    BASIC,
    @JsonProperty("oauth2")
    OAUTH2,
    @JsonProperty("awsv4")
    AWSV4,
    @JsonProperty("digest")
    DIGEST,
    @JsonProperty("hawk")
    HAWK,
    @JsonProperty("ntlm")
    NTLM
}
