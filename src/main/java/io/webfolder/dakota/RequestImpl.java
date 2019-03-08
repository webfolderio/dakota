package io.webfolder.dakota;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static java.util.Collections.emptyMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

class RequestImpl implements Request {

    private static final ByteOrder ORDER = nativeOrder();

    private native void _createResponse(long id, int status, String reasonPhrase);

    private native Map<String, String> _query(long id);

    private native Map<String, String> _header(long id);

    private native String _target(long id);

    private native String _param(long id, String name);

    private native String _param(long id, int index);

    private native int _namedParamSize(long id);

    private native int _indexedParamSize(long id);

    private native String _body(long id);

    private native long _length(long id);

    private native void _content(long id, ByteBuffer buffer);

    private native long _connectionId(long id);

    @Override
    public void createResponse(long id, HttpStatus status) {
        _createResponse(id, status.value, status.reasonPhrase);
    }

    @Override
    public Map<String, String> query(long id) {
        Map<String, String> map = _query(id);
        if (map == null) {
            return emptyMap();
        }
        return map;
    }

    @Override
    public Map<String, String> header(long id) {
        Map<String, String> map = _header(id);
        if (map == null) {
            return emptyMap();
        }
        return map;
    }

    @Override
    public String target(long id) {
        return _target(id);
    }

    @Override
    public String param(long id, String name) {
        return _param(id, name);
    }

    @Override
    public String param(long id, int index) {
        return _param(id, index);
    }

    @Override
    public int namedParamSize(long id) {
        return _namedParamSize(id);
    }

    @Override
    public int indexedParamSize(long id) {
        return _indexedParamSize(id);
    }

    @Override
    public String body(long id) {
        return _body(id);
    }

    @Override
    public byte[] content(long id) {
        long length = length(id);
        if (length > MAX_VALUE) {
            throw new RuntimeException();
        }
        ByteBuffer buffer = allocateDirect((int) length)
                                .order(ORDER);
        _content(id, buffer);
        byte[] content = new byte[buffer.remaining()];
        buffer.get(content);
        return content;
    }

    @Override
    public long length(long id) {
        return _length(id);
    }

    @Override
    public long connectionId(long id) {
        return _connectionId(id);
    }
}
