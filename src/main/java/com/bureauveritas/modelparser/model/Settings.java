package com.bureauveritas.modelparser.model;

import lombok.Getter;
import lombok.Setter;

public class Settings {
    public static int DEFAULT_MCP_CONNECTION_TIMEOUT = 5;
    public static int DEFAULT_GRPC_PORT_NUMBER = 50055;
    public static boolean DEFAULT_INVALID_OPENAPI_HOST_ALLOWED = false;

    @Getter @Setter
    private static boolean invalidOpenAPIHostAllowed = DEFAULT_INVALID_OPENAPI_HOST_ALLOWED;
    @Getter @Setter
    private static int grpcPortNumber = DEFAULT_GRPC_PORT_NUMBER;
    @Getter @Setter
    private static int mcpConnectionTimeoutSeconds = DEFAULT_MCP_CONNECTION_TIMEOUT;
}
