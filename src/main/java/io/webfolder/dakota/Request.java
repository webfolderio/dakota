package io.webfolder.dakota;

import java.util.Map;

public interface Request {

    Response createResponse(HttpStatus status);

    String body();
    
    byte[] content();

    long length();

    Response ok();

    Map<String, Object> query();

    Map<String, Object> header();

    String param(String name);

    String param(int index);

    int namedParamSize();

    int indexedParamSize();

    String target();
}
