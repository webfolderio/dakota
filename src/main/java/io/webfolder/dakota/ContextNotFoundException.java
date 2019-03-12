package io.webfolder.dakota;

public class ContextNotFoundException extends DakotaException {

    private static final long serialVersionUID = -1770397411365957356L;

    public ContextNotFoundException(String contextId) {
        super(contextId);
    }
}
