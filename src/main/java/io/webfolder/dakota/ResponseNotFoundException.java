package io.webfolder.dakota;

class ResponseNotFoundException extends DakotaException {

    private static final long serialVersionUID = 5777350226053659306L;

    ResponseNotFoundException(String contextId) {
        super("Response [" + contextId + "] not found, Make sure that Request.createResponse() is called");
    }
}
