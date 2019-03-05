package io.webfolder.dakota;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

public class TestNotFoundHandler {

    private WebServer server;

    @Before
    public void init() {
        server = new WebServer();
        Router router = new Router();
        new Thread(() -> server.run(router, new NotFoundHandler())).start();
    }

    @Test
    public void test() throws IOException {
        OkHttpClient client = new Builder().writeTimeout(10, SECONDS).readTimeout(10, SECONDS)
                .connectTimeout(10, SECONDS).build();
        Request req = new okhttp3.Request.Builder().url("http://localhost:8080/invalid-url").build();
        Response response = client.newCall(req).execute();
        assertEquals(404, response.code());
    }
}
