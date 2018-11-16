package io.webfolder.dakota;

import static io.webfolder.dakota.HttpStatus.OK;

public class RequestImpl implements Request {

    private final long context;
    
    public RequestImpl(long context) {
        this.context = context;
    }

    @Override
    public Response createResponse(HttpStatus status) {
        _createResponse(status.value, status.reasonPhrase);
        ResponseImpl response = new ResponseImpl(context);
        return response;
    }

    private native void _createResponse(int status, String reasonPhrase);

    @Override
    public Response ok() {
        return createResponse(OK);
    }
}
