package com.bureauveritas.modelparser.control.file.loader;

import com.bureauveritas.modelparser.control.file.handler.a2a.A2AServerFileHandler;
import io.a2a.spec.AgentCard;

import java.io.File;

public class A2AServerFileLoader extends AbstractModelFileLoaderChain<AgentCard, A2AServerFileHandler> {
    public A2AServerFileLoader() {
        super(A2AServerFileHandler::new);
    }

    @Override
    public AgentCard loadModel(File file) throws Exception {
        return null;
    }
}
