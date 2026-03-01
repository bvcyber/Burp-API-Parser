package com.bureauveritas.modelparser.model.postman;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

// Root collection
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostmanCollectionModel {
    @JsonProperty("info")
    private Info info;

    @JsonProperty("item")
    private List<Item> item;

    @JsonProperty("variable")
    private List<Variable> variable;

    @JsonProperty("auth")
    private Auth auth;

    @JsonProperty("event")
    private List<Event> event;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Info {
        @JsonProperty("name")
        private String name;

        @JsonProperty("_postman_id")
        private String postmanId;

        @JsonProperty("description")
        private String description;  // can also be a Description object

        @JsonProperty("schema")
        private String schema;  // e.g. "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"

        @JsonProperty("version")
        private String version;
    }

    // Item can be either a Request item OR a Folder (ItemGroup)
    // The key distinguishing factor: folders have "item" array, requests have "request"
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        // Present if this is a REQUEST item
        @JsonProperty("request")
        private Request request;

        @JsonProperty("response")
        private List<Response> response;

        @JsonProperty("event")
        private List<Event> event;

        // Present if this is a FOLDER (ItemGroup)
        @JsonProperty("item")
        private List<Item> item;

        @JsonProperty("auth")
        private Auth auth;

        @JsonProperty("variable")
        private List<Variable> variable;

        // Helper
        @JsonIgnore
        public boolean isFolder() {
            return item != null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @JsonProperty("url")
        @JsonDeserialize(using = UrlDeserializer.class)  // url can be a String OR Url object
        private Url url;

        @JsonProperty("method")
        private HttpMethod method;

        @JsonProperty("header")
        private List<Header> header;

        @JsonProperty("body")
        private Body body;

        @JsonProperty("auth")
        private Auth auth;

        @JsonProperty("description")
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Url {
        @JsonProperty("raw")
        private String raw;

        @JsonProperty("protocol")
        private String protocol;

        @JsonProperty("host")
        private List<String> host;  // ["api", "example", "com"]

        @JsonProperty("path")
        private List<String> path;  // ["v1", "users", ":id"]

        @JsonProperty("port")
        private String port;

        @JsonProperty("query")
        private List<QueryParam> query;

        @JsonProperty("variable")
        private List<Variable> variable;  // path variables
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body {
        @JsonProperty("mode")
        private BodyMode mode;

        @JsonProperty("raw")
        private String raw;

        @JsonProperty("urlencoded")
        private List<FormParam> urlencoded;

        @JsonProperty("formdata")
        private List<FormParam> formdata;

        @JsonProperty("options")
        private BodyOptions options;

        @JsonProperty("graphql")
        private Map<String, Object> graphql;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BodyOptions {
        @JsonProperty("raw")
        private RawOptions raw;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RawOptions {
            @JsonProperty("language")
            private String language;  // "json", "text", "javascript", "xml", "html"
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private String value;

        @JsonProperty("disabled")
        private Boolean disabled;

        @JsonProperty("description")
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Variable {
        @JsonProperty("id")
        private String id;

        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private Object value;  // can be string, number, boolean

        @JsonProperty("type")
        private VariableType type;

        @JsonProperty("name")
        private String name;

        @JsonProperty("disabled")
        private Boolean disabled;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Auth {
        @JsonProperty("type")
        private AuthType type;

        @JsonProperty("apikey")
        private List<AuthAttribute> apikey;

        @JsonProperty("bearer")
        private List<AuthAttribute> bearer;

        @JsonProperty("basic")
        private List<AuthAttribute> basic;

        @JsonProperty("oauth2")
        private List<AuthAttribute> oauth2;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthAttribute {
        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private Object value;

        @JsonProperty("type")
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Event {
        @JsonProperty("listen")
        private EventType listen;

        @JsonProperty("script")
        private Script script;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Script {
        @JsonProperty("type")
        private ScriptLanguage type;

        @JsonProperty("exec")
        private List<String> exec;  // lines of script

        @JsonProperty("src")
        private String src;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("originalRequest")
        private Request originalRequest;

        @JsonProperty("status")
        private String status;  // e.g. "OK"

        @JsonProperty("code")
        private Integer code;  // e.g. 200

        @JsonProperty("header")
        private List<Header> header;

        @JsonProperty("body")
        private String body;

        @JsonProperty("cookie")
        private List<Object> cookie;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryParam {
        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private String value;

        @JsonProperty("disabled")
        private Boolean disabled;

        @JsonProperty("description")
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormParam {
        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private String value;

        @JsonProperty("disabled")
        private Boolean disabled;

        @JsonProperty("type")
        private FormFieldType type;

        @JsonProperty("src")
        private Object src; // can be string (file path) or object (file details)
    }
}
