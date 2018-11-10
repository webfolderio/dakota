#include "stdafx.h"

#define IDX_METHOD 0
#define IDX_PATH   1
#define IDX_HNDLR  2
#define IDX_LEN    3

static std::atomic<JavaVM*> jvm;
static std::map<std::thread::id, jlong> envCache;

class external_io_context_for_thread_pool_t
{
    restinio::asio_ns::io_context & m_ioctx;

public:
    external_io_context_for_thread_pool_t(
        restinio::asio_ns::io_context & ioctx)
        : m_ioctx{ ioctx }
    {}

    auto & io_context() noexcept { return m_ioctx; }
};

template<typename Io_Context_Holder>
class ioctx_on_thread_pool_t
{
public:
    ioctx_on_thread_pool_t(const ioctx_on_thread_pool_t &) = delete;
    ioctx_on_thread_pool_t(ioctx_on_thread_pool_t &&) = delete;

    template< typename... Io_Context_Holder_Ctor_Args >
    ioctx_on_thread_pool_t(
        std::size_t pool_size,
        Io_Context_Holder_Ctor_Args && ...ioctx_holder_args)
        : m_ioctx_holder{
                std::forward<Io_Context_Holder_Ctor_Args>(ioctx_holder_args)... }
                , m_pool(pool_size)
        , m_status(status_t::stopped)
    {}

    ~ioctx_on_thread_pool_t() {
        if (started()) {
            stop();
            wait();
        }
    }

    void start() {

        if (started()) {
            throw restinio::exception_t{
                "io_context_with_thread_pool is already started" };
        }

        try {
            std::generate(
                begin(m_pool),
                end(m_pool),
                [this] {
                return
                    std::thread{ [this] {

                        JavaVM *vm = jvm.load();
                        if (vm) {
                            JNIEnv* env = NULL;
                            jint ret = vm->AttachCurrentThread((void**)&env, NULL);
                            if (ret == JNI_OK) {
                                envCache[std::this_thread::get_id()] = (jlong)env;

                                thread_local struct DetachOnExit {
                                    ~DetachOnExit() {
                                        JavaVM *vm = jvm.load();
                                        if (vm) {
                                            vm->DetachCurrentThread();
                                        }
                                    }
                                } detachOnExit;
                            }
                        }

                        auto work {
                             restinio::asio_ns::make_work_guard(m_ioctx_holder.io_context())
                        };

                        m_ioctx_holder.io_context().run();
                    } };
            });

            // When all thread started successfully
            // status can be changed.
            m_status = status_t::started;
        }
        catch (const std::exception &)
        {
            io_context().stop();
            for (auto & t : m_pool) {
                if (t.joinable()) {
                    t.join();
                }
            }
        }
    }

    void stop() {
        if (started()) {
            io_context().stop();
        }
    }

    void wait() {
        if (started()) {
            for (auto & t : m_pool) {
                t.join();
            }

            // When all threads are stopped status can be changed.
            m_status = status_t::stopped;
        }
    }

    bool started() const noexcept { return status_t::started == m_status; }

    restinio::asio_ns::io_context & io_context() noexcept {
        return m_ioctx_holder.io_context();
    }

private:
    enum class status_t : std::uint8_t { stopped, started };

    Io_Context_Holder m_ioctx_holder;
    std::vector<std::thread> m_pool;
    status_t m_status;
};

struct dakota_traits : public restinio::default_traits_t {
    using request_handler_t = restinio::router::express_router_t<>;
};

static struct ReflectionUtil {

    ReflectionUtil() {
        // no op
    }

    ReflectionUtil(JNIEnv* env, bool callGlobal) {
        global = callGlobal;
        class_requestImpl = getClass(env, "io/webfolder/dakota/RequestImpl");
        constructor_request = getMethod(env, class_requestImpl, "<init>", "(J)V");
        class_routeHandler = getClass(env, "io/webfolder/dakota/RouteHandler");
        method_handle = getMethod(env, class_routeHandler, "handle", "(Lio/webfolder/dakota/Request;)V");
        class_server = getClass(env, "io/webfolder/dakota/Server");
        field_pool = getField(env, class_server, "pool", "J");
        reflection_util = getField(env, class_server, "reflectionUtil", "J");
    }

public:
    jclass request() { return class_requestImpl; }
    jclass handler() { return class_routeHandler; }
    jmethodID constructorRequest() { return constructor_request; }
    jmethodID handle() { return method_handle; }
    jfieldID pool() { return field_pool; }
    jfieldID reflectionUtil() { return reflection_util; }

    void dispose(JNIEnv *env) {
        if (global) {
            env->DeleteGlobalRef(class_requestImpl);
            env->DeleteGlobalRef(class_routeHandler);
            env->DeleteGlobalRef((jobject)constructor_request);
            env->DeleteGlobalRef((jobject)method_handle);
        }
    }

private:
    bool global;
    jclass class_requestImpl, class_routeHandler, class_server;
    jmethodID constructor_request, method_handle;
    jfieldID field_pool, reflection_util;

    jclass getClass(JNIEnv *env, const char *className) {
        jclass klass = env->FindClass(className);
        if (global) {
            klass = (jclass)env->NewGlobalRef(klass);
        }
        return klass;
    }

