package com.bureauveritas.modelparser.control.file.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlParsingUtils {
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://)?([^/]+)([^?]*)(?:\\?(.*))?$");

    private UrlParsingUtils() {
    }

    public static ProtocolDomainPathQuery parseProtocolDomainPathQuery(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher matcher = URL_PATTERN.matcher(url);
        return matcher.find() ?
            new ProtocolDomainPathQuery(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)) : null;
    }

    public record ProtocolDomainPathQuery(String protocol, String domain, String path, String query) {
        public String getBaseUri() {
            return protocol + domain + (path == null ? "" : path);
        }

        public String getEndpoint() {
            String basePath = path == null ? "" : path;
            return basePath + (query != null ? "?" + query : "");
        }
    }
}

