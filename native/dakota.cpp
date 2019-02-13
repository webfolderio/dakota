#include "stdafx.h"

#define IDX_METHOD 0
#define IDX_PATH   1
#define IDX_HNDLR  2
#define IDX_LEN    3

#define HANDLER_STATUS_REJECTED 0
#define HANDLER_STATUS_ACCEPTED 1

static std::atomic<JavaVM*> jvm;
static std::map<std::thread::id, jlong> envCache;

static class external_io_context_for_thread_pool_t
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
                            jint ret = vm->AttachCurrentThreadAsDaemon((void**)&env, NULL);
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

static struct dakota_traits : public restinio::default_traits_t {
    using request_handler_t = restinio::router::express_router_t<>;
};

static enum ReflectionCacheType {
    global,
    local
};

static class String {
    JNIEnv * env_;
    jstring java_str_;
    const char * str_;
public:
    String(const String &) = delete;
    String(String &&) = delete;
    String(JNIEnv * env, jstring from)
        : env_{ env }, java_str_(from), str_{ env->GetStringUTFChars(from, JNI_FALSE) }
    {}
    ~String() {
        env_->ReleaseStringUTFChars(java_str_, str_);
    }
    const char * c_str() const noexcept { return str_; }
};

static class JavaClass {

    JNIEnv *env_;
    ReflectionCacheType type_;
    jclass klass_;

public:

    JavaClass(const JavaClass &) = delete;
    JavaClass(JavaClass &&) = delete;
    ~JavaClass() {
        switch (type_) {
        case global: env_->DeleteGlobalRef(klass_);
        default: env_->DeleteLocalRef(klass_);
        }
    }

    JavaClass(JNIEnv *env, const char *className) : JavaClass(env, local, className) { }

    JavaClass(JNIEnv *env, ReflectionCacheType type, const char *className) :
        env_(env), type_(type) {
        jclass klass = env_->FindClass(className);
        if (type_ == global) {
            klass_ = (jclass)env_->NewGlobalRef(klass);
            env->DeleteLocalRef(klass);
        }
        else {
            klass_ = klass;
        }
    }

    jclass get() const noexcept {
        return klass_;
    }
};

static class JavaMethod {

    JNIEnv *env_;
    ReflectionCacheType type_;
    jmethodID method_;

public:

    JavaMethod(const JavaMethod &) = delete;
    JavaMethod(JavaMethod &&) = delete;
    ~JavaMethod() {
        switch (type_) {
        case global: env_->DeleteGlobalRef((jobject)method_);
        default: env_->DeleteLocalRef((jobject)method_);
        }
    }

    JavaMethod(JNIEnv *env, const char *klass,
        const char *name, const char *signature)
        : JavaMethod(env, local, klass, name, signature) { }

    JavaMethod(JNIEnv *env, ReflectionCacheType type,
        const char *klass, const char *name,
        const char *signature) :
        env_(env), type_(type) {
        jclass klass_ = env_->FindClass(klass);
        if (klass_) {
            jmethodID method = env->GetMethodID(klass_, name, signature);
            if (type_ == global) {
                method_ = (jmethodID)env_->NewGlobalRef((jobject)method);
            }
            else {
                method_ = method;
            }
            env->DeleteLocalRef(klass_);
        }
    }

    jmethodID get() const noexcept {
        return method_;
    }
};

static class Context {
    restinio::request_handle_t *req_;
    restinio::response_builder_t<restinio::restinio_controlled_output_t> *res_;
    jobject requestObject_;

public:
    Context(const JavaMethod &) = delete;
    Context(JavaMethod &&) = delete;
    ~Context() {
        delete req_;
        delete res_;
    }
    Context(restinio::request_handle_t *req) : req_(req) {
    }
    restinio::request_handle_t *request() const {
        return req_;
    }
    restinio::response_builder_t<restinio::restinio_controlled_output_t> *response() const {
        return res_;
    }
    void setResponse(restinio::response_builder_t<restinio::restinio_controlled_output_t> *response) {
        res_ = response;
    }
    void setRequestObject(jobject requestObject) {
        requestObject_ = requestObject;
    }
    jobject requestObject() {
        return requestObject_;
    }
};

static class JavaField {

    jfieldID field_;

public:

    JavaField(JNIEnv* env, const const char *klass, const char *name, const char *signature) {
        jclass javaClass = env->FindClass(klass);
        if (javaClass) {
            field_ = env->GetFieldID(javaClass, name, signature);
            env->DeleteLocalRef(javaClass);
        }
    }

    jfieldID get() {
        return field_;
    }
};

static struct Restinio {

    using thread_pool_t = ioctx_on_thread_pool_t<external_io_context_for_thread_pool_t>;

