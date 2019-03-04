package io.webfolder.dakota;

import static java.io.File.pathSeparatorChar;
import static java.lang.System.load;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class WebServer {

    static {
        Path libFile;
        ClassLoader cl = WebServer.class.getClassLoader();
        boolean windows = ';' == pathSeparatorChar;
        String library = windows ? "META-INF/dakota.dll" : "META-INF/libdakota.so";
        try (InputStream is = cl.getResourceAsStream(library)) {
            libFile = createTempFile("dakota", windows ? ".dll" : ".so");
            copy(is, libFile, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        libFile.toFile().deleteOnExit();
        load(libFile.toAbsolutePath().toString());
    }

    private Object[][] routes = new Object[0][];

    // ------------------------------------------------------------------------
    // native peer
    // ------------------------------------------------------------------------

    private long pool;

    // ------------------------------------------------------------------------
    // private native methods
    // ------------------------------------------------------------------------
    private native void _run(Settings settings, Object[][] routes, Handler nonMatchedHandler);

    private native void _stop();

    public void run(Router router) {
        _run(new Settings(), router.getRoutes(), null);
    }

    public void run(Settings settings, Router router) {
        _run(settings, router.getRoutes(), null);
    }

    public void run(Router router, Handler nonMatchedHandler) {
        _run(new Settings(), router.getRoutes(), nonMatchedHandler);
    }

    public void run(Settings settings, Router router, Handler nonMatchedHandler) {
        _run(settings, router.getRoutes(), nonMatchedHandler);
    }

    public void stop() {
        _stop();
    }
}
