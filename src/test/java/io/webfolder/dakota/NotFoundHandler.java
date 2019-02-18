package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static io.webfolder.dakota.HttpStatus.NotFound;

public class NotFoundHandler implements Handler {

    @Override
    public HandlerStatus handle(Request request) {
        Response response = request.createResponse(NotFound);
        response.done();
        return accepted;
    }
}
