package io.webfolder.dakota;

class ContextNotFoundException extends DakotaException {

    private static final long serialVersionUID = -1770397411365957356L;

    ContextNotFoundException(String contextId) {
        super("Context [" + contextId + "] not found");
    }
}
