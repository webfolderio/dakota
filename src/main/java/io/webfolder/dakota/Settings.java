package io.webfolder.dakota;

import static java.net.InetAddress.getByName;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Settings {

    private final InetAddress address;

    private final int port;

    public Settings() {
        try {
            address = getByName("localhost");
        } catch (UnknownHostException e) {
            throw new DakotaException(e);
        }
        port = 8080;
    }

    public Settings(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address.getHostAddress();
    }

    public int getPort() {
        return port;
    }
}
