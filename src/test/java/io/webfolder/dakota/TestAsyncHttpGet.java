package io.webfolder.dakota;

import static io.webfolder.dakota.HttpStatus.*;
import static io.webfolder.dakota.HandlerStatus.accepted;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

public class TestAsyncHttpGet {

    private WebServer server;

    private okhttp3.Request req;

    @Before
    public void init() {
        server = new WebServer();

        Request request = server.getRequest();
        Response response = server.getResponse();

        Router router = new Router();

        router.get("/foo", id -> {
            new Thread(() -> {
                request.createResponse(id, OK);
                response.body(id, "hello, world!");
                response.done(id);
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
        OkHttpClient client = new Builder().writeTimeout(10, SECONDS).readTimeout(10, SECONDS)
                .connectTimeout(10, SECONDS).build();
        req = new okhttp3.Request.Builder().url("http://localhost:8080/foo").build();
        String body = client.newCall(req).execute().body().string();
        assertEquals("hello, world!", body);
    }
}
