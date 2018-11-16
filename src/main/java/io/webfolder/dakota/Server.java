package io.webfolder.dakota;

import static java.lang.System.load;

import java.nio.file.Paths;

public class Server {

    static {
        load(Paths.get(".")
            .toAbsolutePath()
            .normalize()
            .resolve("native/build/Release/dakota.dll")
            .toString());
    }

    private Object[][] routes = new Object[0][];

    // ------------------------------------------------------------------------
    // native peers
    // ------------------------------------------------------------------------

    private long pool;

    // ------------------------------------------------------------------------
    // private native methods
    // ------------------------------------------------------------------------
    private native void _run(Object[][] routes);

    private native void _stop();

    public void run(Router router) {
        _run(router.getRoutes());
    }

    public void stop() {
        _stop();
    }
}
