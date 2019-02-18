package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;

public class TestUrlFragment {

    private WebServer server;

    private String fragment;

    @Before
    public void init() {
        server = new WebServer();

        Router router = new Router();

        router.get("/test", request -> {
            Response response = request.ok();
            fragment = request.fragment();
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
    @Ignore
    public void test() throws IOException {
        OkHttpClient client = new Builder()
                                    .writeTimeout(10, SECONDS)
                                    .readTimeout(10, SECONDS)
                                    .connectTimeout(10, SECONDS
                                ).build();

        Request req = new okhttp3.Request.Builder()
                                .url("http://localhost:8080/test#my-fragment")
                                .build();
        client.newCall(req).execute();

        assertEquals("my-fragment", fragment);
    }
}
