package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;

public class TestHttpSendFile {

    private WebServer server;

    @Before
    public void init() {
        server = new WebServer();

        Router router = new Router();

        router.get("/foo", request -> {
            Response response = request.ok();
            Path file = null;   
            try {
                file = Files.createTempFile("dakota", ".txt");
                Files.write(file, "Привет, мир!".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            response.body(file.toFile());
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
        Request req = new okhttp3.Request.Builder().url("http://localhost:8080/foo").build();
        String body = new String(client.newCall(req).execute().body().bytes(), StandardCharsets.UTF_8);
        assertEquals("Привет, мир!", body);
    }
}
