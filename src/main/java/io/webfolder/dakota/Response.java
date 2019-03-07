package io.webfolder.dakota;

import java.nio.ByteBuffer;

public interface Response {

    void body(long id, String content);

    void body(long id, byte[] content);

    void body(long id, ByteBuffer content);

    void appendHeader(long id, String name, String value);

    void closeConnection(long id);

    void keepAliveConnection(long id);

    void appendHeaderDateField(long id);

    void done(long id);
}
