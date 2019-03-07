package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;

public class TestHttpGet {

    private WebServer server;

    private long id;

    private String str;

    @Before
    public void init() {
        server = new WebServer();

        Router router = new Router();

        router.get("/foo", request -> {
            Response response = request.ok();
            response.body("hello, world!");
            response.done();
            id = request.id();
            str = request.toString();
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
        Request req = new okhttp3.Request.Builder().url("http://localhost:8080/foo").build();
        String body = client.newCall(req).execute().body().string();
        assertEquals("hello, world!", body);
        assertEquals(1, id);
        assertEquals("{req_id: 0, conn_id: 1, path: /foo, query: }", str);
    }
}
