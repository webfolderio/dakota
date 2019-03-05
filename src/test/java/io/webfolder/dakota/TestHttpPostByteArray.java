package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;

public class TestHttpPostByteArray {

    private WebServer server;

    @Before
    public void init() {
        server = new WebServer();

        Router router = new Router();

        router.post("/foo", request -> {
            String body = request.body();
            Response response = request.ok();
            response.body(body.getBytes());
            response.done();
            return accepted;
        });

        new Thread(() -> server.run(router)).start();
    }

    @Test
    public void test() throws IOException {
        OkHttpClient client = new Builder().writeTimeout(10, SECONDS).readTimeout(10, SECONDS)
                .connectTimeout(10, SECONDS).build();
        Request req = new okhttp3.Request.Builder()
                                .url("http://localhost:8080/foo")
                                .post(RequestBody.create(MediaType.parse("application/octet-stream"), "hello, world!".getBytes()))
                            .build();
        String body = client.newCall(req).execute().body().string();
        assertEquals("hello, world!", body);
    }
}
