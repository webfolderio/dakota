package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static io.webfolder.dakota.HttpStatus.OK;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.RequestBody;

public class TestHttpPostByteBuffer {

    private WebServer server;

    private int freePort;

    @Before
    public void init() {
        try (ServerSocket socket = new ServerSocket(0)) {
            this.freePort = socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Settings settings = new Settings(freePort);

        server = new WebServer(settings);

        Request request = server.getRequest();
        Response response = server.getResponse();

        Router router = new Router();

        router.post("/foo", contextId -> {
            request.createResponse(contextId, OK);
            ByteBuffer requestBuffer = request.bodyAsByteBuffer(contextId);
            byte[] content = new byte[requestBuffer.remaining()];
            requestBuffer.get(content);
            String body = new String(content);
            ByteBuffer responseBuffer = allocateDirect(body.length()).order(nativeOrder());
            responseBuffer.put(body.getBytes());
            response.body(contextId, responseBuffer);
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
        okhttp3.Request req = new okhttp3.Request.Builder()
                                .url("http://localhost:" + freePort + "/foo")
                                .post(RequestBody.create(MediaType.parse("application/octet-stream"), "hello, world!".getBytes()))
                            .build();
        String body = client.newCall(req).execute().body().string();
        assertEquals("hello, world!", body);
    }
}
