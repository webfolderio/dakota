package io.webfolder.dakota;

class ResponseImpl implements Response {

    private final long context;

    public ResponseImpl(long context) {
        this.context = context;
    }

    private native void _setBody(String content);

    private native void _done();

    private native void _appendHeader(String name, String value);

    private native void _closeConnection();

    private native void _keepAliveConnection();

    private native void _appendHeaderDateField();

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

    @Override
    public void closeConnection() {
        _closeConnection();
    }

    @Override
    public void keepAliveConnection() {
        _keepAliveConnection();
    }

    @Override
    public void appendHeaderDateField() {
        _appendHeaderDateField();
    }
}
