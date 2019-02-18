package io.webfolder.dakota;

import static java.lang.System.load;

public class WebServer {

    static {
        load("C:\\Users\\ozhan\\Desktop\\dakota\\native\\build\\Debug\\dakota.dll");
    }

    private Object[][] routes = new Object[0][];

    // ------------------------------------------------------------------------
    // native peer
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
