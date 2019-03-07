package io.webfolder.dakota;

@FunctionalInterface
public interface Handler {

    HandlerStatus handle(long id);
}
