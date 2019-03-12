package io.webfolder.dakota;

import static java.lang.String.format;
import static java.lang.System.out;

public class ConsoleLogger implements Logger {

    @Override
    public void log(int level, String message) {
        out.println(format("[%s] %s", toString(level), message));
    }

    @Override
    public boolean ennabled(int level) {
        switch (level) {
            case TRACE:
                return true;
            case INFO:
                return true;
            case WARN:
                return true;
            case ERROR:
                return true;
            default:
                return true;
        }
    }

    private String toString(int level) {
        switch (level) {
            case TRACE:
                return "TRACE";
            case INFO:
                return "INFO";
            case WARN:
                return "WARN";
            case ERROR:
                return "ERROR";
            default:
                return "";
        }
    }
}
