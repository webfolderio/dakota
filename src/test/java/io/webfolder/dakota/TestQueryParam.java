package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static io.webfolder.dakota.HttpStatus.OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

public class TestQueryParam {

    private WebServer server;

    private int freePort;

    private Map<String, String> queryMap;

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

        router.get("/foo", contextId -> {
            request.createResponse(contextId, OK);
            queryMap = request.query(contextId);
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
        OkHttpClient client = new Builder().writeTimeout(240, SECONDS).readTimeout(240, SECONDS)
                .connectTimeout(240, SECONDS).build();
        okhttp3.Request req = new okhttp3.Request.Builder().url("http://localhost:" + freePort + "/foo?foo=bar&abc=123").build();
        client.newCall(req).execute();
        assertNotNull(queryMap);
        assertEquals(2, queryMap.size());
        assertEquals("bar", queryMap.get("foo"));
        assertEquals("123", queryMap.get("abc"));
    }
}
