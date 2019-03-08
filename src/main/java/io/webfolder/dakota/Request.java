package io.webfolder.dakota;

import java.util.Map;

public interface Request {

    void createResponse(long id, HttpStatus status);

    String body(long id);
    
    byte[] content(long id);

    long length(long id);

    Map<String, String> query(long id);

    Map<String, String> header(long id);

    String param(long id, String name);

    String param(long id, int index);

    int namedParamSize(long id);

    int indexedParamSize(long id);

    String target(long id);

    long connectionId(long id);
}
