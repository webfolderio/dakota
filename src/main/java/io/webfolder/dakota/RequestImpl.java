package io.webfolder.dakota;

import static io.webfolder.dakota.HttpStatus.OK;

public class RequestImpl implements Request {

    private final long request;

    public RequestImpl(long request) {
        this.request = request;
    }

    @Override
    public Response createResponse(HttpStatus status) {
        long peer = _createResponse(status.value, status.reasonPhrase);
        ResponseImpl response = new ResponseImpl(peer, this);
        return response;
    }

    private native long _createResponse(int status, String reasonPhrase);

    @Override
    public Response ok() {
        return createResponse(OK);
    }
}
