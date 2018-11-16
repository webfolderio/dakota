package io.webfolder.dakota;

public enum HandlerStatus {
    accepted(1),
    rejected(0);

    public final int value;

    private HandlerStatus(int value) {
        this.value = value;
    }
}
