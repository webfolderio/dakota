package io.webfolder.dakota;

import java.nio.ByteBuffer;

public interface Response {

    void body(String content);

    void body(byte[] content);

    void body(ByteBuffer content);

    void appendHeader(String name, String value);

    void closeConnection();

    void keepAliveConnection();

    void appendHeaderDateField();

    void done();
}
