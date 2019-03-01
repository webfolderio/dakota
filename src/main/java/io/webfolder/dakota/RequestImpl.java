package io.webfolder.dakota;

import static java.util.Collections.emptyMap;
import static io.webfolder.dakota.HttpStatus.OK;

import java.util.Map;

class RequestImpl implements Request {

    private final long context;

    public RequestImpl(long context) {
        this.context = context;
    }

    @Override
    public Response createResponse(HttpStatus status) {
        _createResponse(status.value, status.reasonPhrase);
        ResponseImpl response = new ResponseImpl(context);
        return response;
    }

    private native void _createResponse(int status, String reasonPhrase);

    private native Map<String, Object> _query();

    private native Map<String, Object> _header();

    private native String _target();

    private native String _param(String name);

    private native String _param(int index);

    private native int _namedParamSize();

    private native int _indexedParamSize();

    private native String _body();

    @Override
    public Response ok() {
        return createResponse(OK);
    }

    @Override
    public Map<String, Object> query() {
        Map<String, Object> map = _query();
        if (map == null) {
            return emptyMap();
        }
        return map;
    }

    @Override
    public Map<String, Object> header() {
        Map<String, Object> map = _header();
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
}
