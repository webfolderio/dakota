package io.webfolder.dakota;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static io.webfolder.dakota.HttpStatus.NotFound;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

public class TestNotFoundHandler {

    private WebServer server;

    private int freePort;

    private okhttp3.Request req;

    private okhttp3.Response response;

    public static class NotFoundHandler implements Handler {


        @Override
        public HandlerStatus handle(long contextId, Request req, Response res) {
            req.createResponse(contextId, NotFound);
            res.done(contextId);
            return accepted;
        }
    }

    @Before
    public void init() {
        try (ServerSocket socket = new ServerSocket(0)) {
            this.freePort = socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Settings settings = new Settings(freePort);

        server = new WebServer(settings);

        Router router = new Router();
        new Thread(() -> server.run(router, new NotFoundHandler())).start();
    }

    @After
    public void destroy() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void test() throws IOException {
        while (!server.running()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        OkHttpClient client = new Builder().writeTimeout(10, SECONDS).readTimeout(10, SECONDS)
                .connectTimeout(10, SECONDS).retryOnConnectionFailure(true).build();
        req = new okhttp3.Request.Builder().url("http://localhost:" + freePort + "/invalid-url").build();
        response = client.newCall(req).execute();
        assertEquals(404, response.code());
    }
}
