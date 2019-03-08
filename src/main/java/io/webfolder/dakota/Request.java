package io.webfolder.dakota;

import java.util.Map;

public interface Request {

    void createResponse(long contextId, HttpStatus status);

    String body(long contextId);
    
    byte[] content(long contextId);

    long length(long contextId);

    Map<String, String> query(long contextId);

    Map<String, String> header(long contextId);

    String param(long contextId, String name);

    String param(long contextId, int index);

    int namedParamSize(long contextId);

    int indexedParamSize(long contextId);

    String target(long contextId);

    long connectionId(long contextId);
}
