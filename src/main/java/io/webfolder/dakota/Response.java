package io.webfolder.dakota;

public interface Response {

    void setBody(String content);

    void appendHeader(String name, String value);

    void done();
}
