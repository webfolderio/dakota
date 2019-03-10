package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static io.webfolder.dakota.HttpStatus.NotFound;

public class NotFoundHandler implements Handler {


    @Override
    public HandlerStatus handle(long contextId, Request req, Response res) {
        req.createResponse(contextId, NotFound);
        res.done(contextId);
        return accepted;
    }
}
