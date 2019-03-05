package io.webfolder.dakota;

import static io.webfolder.dakota.HandlerStatus.accepted;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;

public class TestQueryParam {

    private WebServer server;

    private Map<String, String> queryMap;

    @Before
    public void init() {
        server = new WebServer();

        Router router = new Router();

        router.get("/foo", request -> {
            Response response = request.ok();
            queryMap = request.query();
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
        OkHttpClient client = new Builder().writeTimeout(240, SECONDS).readTimeout(240, SECONDS)
                .connectTimeout(240, SECONDS).build();
        Request req = new okhttp3.Request.Builder().url("http://localhost:8080/foo?foo=bar&abc=123").build();
        client.newCall(req).execute();
        assertNotNull(queryMap);
        assertEquals(2, queryMap.size());
        assertEquals("bar", queryMap.get("foo"));
        assertEquals("123", queryMap.get("abc"));
    }
}
