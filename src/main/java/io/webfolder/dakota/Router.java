package io.webfolder.dakota;

import static java.lang.System.arraycopy;

public class Router {

    private Object[][] routes = new Object[0][];

    public Router get(String path, Handler handler) {
        addHandler("get", path, handler);
        return this;
    }

    public Router post(String path, Handler handler) {
        addHandler("post", path, handler);
        return this;
    }

    public Router delete(String path, Handler handler) {
        addHandler("delete", path, handler);
        return this;
    }

    public Router head(String path, Handler handler) {
        addHandler("head", path, handler);
        return this;
    }

    private void addHandler(String method, String path, Handler handler) {
        Object[][] copy = new Object[routes.length + 1][];
        arraycopy(routes, 0, copy, 0, routes.length);
        copy[routes.length] = new Object[] { method, path, handler };
        routes = copy;
    }

    Object[][] getRoutes() {
        return routes;
    }
}
