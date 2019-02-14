package io.webfolder.dakota;

class ResponseImpl implements Response {

    private final long context;

    public ResponseImpl(long context) {
        this.context = context;
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
