package io.webfolder.dakota;

public class ResponseImpl implements Response {

    private final long response;

    private Request request;

    public ResponseImpl(long response, Request request) {
        this.response = response;
        this.request = request;
    }

    @Override
    public void setBody(String content) {
        _setBody(content);
    }

    private native void _setBody(String content);

    private native void _done();

    @Override
    public void done() {
        _done();
    }
}
