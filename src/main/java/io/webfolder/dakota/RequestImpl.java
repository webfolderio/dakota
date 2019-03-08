package io.webfolder.dakota;

import static java.lang.Integer.MAX_VALUE;
import static java.nio.ByteBuffer.allocateDirect;
import static java.util.Collections.emptyMap;

import static java.nio.ByteOrder.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

class RequestImpl implements Request {

    private static final ByteOrder ORDER = nativeOrder();

    private native void _createResponse(long contextId, int status, String reasonPhrase);

    private native Map<String, String> _query(long contextId);

    private native Map<String, String> _header(long contextId);

    private native String _target(long contextId);

    private native String _param(long contextId, String name);

    private native String _param(long contextId, int index);

    private native int _namedParamSize(long contextId);

    private native int _indexedParamSize(long contextId);

    private native String _body(long contextId);

    private native long _length(long contextId);

    private native byte[] _bodyAsByteArray(long contextId);

    private native void _bodyAsByteBuffer(long context, ByteBuffer buffer);

    private native long _connectionId(long contextId);

    @Override
    public void createResponse(long contextId, HttpStatus status) {
        _createResponse(contextId, status.value, status.reasonPhrase);
    }

    @Override
    public Map<String, String> query(long contextId) {
        Map<String, String> map = _query(contextId);
        if (map == null) {
            return emptyMap();
        }
        return map;
    }

    @Override
    public Map<String, String> header(long contextId) {
        Map<String, String> map = _header(contextId);
        if (map == null) {
            return emptyMap();
        }
        return map;
    }

    @Override
    public String target(long contextId) {
        return _target(contextId);
    }

    @Override
    public String param(long contextId, String name) {
        return _param(contextId, name);
    }

    @Override
    public String param(long contextId, int index) {
        return _param(contextId, index);
    }

    @Override
    public int namedParamSize(long contextId) {
        return _namedParamSize(contextId);
    }

    @Override
    public int indexedParamSize(long contextId) {
        return _indexedParamSize(contextId);
    }

    @Override
    public String body(long contextId) {
        return _body(contextId);
    }

    @Override
    public byte[] bodyAsByteArray(long contextId) {
        byte[] content = _bodyAsByteArray(contextId);
        return content;
    }

    @Override
    public ByteBuffer bodyAsByteBuffer(long contextId) {
        long length = length(contextId);
        if (length > MAX_VALUE) {
            throw new DakotaException("Request body is too big to fit byte array. Request size: " + length);
        }
        ByteBuffer buffer = allocateDirect((int) length)
                                .order(ORDER);
        _bodyAsByteBuffer(contextId, buffer);
        return buffer;
    }

    @Override
    public long length(long contextId) {
        return _length(contextId);
    }

    @Override
    public long connectionId(long contextId) {
        return _connectionId(contextId);
    }
}
