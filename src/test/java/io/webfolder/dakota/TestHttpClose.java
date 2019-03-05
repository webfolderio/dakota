package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;

public class TestHttpClose {

    private WebServer server;

    @Before
    public void init() {
        server = new WebServer();

        Router router = new Router();

        router.get("/foo", request -> {
            Response response = request.ok();
            response.body("hello, world!");
            response.closeConnection();
            response.done();
            return accepted;
        });

        new Thread(() -> server.run(router)).start();
    }

    @Test
    public void test() throws IOException {
        OkHttpClient client = new Builder().writeTimeout(10, SECONDS).readTimeout(10, SECONDS)
                .connectTimeout(10, SECONDS).build();
        Request req = new okhttp3.Request.Builder().url("http://localhost:8080/foo").build();
        okhttp3.Response resp = client.newCall(req).execute();
        String body = resp.body().string();
        assertEquals("hello, world!", body);
        assertEquals("close", resp.header("Connection"));
    }
}
