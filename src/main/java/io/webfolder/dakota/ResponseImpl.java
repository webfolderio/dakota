package io.webfolder.dakota;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class ResponseImpl implements Response {

    private long context;

    private static final ByteOrder ORDER = nativeOrder();

    private native void _body(long id, String content);

    private native void _body(long id, ByteBuffer content);

    private native void _done(long id);

    private native void _appendHeader(long id, String name, String value);

    private native void _closeConnection(long id);

    private native void _keepAliveConnection(long id);

    private native void _appendHeaderDateField(long id);

    @Override
    public void body(long id, String content) {
        _body(id, content);
    }

    @Override
    public void appendHeader(long id, String name, String value) {
        assert name != null;
        _appendHeader(id, name, value);
    }

    @Override
    public void closeConnection(long id) {
        _closeConnection(id);
    }

    @Override
    public void keepAliveConnection(long id) {
        _keepAliveConnection(id);
    }

    @Override
    public void appendHeaderDateField(long id) {
        _appendHeaderDateField(id);
    }

    @Override
    public void body(long id, ByteBuffer content) {
        assert content != null;
        if (!content.isDirect()) {
            throw new IllegalArgumentException();
        }
        if (!content.order().equals(ORDER)) {
            throw new IllegalArgumentException();
        }
        _body(id, content);
    }

    @Override
    public void body(long id, byte[] content) {
        assert content != null;
        ByteBuffer buffer = allocateDirect(content.length)
                                .order(ORDER)
                            .put(content);
        _body(id, buffer);
    }

    public void done(long id) {
        _done(id);
    }
}
