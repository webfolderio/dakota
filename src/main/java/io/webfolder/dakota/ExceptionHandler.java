package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static io.webfolder.dakota.HttpStatus.InternalServerError;

public interface ExceptionHandler {

    public default HandlerStatus handle(long contextId, Request req, Response res, Throwable t) {
        req.createResponse(contextId, InternalServerError);
        res.body(contextId, t.getMessage());
        res.done(contextId);
        return accepted;
    }
}
