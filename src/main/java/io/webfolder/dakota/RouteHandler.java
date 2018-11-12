package io.webfolder.dakota;

@FunctionalInterface
public interface RouteHandler {

    boolean handle(Request context);
}
