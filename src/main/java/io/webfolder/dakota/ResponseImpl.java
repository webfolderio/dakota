package io.webfolder.dakota;

class ResponseImpl implements Response {

    private final long context;

    public ResponseImpl(long context) {
        this.context = context;
    }

    private native void _setBody(String content);

    private native void _done();

    private native void _appendHeader(String name, String value);

    @Override
    public void setBody(String content) {
        _setBody(content);
    }
    
    @Override
    public void done() {
        _done();
    }

    @Override
    public void appendHeader(String name, String value) {
        _appendHeader(name, value);
    }
}
