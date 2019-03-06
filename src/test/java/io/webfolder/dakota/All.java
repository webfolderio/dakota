package io.webfolder.dakota;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({
    TestAppendDateHeader.class,
    TestAppendHeader.class,
    TestAsyncHttpGet.class,
    TestHttpClose.class,
    TestHttpGet.class,
    TestHttpHeader.class,
    TestHttpIndexParam.class,
    TestHttpKeepAlive.class,
    TestHttpParam.class,
    TestHttpPost.class,
    TestHttpPostByteArray.class,
    TestMultipleServer.class,
    TestNotFoundHandler.class,
    TestQueryParam.class
})
@RunWith(Suite.class)
public class All {

}
