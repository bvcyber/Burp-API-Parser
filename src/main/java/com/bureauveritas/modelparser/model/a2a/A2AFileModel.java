package com.bureauveritas.modelparser.model.a2a;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@JsonDeserialize
@JsonSerialize
public class A2AFileModel {
    @JsonProperty("servers")
    public List<A2AServerModel> servers;

    public record A2AServerModel(
        @JsonProperty("name")
        String name,
        @JsonProperty("description")
        String description,
        @JsonProperty("url")
        String url
    ) {}
}
