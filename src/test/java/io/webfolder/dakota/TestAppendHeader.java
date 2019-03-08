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

public class TestAppendHeader {

    private WebServer server;

    @Before
    public void init() {
        server = new WebServer();

        Request request = server.getRequest();
        Response response = server.getResponse();

        Router router = new Router();

        router.get("/foo", contextId -> {
            request.createResponse(contextId, OK);
            response.body(contextId, "hello, world!");
            response.appendHeader(contextId, "foo", "bar");
            response.done(contextId);
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
        okhttp3.Request req = new okhttp3.Request.Builder().url("http://localhost:8080/foo").build();
        okhttp3.Response resp = client.newCall(req).execute();
        String body = resp.body().string();
        assertEquals("hello, world!", body);
        assertEquals("bar", resp.header("foo"));
    }
}
