package com.bureauveritas.modelparser.control.file.handler.mcp;

import burp.api.montoya.http.message.requests.HttpRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.regex.Pattern;

public class McpUtils {
    public static final ObjectMapper JSON_MAPPER = JsonMapper.builder()
        .changeDefaultPropertyInclusion(incl -> incl
            .withValueInclusion(JsonInclude.Include.NON_NULL)
            .withContentInclusion(JsonInclude.Include.NON_NULL))
        .build();
    public static final Pattern OPERATION_NAME_PATTERN = Pattern.compile("^([^(]+) (?:\\((\\S+)\\))?$");

    public static HttpRequest applyHttpStreamingHeaders(HttpRequest request, boolean withSessionId) {
        if (withSessionId) {
            request = request.withHeader("Mcp-Session-Id", "<SESSION_ID>");
        }
        return request.withHeader("Accept", "application/json,text/event-stream");
    }
}
