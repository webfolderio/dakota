package io.webfolder.dakota;

public interface Response {

    void body(String content);

    void body(byte[] content);

    void appendHeader(String name, String value);

    void closeConnection();

    void keepAliveConnection();

    void appendHeaderDateField();

    void done();
}