    jmethodID getMethod(JNIEnv *env, jclass klass, const char *name, const char *signature) {
        jmethodID method = (jmethodID)env->GetMethodID(klass, name, signature);
        if (global) {
            method = (jmethodID)env->NewGlobalRef((jobject)method);
        }
        return method;
    }

    jfieldID getField(JNIEnv *env, jclass klass, const char *name, const char *signature) {
        jfieldID field = env->GetFieldID(klass, name, signature);
        return field;
    }
};

static struct Server {

    using thread_pool_t = ioctx_on_thread_pool_t<external_io_context_for_thread_pool_t>;

    Server() {
        // no op
    }

    Server(JNIEnv *env) {
        JNINativeMethod methods[] = {
            "_run", "([[Ljava/lang/Object;)V", (void *)&Server::run,
            "_stop", "()V", (void *)&Server::stop,
        };
        jclass klass = env->FindClass("io/webfolder/dakota/Server");
        env->RegisterNatives(klass, methods, sizeof(methods) / sizeof(methods[0]));
    }

public:

    static restinio::http_method_t to_method(const char *method) {
        restinio::http_method_t httpMethod;
        if (strcmp(method, "get") == 0) {
            httpMethod = restinio::http_method_t::http_get;
        }
        else if (strcmp(method, "post") == 0) {
            httpMethod = restinio::http_method_t::http_post;
        }
        else if (strcmp(method, "delete") == 0) {
            httpMethod = restinio::http_method_t::http_delete;
        }
        else if (strcmp(method, "head") == 0) {
            httpMethod = restinio::http_method_t::http_head;
        }
        return httpMethod;
    }

    static JNIEXPORT void JNICALL run(JNIEnv *env, jobject that, jobjectArray routes) {
        auto router = std::make_unique<restinio::router::express_router_t<>>();

        ReflectionUtil reflect = { env, true };

        jsize len = env->GetArrayLength(routes);
        for (jsize i = 0; i < len; i++) {
            jobjectArray next = (jobjectArray)env->GetObjectArrayElement(routes, i);
            if (env->GetArrayLength(next) != IDX_LEN) {
                continue;
            }
            jstring method = (jstring)env->GetObjectArrayElement(next, IDX_METHOD);
            const char *c_method = env->GetStringUTFChars(method, JNI_FALSE);
            jstring path = (jstring)env->GetObjectArrayElement(next, IDX_PATH);
            const char *c_path = env->GetStringUTFChars(path, JNI_FALSE);
            jobject handler = (jobject)env->GetObjectArrayElement(next, IDX_HNDLR);
            handler = env->NewGlobalRef(handler);

            restinio::http_method_t httpMethod = to_method(c_method);

            router->add_handler(httpMethod,
                c_path,
                [&](restinio::request_handle_t req,
                    restinio::router::route_params_t params) {

                jlong ptr_req = (jlong)&req;

                auto env_route = *(JNIEnv **)&envCache[std::this_thread::get_id()];

                jobject requestImpl = env_route->NewObject(reflect.request(),
                    reflect.constructorRequest(),
                    ptr_req);

                requestImpl = env_route->NewGlobalRef(requestImpl);
                env_route->CallObjectMethod(handler, reflect.handle(), requestImpl);

                if (env_route->ExceptionCheck()) {
                    env_route->DeleteGlobalRef(requestImpl);
                    requestImpl = NULL;
                }

                auto resp = req->create_response();
                resp.set_body("test");

                return resp.done([requestImpl](const auto & ec) {
                    auto env_done = *(JNIEnv **)&envCache[std::this_thread::get_id()];
                    if (env_done) {
                        if (requestImpl != NULL) {
                            env_done->DeleteGlobalRef(requestImpl);
                        }
                    }
                });
            });

            env->ReleaseStringUTFChars(method, c_method);
            env->ReleaseStringUTFChars(path, c_path);
        }

        restinio::asio_ns::io_context ioctx;

        auto pool_size = std::thread::hardware_concurrency();

        using settings_t = restinio::run_on_thread_pool_settings_t<dakota_traits>;
        using server_t = restinio::http_server_t<dakota_traits>;

        auto settings = restinio::on_thread_pool<dakota_traits>(pool_size)
                                    .port(8080)
                                    .address("localhost")
                                    .request_handler(std::move(router));

        thread_pool_t pool{ pool_size, ioctx };

        server_t server{
            restinio::external_io_context(pool.io_context()),
            std::forward<settings_t>(settings) };

        server.open_sync();
        pool.start();

        if (pool.started()) {
            env->SetLongField(that, reflect.pool(), (jlong)&pool);
            env->SetLongField(that, reflect.reflectionUtil(), (jlong)&reflect);
        }

        pool.wait();
    }

    static JNIEXPORT void JNICALL stop(JNIEnv *env, jobject that) {
        ReflectionUtil util{ env, false };
        jlong ptr_pool = env->GetLongField(that, util.pool());
        auto *pool = *(thread_pool_t **) &ptr_pool;
        pool->stop();
        jlong ptr_reflect = env->GetLongField(that, util.reflectionUtil());
        auto *relect = *(ReflectionUtil **) &ptr_reflect;
        relect->dispose(env);
    }
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_EVERSION;
    }
    jvm = vm;
    Server server{ env };
    return JNI_VERSION_1_8;
}
