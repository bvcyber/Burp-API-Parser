package com.bureauveritas.modelparser.model;

import com.bureauveritas.modelparser.BurpApi;
import com.bureauveritas.modelparser.control.file.handler.AbstractModelFileHandler;
import com.bureauveritas.modelparser.control.file.loader.*;
import lombok.Getter;

import java.io.File;

public class OpenModelFile {

    private static OpenModelFile instance;
    @Getter
    private AbstractModelFileHandler<?> modelFileHandler;
    private final AbstractModelFileLoaderChain<?,?> modelFileLoaderChain;

    public OpenModelFile() {
        modelFileLoaderChain = new AWSServiceModelFileLoader();
        modelFileLoaderChain
            .setNext(new OpenAPIFileLoader())
            .setNext(new McpServersJsonFileLoader())
            .setNext(new PostmanCollectionFileLoader())
            .setNext(new ErrorFileLoader());
    }

    public OpenModelFile(File file) {
        this();
        loadModelFromFile(file);
    }

    public void loadModelFromFile(File file) {
        modelFileHandler = modelFileLoaderChain.getModelFileHandler(file);
        BurpApi.getInstance().logging().logToOutput(String.format("Loaded model file handler %s",
            modelFileHandler != null ? modelFileHandler.getClass().getSimpleName() : "null"));
    }
}
