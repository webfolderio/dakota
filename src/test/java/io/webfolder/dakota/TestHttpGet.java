package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class TestHttpGet {

    private WebServer server;

    @Before
    public void init() {
        server = new WebServer();

        Router router = new Router();

        router.get("/foo", context -> {
            Response response = context.ok();
            response.setBody("hello, aworld!");
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
        OkHttpClient client = new OkHttpClient.Builder().writeTimeout(1, TimeUnit.DAYS).readTimeout(1, TimeUnit.DAYS).connectTimeout(1, TimeUnit.DAYS).build();
        Request req = new okhttp3.Request.Builder()
                                .url("http://localhost:8080/foo")
                                .build();
        String body = client.newCall(req).execute().body().string();
        assertEquals("hello, aworld!", body);
    }
}
