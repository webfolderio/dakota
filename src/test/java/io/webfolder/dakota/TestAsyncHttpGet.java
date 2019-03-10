package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static io.webfolder.dakota.HttpStatus.OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

public class TestAsyncHttpGet {

    private WebServer server;

    private okhttp3.Request req;

    private int freePort;

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

        router.get("/foo", (contextId, request, response) -> {
            new Thread(() -> {
                request.createResponse(contextId, OK);
                response.body(contextId, "hello, world!");
                response.done(contextId);
            }).start();
            return accepted;
        });

        new Thread(() -> server.run(router)).start();
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
        req = new okhttp3.Request.Builder().url("http://localhost:" + freePort + "/foo").build();
        String body = client.newCall(req).execute().body().string();
        assertEquals("hello, world!", body);
    }
}
