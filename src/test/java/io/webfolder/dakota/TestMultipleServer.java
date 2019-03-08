package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static io.webfolder.dakota.HttpStatus.OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

public class TestMultipleServer {

    private WebServer server1;

    private WebServer server2;

    private long id1;

    private long id2;

    private int freePort1;

    private int freePort2;

    @Before
    public void init() {
        try (ServerSocket socket = new ServerSocket(0)) {
            this.freePort1 = socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Settings settings1 = new Settings(freePort1);

        server1 = new WebServer(settings1);

        Request request1 = server1.getRequest();
        Response response1 = server1.getResponse();

        Router router1 = new Router();

        router1.get("/foo", contextId -> {
            request1.createResponse(contextId, OK);
            response1.body(contextId, "server1");
            response1.done(contextId);
            id1 = request1.connectionId(contextId);
            return accepted;
        });

        new Thread(() -> server1.run(router1)).start();

        try (ServerSocket socket = new ServerSocket(0)) {
            this.freePort2 = socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Settings settings2 = new Settings(freePort2);

        server2 = new WebServer(settings2);

        Router router2 = new Router();

        Request request2 = server1.getRequest();
        Response response2 = server1.getResponse();
        
        router2.get("/foo", id -> {
            request2.createResponse(id, OK);
            response2.body(id, "server2");
            response2.done(id);
            id2 = request2.connectionId(id);
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
                .connectTimeout(10, SECONDS).retryOnConnectionFailure(true).build();
        okhttp3.Request req1 = new okhttp3.Request.Builder().url("http://localhost:" + freePort1 + "/foo").build();
        okhttp3.Response resp1 = client.newCall(req1).execute();
        String body1 = resp1.body().string();
        assertEquals("server1", body1);
        assertEquals(1, id1);

        okhttp3.Request req2 = new okhttp3.Request.Builder().url("http://localhost:" + freePort2 + "/foo").build();
        okhttp3.Response resp2 = client.newCall(req2).execute();
        String body2 = resp2.body().string();
        assertEquals("server2", body2);
        assertEquals(1, id2);
    }
}
