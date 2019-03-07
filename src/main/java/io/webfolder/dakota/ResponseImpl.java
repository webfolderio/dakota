package io.webfolder.dakota;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class ResponseImpl implements Response {

    private final long context;

    private static final ByteOrder ORDER = nativeOrder();

    public ResponseImpl(long context) {
        this.context = context;
    }

    private native void _body(String content);

    private native void _body(ByteBuffer content);

    private native void _done();

    private native void _appendHeader(String name, String value);

    private native void _closeConnection();

    private native void _keepAliveConnection();

    private native void _appendHeaderDateField();

    @Override
    public void body(String content) {
        _body(content);
    }
    
    @Override
    public void done() {
        _done();
    }

    @Override
    public void appendHeader(String name, String value) {
        assert name != null;
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

    @Override
    public void body(ByteBuffer content) {
        assert content != null;
        if (!content.isDirect()) {
            throw new IllegalArgumentException();
        }
        if (!content.order().equals(ORDER)) {
            throw new IllegalArgumentException();
        }
        _body(content);
    }

    @Override
    public void body(byte[] content) {
        assert content != null;
        ByteBuffer buffer = allocateDirect(content.length)
                                .order(ORDER)
                            .put(content);
        _body(buffer);
    }
}
