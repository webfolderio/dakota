package io.webfolder.dakota;

import java.nio.ByteBuffer;

public interface Response {

    void body(long contextId, String content);

    void body(long contextId, byte[] content);

    void body(long contextId, ByteBuffer content);

    void appendHeader(long contextId, String name, String value);

    void closeConnection(long contextId);

    void keepAliveConnection(long contextId);

    void appendHeaderDateField(long contextId);

    void done(long contextId);
}
