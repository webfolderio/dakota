package io.webfolder.dakota;

public interface Request {
    
    Response createResponse(HttpStatus status);

    Response ok();
}