    Restinio(JNIEnv *env) {
        JNINativeMethod serverMethods[] = {
            "_run", "([[Ljava/lang/Object;)V", (void *)&Restinio::run,
            "_stop", "()V", (void *)&Restinio::stop,
        };

        JavaClass server{ env,  "io/webfolder/dakota/WebServer" };
        env->RegisterNatives(server.get(), serverMethods, sizeof(serverMethods) / sizeof(serverMethods[0]));

        JNINativeMethod requestImpl[] = {
            "_createResponse", "(ILjava/lang/String;)V", (void *)&Restinio::createResponse
        };

        JavaClass request = { env, "io/webfolder/dakota/RequestImpl" };
        env->RegisterNatives(request.get(), requestImpl, sizeof(requestImpl) / sizeof(requestImpl[0]));

        JNINativeMethod responseImpl[] = {
            "_done", "()V", (void *)&Restinio::done,
            "_setBody", "(Ljava/lang/String;)V", (void *)&Restinio::setBody
        };
        JavaClass response = { env, "io/webfolder/dakota/ResponseImpl" };
        env->RegisterNatives(response.get(), responseImpl, sizeof(responseImpl) / sizeof(responseImpl[0]));
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
        auto klassRequest = new JavaClass{ env, global, "io/webfolder/dakota/RequestImpl" };
        auto constructorRequest = new JavaMethod{ env, global, "io/webfolder/dakota/RequestImpl", "<init>", "(J)V" };
        auto handleMethod = new JavaMethod{ env, global, "io/webfolder/dakota/Handler", "handle", "(Lio/webfolder/dakota/Request;)Lio/webfolder/dakota/HandlerStatus;" };
        auto statusField = new JavaField{ env, "io/webfolder/dakota/HandlerStatus", "value", "I" };

        auto executeHandler = [&](jobject handler,
            restinio::request_handle_t req,
            restinio::router::route_params_t params) {

            auto context = new Context{
                new restinio::request_handle_t{ req }
            };

            auto envCurrentThread = *(JNIEnv **)&envCache[std::this_thread::get_id()];
            if (envCurrentThread == NULL) {
                return restinio::request_rejected();
            }
            jobject request = envCurrentThread->NewObject(klassRequest->get(),
                constructorRequest->get(), (jlong)context);
            jobject globalRequest = envCurrentThread->NewGlobalRef(request);
            envCurrentThread->DeleteLocalRef(request);
            context->setRequestObject(globalRequest);
            jobject handlerStatus = envCurrentThread->CallObjectMethod(handler, handleMethod->get(), globalRequest);
            if (envCurrentThread->ExceptionCheck()) {
                envCurrentThread->DeleteGlobalRef(globalRequest);
                return restinio::request_rejected();
            }
            jint status = (jint)envCurrentThread->GetIntField(handlerStatus, statusField->get());
            switch (status) {
            case HANDLER_STATUS_ACCEPTED: return restinio::request_accepted();
            default: return restinio::request_rejected();
            }
        };

        auto router = std::make_unique<restinio::router::express_router_t<>>();
        jsize len = env->GetArrayLength(routes);
        for (jsize i = 0; i < len; i++) {
            jobjectArray next = (jobjectArray)env->GetObjectArrayElement(routes, i);
            if (env->GetArrayLength(next) != IDX_LEN) {
                continue;
            }
            String method{ env, (jstring)env->GetObjectArrayElement(next, IDX_METHOD) };
            String path{ env, (jstring)env->GetObjectArrayElement(next, IDX_PATH) };
            jobject handler = (jobject)env->GetObjectArrayElement(next, IDX_HNDLR);
            restinio::http_method_t httpMethod = to_method(method.c_str());

            router->add_handler(httpMethod, path.c_str(), [executeHandler, handler](auto req, auto params) {
                return executeHandler(handler, std::move(req), std::move(params));
            });
        }

        auto pool_size = std::thread::hardware_concurrency();

        using settings_t = restinio::run_on_thread_pool_settings_t<dakota_traits>;
        using server_t = restinio::http_server_t<dakota_traits>;

        restinio::asio_ns::io_context ioctx;
        auto pool = new thread_pool_t{ pool_size, ioctx };

        auto settings = restinio::on_thread_pool<dakota_traits>(pool_size)
            .port(8080)
            .address("localhost")
            .cleanup_func([pool, klassRequest, constructorRequest, handleMethod, statusField]() {
            delete pool, klassRequest, constructorRequest,
                handleMethod, statusField;
        })
            .request_handler(std::move(router));

        server_t server{
            restinio::external_io_context(ioctx),
            std::forward<settings_t>(settings)
        };

        server.open_sync();
        pool->start();

        if (pool->started()) {
            JavaField field = { env, "io/webfolder/dakota/WebServer", "pool", "J" };
            env->SetLongField(that, field.get(), (jlong)pool);
        }

        pool->wait();
    }

    static JNIEXPORT void JNICALL stop(JNIEnv *env, jobject that) {
        JavaField field = { env, "io/webfolder/dakota/WebServer", "pool", "J" };
        jlong ptr = env->GetLongField(that, field.get());
        auto *pool = *(thread_pool_t **)ptr;
        pool->stop();
    }

    static JNIEXPORT void JNICALL createResponse(JNIEnv *env, jobject that, jint status, jstring reasonPhrase) {
        JavaField field = { env, "io/webfolder/dakota/RequestImpl", "context", "J" };
        jlong ptr = env->GetLongField(that, field.get());
        auto *context = *(Context **)&ptr;
        restinio::request_handle_t *request = context->request();
        String str{ env, reasonPhrase };
        auto response = std::make_unique<restinio::response_builder_t<restinio::restinio_controlled_output_t>>(
            (*request)->create_response(status, str.c_str()));
        context->setResponse(response.release());
    }

    static JNIEXPORT void JNICALL setBody(JNIEnv *env, jobject that, jstring body) {
        JavaField field = { env, "io/webfolder/dakota/ResponseImpl", "context", "J" };
        jlong ptr = env->GetLongField(that, field.get());
        auto *context = *(Context **)&ptr;
        String str{ env, body };
        context->response()->set_body(str.c_str());
    }

    static JNIEXPORT void JNICALL done(JNIEnv *env, jobject that) {
        JavaField field = { env, "io/webfolder/dakota/ResponseImpl", "context", "J" };
        jlong ptr = env->GetLongField(that, field.get());
        auto *context = *(Context **)&ptr;
        context->response()->done([context, env](const auto & ec) {
            // env->DeleteGlobalRef(context->requestObject());
            delete context;
        });
    }
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_EVERSION;
    }
    jvm = vm;
    Restinio restinio{ env };
    
    return JNI_VERSION_1_8;
}
