package io.webfolder.dakota;

import java.util.Map;

public interface Request {
    
    Response createResponse(HttpStatus status);

    Response ok();

    Map<String, Object> query();

    Map<String, Object> header();

    String fragment();

    String target();
}
