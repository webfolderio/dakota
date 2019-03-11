package io.webfolder.dakota;

import static java.lang.String.format;

public class ConsoleLogger implements Logger {

    @Override
    public void trace(String msg) {
        System.out.println(format("[%s] %s", "TRACE", msg));
    }

    @Override
    public void info(String msg) {
        System.out.println(format("[%s] %s", "INFO", msg));
    }

    @Override
    public void error(String msg) {
        System.out.println(format("[%s] %s", "ERROR", msg));
    }

    @Override
    public void warn(String msg) {
        System.out.println(format("[%s] %s", "WARN", msg));
    }
}
