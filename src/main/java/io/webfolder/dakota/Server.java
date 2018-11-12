package io.webfolder.dakota;

import static java.lang.System.arraycopy;
import static java.lang.System.load;

import java.nio.file.Paths;

public class Server {

    static {
        load(Paths.get(".")
            .toAbsolutePath()
            .normalize()
            .resolve("build-debug/Debug/dakota.dll")
            .toString());
    }

    private Object[][] routes = new Object[0][];

    // ------------------------------------------------------------------------
    // native peers
    // ------------------------------------------------------------------------

    private long pool;

    private long reflectionUtil;

    // ------------------------------------------------------------------------
    // private native methods
    // ------------------------------------------------------------------------
    private native void _run(Object[][] routes);

    private native void _stop();

    // ------------------------------------------------------------------------
    // public methods
    // ------------------------------------------------------------------------

    public Server get(String path, RouteHandler handler) {
        addHandler("get", path, handler);
        return this;
    }

    public Server post(String path, RouteHandler handler) {
        addHandler("post", path, handler);
        return this;
    }

    public Server delete(String path, RouteHandler handler) {
        addHandler("delete", path, handler);
        return this;
    }

    public Server head(String path, RouteHandler handler) {
        addHandler("head", path, handler);
        return this;
    }

    private void addHandler(String method, String path, RouteHandler handler) {
        Object[][] temp = new Object[routes.length + 1][];
        arraycopy(routes, 0, temp, 0, routes.length);
        temp[routes.length] = new Object[] {
            method, path, handler
        };
        routes = temp;
    }

    public void run() {
        _run(routes);
    }

    public void stop() {
        _stop();
    }
}
