package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class TestHttpGet {

    private Server server;

    @Before
    public void init() {
        server = new Server();

        Router router = new Router();

        router.get("/foo", context -> {
            Response response = context.ok();
            response.setBody("hello, world!");
            response.done();
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
        OkHttpClient client = new OkHttpClient();
        Request req = new okhttp3.Request.Builder()
                                .url("http://localhost:8080/foo")
                                .build();
        String body = client.newCall(req).execute().body().string();
        assertEquals("hello, world!", body);
    }
}
