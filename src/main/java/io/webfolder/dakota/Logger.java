package io.webfolder.dakota;

public interface Logger {

    void trace(String msg);

    void info(String msg);

    void error(String msg);

    void warn(String msg);
}
