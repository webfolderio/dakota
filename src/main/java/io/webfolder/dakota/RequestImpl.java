package io.webfolder.dakota;

import static io.webfolder.dakota.HttpStatus.OK;
import static java.lang.Integer.MAX_VALUE;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static java.util.Collections.emptyMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

class RequestImpl implements Request {

    private static final ByteOrder ORDER = nativeOrder();

    private final long context;

    private final long id;

    RequestImpl(long context, long id) {
        this.context = context;
        this.id = id;
    }

    @Override
    public Response createResponse(HttpStatus status) {
        _createResponse(status.value, status.reasonPhrase);
        ResponseImpl response = new ResponseImpl(context);
        return response;
    }

    private native void _createResponse(int status, String reasonPhrase);

    private native Map<String, String> _query();

    private native Map<String, String> _header();

    private native String _target();

    private native String _param(String name);

    private native String _param(int index);

    private native int _namedParamSize();

    private native int _indexedParamSize();

    private native String _body();

    private native long _length();

    private native void _content(ByteBuffer buffer);

    private native String _toString();

    @Override
    public Response ok() {
        return createResponse(OK);
    }

    @Override
    public Map<String, String> query() {
        Map<String, String> map = _query();
        if (map == null) {
            return emptyMap();
        }
        return map;
    }

    @Override
    public Map<String, String> header() {
        Map<String, String> map = _header();
        if (map == null) {
            return emptyMap();
        }
        return map;
    }

    @Override
    public String target() {
        return _target();
    }

    @Override
    public String param(String name) {
        return _param(name);
    }

    @Override
    public String param(int index) {
        return _param(index);
    }

    @Override
    public int namedParamSize() {
        return _namedParamSize();
    }

    @Override
    public int indexedParamSize() {
        return _indexedParamSize();
    }

    @Override
    public String body() {
        return _body();
    }

    @Override
    public byte[] content() {
        long length = length();
        if (length > MAX_VALUE) {
            throw new RuntimeException();
        }
        ByteBuffer buffer = allocateDirect((int) length)
                                .order(ORDER);
        _content(buffer);
        byte[] content = new byte[buffer.remaining()];
        buffer.get(content);
        return content;
    }

    @Override
    public long length() {
        return _length();
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public String toString() {
        return _toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RequestImpl other = (RequestImpl) obj;
        if (id != other.id)
            return false;
        return true;
    }
}
