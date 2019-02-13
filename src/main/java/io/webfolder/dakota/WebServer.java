package io.webfolder.dakota;

import static java.lang.System.getProperty;
import static java.lang.System.load;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Locale.ENGLISH;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class WebServer {

    private static final String OS = getProperty("os.name").toLowerCase(ENGLISH);

    private static final boolean WINDOWS = OS.startsWith("windows");

    static {
        ClassLoader cl = WebServer.class.getClassLoader();
        Path libFile;
        String library = WINDOWS ? "META-INF/dakota.dll" : "META-INF/dakota.so";
        try (InputStream is = cl.getResourceAsStream(library)) {
            libFile = createTempFile("dakota", WINDOWS ? ".dll" : ".so");
            copy(is, libFile, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        libFile.toFile().deleteOnExit();
        load(libFile.toAbsolutePath().toString());
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
