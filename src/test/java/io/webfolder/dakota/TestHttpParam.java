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

public class TestHttpParam {

    private WebServer server;

    private String bar;

    private int nps;

    private int ips;

    @Before
    public void init() {
        server = new WebServer();

        Router router = new Router();

        router.get("/foo/:bar(\\d{2})", request -> {
            String bar = request.param("bar");
            this.bar = bar;
            this.nps = request.namedParamSize();
            this.ips = request.indexedParamSize();
            Response response = request.ok();
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
        OkHttpClient client = new Builder().writeTimeout(10, SECONDS).readTimeout(10, SECONDS)
                .connectTimeout(10, SECONDS).build();
        Request req = new okhttp3.Request.Builder().url("http://localhost:8080/foo/20").build();
        String body = client.newCall(req).execute().body().string();
        assertEquals("hello, world!", body);
        assertEquals("20", bar);
        assertEquals(1, nps);
        assertEquals(0, ips);
    }
}
