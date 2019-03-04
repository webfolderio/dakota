package io.webfolder.dakota;

import java.net.UnknownHostException;

public class DakotaException extends RuntimeException {

    private static final long serialVersionUID = 8161996050295603720L;

    public DakotaException(String message) {
        super(message);
    }

    public DakotaException(UnknownHostException e) {
        super(e);
    }
}
