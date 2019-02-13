package io.webfolder.dakota;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        WebServer server = new WebServer();

        Router router = new Router();

        router.get("/foo", request -> {
            System.out.println("ok");
            Response response = request.ok();
            System.out.println("body");
            response.setBody("foo");
            System.out.println("done");
            response.done();
            return HandlerStatus.accepted;
        });

        server.run(router);
    }
}
