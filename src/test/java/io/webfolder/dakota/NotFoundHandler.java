package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static io.webfolder.dakota.HttpStatus.NotFound;

public class NotFoundHandler implements Handler {

    private final WebServer server;

    public NotFoundHandler(WebServer server) {
        this.server = server;
    }

    @Override
    public HandlerStatus handle(long id) {
        server.getRequest().createResponse(id, NotFound);
        server.getResponse().done(id);
        return accepted;
    }
}
