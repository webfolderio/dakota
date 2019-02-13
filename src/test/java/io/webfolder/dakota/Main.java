package io.webfolder.dakota;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) throws IOException {
        WebServer server = new WebServer();

        Router router = new Router();

        AtomicInteger counter = new AtomicInteger();

        router.get("/foo", request -> {
            Response response = request.ok();
            response.setBody("counter: " + counter.incrementAndGet());
            response.done();
            return HandlerStatus.accepted;
        });

        server.run(router);
    }
}
