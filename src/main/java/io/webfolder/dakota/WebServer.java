package io.webfolder.dakota;

import static io.webfolder.dakota.Logger.ERROR;
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

    private long server;

    private boolean started;

    private final Settings settings;

    private final Request request;

    private final Response response = new ResponseImpl();

    // ------------------------------------------------------------------------
    // private native methods
    // ------------------------------------------------------------------------
    private native void _run(Settings settings,
                                Object[][] routes,
                                Handler nonMatchedHandler,
                                Request request,
                                Response response);

    private native void _stop();

    public WebServer(Settings settings) {
        this.settings = settings;
        this.request = new RequestImpl(settings.getLogger());
    }

    public WebServer() {
        this(new Settings());
    }

    public void run(Router router) {
        _run(settings, router.getRoutes(), null, request, response);
    }

    public void run(Router router, Handler nonMatchedHandler) {
        _run(settings, router.getRoutes(), nonMatchedHandler, request, response);
    }

    public void stop() {
        _stop();
    }

    public boolean running() {
        return server > 0 && started ? true : false;
    }

    private Logger getLogger() {
        return settings.getLogger();
    }

    private ExceptionHandler getExceptionHandler() {
        return settings.getExceptionHandler();
    }

    private boolean reject(Throwable t) {
        if (t != null && (t instanceof ContextNotFoundException ||
                          t instanceof ResponseNotFoundException)) {
            getLogger().log(ERROR, t.getMessage());
            return true;
        }
        return true;
    }
}
