package io.webfolder.dakota;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.get("/foo", new RouteHandler() {
            @Override
            public boolean handle(Request request) {
                Response response = request.ok();
                response.setBody("hello, world!");
                response.done();
                return true;
            }
        });
        server.run();
    }
}
