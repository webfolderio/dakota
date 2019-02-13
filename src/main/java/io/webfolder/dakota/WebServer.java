package io.webfolder.dakota;

public class WebServer {

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
