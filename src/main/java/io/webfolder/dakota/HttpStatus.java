package io.webfolder.dakota;

public enum HttpStatus {
    OK(200, "OK");

    public final int value;

    public final String reasonPhrase;

    private HttpStatus(int value, String reasonPhrase) {
        this.value = value;
        this.reasonPhrase = reasonPhrase;
    }
}
