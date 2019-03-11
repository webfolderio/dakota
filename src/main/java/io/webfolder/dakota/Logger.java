package io.webfolder.dakota;

public interface Logger {

    public static final int TRACE = 0;

    public static final int INFO  = 1;

    public static final int WARN  = 2;

    public static final int ERROR = 3;

    void log(int level, String message);

    boolean ennabled(int level);
}
