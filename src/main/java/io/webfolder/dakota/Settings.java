package io.webfolder.dakota;

public class Settings {

    private final String address;

    private final int port;

    private Logger logger = new ConsoleLogger();

    public Settings() {
        this(8080);
    }

    public Settings(int port) {
        this("localhost", port);
    }

    public Settings(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
