package com.bureauveritas.modelparser.control.file.handler.postman;

import com.bureauveritas.modelparser.control.file.handler.openapi.OpenAPIFileHandler;
import io.swagger.v3.oas.models.OpenAPI;

public class PostmanCollectionFileHandler extends OpenAPIFileHandler {
    public PostmanCollectionFileHandler(OpenAPI modelObject) {
        super(modelObject);
    }

    @Override
    public String getModelType() {
        return "Postman Collection";
    }
}
