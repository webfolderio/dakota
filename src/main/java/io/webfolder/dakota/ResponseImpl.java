package io.webfolder.dakota;

import static java.nio.ByteOrder.nativeOrder;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class ResponseImpl implements Response {

    private long contextId;

    private static final ByteOrder ORDER = nativeOrder();

    private native void _body(long contextId, String content, boolean compress);

    private native void _body(long contextId, ByteBuffer content);

    private native void _body(long contextId, byte[] content);

    private native void _sendfile(long contextId, String path);

    private native void _done(long contextId);

    private native void _appendHeader(long contextId, String name, String value);

    private native void _closeConnection(long contextId);

    private native void _keepAliveConnection(long contextId);

    private native void _appendHeaderDateField(long contextId);

    @Override
    public void body(long contextId, String content) {
        _body(contextId, content, false);
    }

    @Override
    public void body(long contextId, String content, boolean compress) {
        _body(contextId, content, compress);
    }

    @Override
    public void appendHeader(long contextId, String name, String value) {
        assert name != null;
        _appendHeader(contextId, name, value);
    }

    @Override
    public void closeConnection(long contextId) {
        _closeConnection(contextId);
    }

    @Override
    public void keepAliveConnection(long contextId) {
        _keepAliveConnection(contextId);
    }

    @Override
    public void appendHeaderDateField(long contextId) {
        _appendHeaderDateField(contextId);
    }

    @Override
    public void body(long contextId, ByteBuffer content) {
        assert content != null;
        if (!content.isDirect()) {
            throw new IllegalArgumentException();
        }
        if (!content.order().equals(ORDER)) {
            throw new IllegalArgumentException();
        }
        _body(contextId, content);
    }

    @Override
    public void body(long contextId, byte[] content) {
        assert content != null;
        _body(contextId, content);
    }


    @Override
    public void body(long contextId, File file) {
        _sendfile(contextId, file.getAbsolutePath());
    }

    public void done(long contextId) {
        _done(contextId);
    }
}
