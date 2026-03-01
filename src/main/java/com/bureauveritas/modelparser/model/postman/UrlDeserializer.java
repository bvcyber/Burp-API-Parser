package com.bureauveritas.modelparser.model.postman;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Custom deserializer for URL field which can be either a String or a Url object.
 */
public class UrlDeserializer extends JsonDeserializer<PostmanCollectionModel.Url> {

    @Override
    public PostmanCollectionModel.Url deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();

        if (p.currentToken().isStructStart()) {
            // It's a JSON object - deserialize as Url object
            return mapper.readValue(p, PostmanCollectionModel.Url.class);
        } else {
            // It's a string - create a Url object with raw URL
            PostmanCollectionModel.Url url = new PostmanCollectionModel.Url();
            url.setRaw(p.getText());
            return url;
        }
    }
}


