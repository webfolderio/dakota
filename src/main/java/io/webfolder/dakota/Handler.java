package io.webfolder.dakota;

@FunctionalInterface
public interface Handler {

    HandlerStatus handle(long contextId, Request req, Response res);
}
