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

public class TestMultipleServer {

    private WebServer server1;

    private WebServer server2;

    @Before
    public void init() {
        server1 = new WebServer();

        Router router1 = new Router();

        router1.get("/foo", request -> {
            Response response = request.ok();
            response.body("server1");
            response.done();
            return accepted;
        });

        new Thread(() -> server1.run(router1)).start();

        server2 = new WebServer(new Settings(2020));

        Router router2 = new Router();

        router2.get("/foo", request -> {
            Response response = request.ok();
            response.body("server2");
            response.done();
            return accepted;
        });

        new Thread(() -> server2.run(router2)).start();
    }

    @After
    public void destroy() {
        if (server1 != null) {
            server1.stop();
        }
        if (server2 != null) {
            server2.stop();
        }
    }

    @Test
    public void test() throws IOException {
        OkHttpClient client = new Builder().writeTimeout(10, SECONDS).readTimeout(10, SECONDS)
                .connectTimeout(10, SECONDS).build();
        Request req1 = new okhttp3.Request.Builder().url("http://localhost:8080/foo").build();
        okhttp3.Response resp1 = client.newCall(req1).execute();
        String body1 = resp1.body().string();
        assertEquals("server1", body1);

        Request req2 = new okhttp3.Request.Builder().url("http://localhost:2020/foo").build();
        okhttp3.Response resp2 = client.newCall(req2).execute();
        String body2 = resp2.body().string();
        assertEquals("server2", body2);
    }
}
