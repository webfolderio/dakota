package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static io.webfolder.dakota.HttpStatus.OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

public class TestHttpParam {

    private WebServer server;

    private String bar;

    private int nps;

    private int ips;

    @Before
    public void init() {
        server = new WebServer();

        Request request = server.getRequest();
        Response response = server.getResponse();

        Router router = new Router();

        router.get("/foo/:bar(\\d{2})", id -> {
            request.createResponse(id, OK);
            String bar = request.param(id, "bar");
            this.bar = bar;
            this.nps = request.namedParamSize(id);
            this.ips = request.indexedParamSize(id);
            response.body(id, "hello, world!");
            response.done(id);
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
        okhttp3.Request req = new okhttp3.Request.Builder().url("http://localhost:8080/foo/20").build();
        String body = client.newCall(req).execute().body().string();
        assertEquals("hello, world!", body);
        assertEquals("20", bar);
        assertEquals(1, nps);
        assertEquals(0, ips);
    }
}
