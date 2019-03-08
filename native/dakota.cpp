#include "stdafx.h"

#ifdef __GNUG__
#pragma GCC diagnostic ignored "-Wwrite-strings"
#endif

#define IDX_METHOD 0
#define IDX_PATH 1
#define IDX_HNDLR 2
#define IDX_LEN 3

#define HANDLER_STATUS_REJECTED 0
#define HANDLER_STATUS_ACCEPTED 1

static std::atomic<JavaVM*> jvm;
static std::map<std::thread::id, jlong> envCache;

class external_io_context_for_thread_pool_t {
    restinio::asio_ns::io_context& m_ioctx;

public:
    external_io_context_for_thread_pool_t(
        restinio::asio_ns::io_context& ioctx)
        : m_ioctx{ ioctx }
    {
    }

    auto& io_context() noexcept { return m_ioctx; }
};

template <typename Io_Context_Holder>
class ioctx_on_thread_pool_t {
public:
    ioctx_on_thread_pool_t(const ioctx_on_thread_pool_t&) = delete;
    ioctx_on_thread_pool_t(ioctx_on_thread_pool_t&&) = delete;

    template <typename... Io_Context_Holder_Ctor_Args>
    ioctx_on_thread_pool_t(
        std::size_t pool_size,
        Io_Context_Holder_Ctor_Args&&... ioctx_holder_args)
        : m_ioctx_holder{
            std::forward<Io_Context_Holder_Ctor_Args>(ioctx_holder_args)...
        }
        , m_pool(pool_size)
        , m_status(status_t::stopped)
    {
    }

    ~ioctx_on_thread_pool_t()
    {
        if (started()) {
            stop();
            wait();
        }
    }

    void start()
    {

        if (started()) {
            throw restinio::exception_t{
                "io_context_with_thread_pool is already started"
            };
        }

        try {
            std::generate(
                begin(m_pool),
                end(m_pool),
                [this] {
                    return std::thread{ [this] {
                        JavaVM* vm = jvm.load();
                        if (vm) {
                            JNIEnv* env = nullptr;
                            jint ret = vm->AttachCurrentThreadAsDaemon((void**)&env, nullptr);
                            if (ret == JNI_OK) {
                                envCache.insert({ std::move(std::this_thread::get_id()), (jlong)env });

                                thread_local struct DetachOnExit {
                                    ~DetachOnExit()
                                    {
                                        JavaVM* vm = jvm.load();
                                        if (vm) {
                                            vm->DetachCurrentThread();
                                        }
                                    }
                                } detachOnExit;
                            }
                        }

                        auto work{
                            restinio::asio_ns::make_work_guard(m_ioctx_holder.io_context())
                        };

                        m_ioctx_holder.io_context().run();
                    } };
                });

            // When all thread started successfully
            // status can be changed.
            m_status = status_t::started;
        } catch (const std::exception&) {
            io_context().stop();
            for (auto& t : m_pool) {
                if (t.joinable()) {
                    t.join();
                }
            }
        }
    }

    void stop()
    {
        if (started()) {
            io_context().stop();
        }
    }

    void wait()
    {
        if (started()) {
            for (auto& t : m_pool) {
                t.join();
            }
            // When all threads are stopped status can be changed.
            m_status = status_t::stopped;
        }
    }

    bool started() const noexcept { return status_t::started == m_status; }

    restinio::asio_ns::io_context& io_context() noexcept
    {
        return m_ioctx_holder.io_context();
    }

private:
    enum class status_t : std::uint8_t { stopped,
        started };

    Io_Context_Holder m_ioctx_holder;
    std::vector<std::thread> m_pool;
    status_t m_status;
};

#ifdef _DEBUG
struct dakota_traits : public restinio::traits_t<restinio::asio_timer_manager_t, restinio::single_threaded_ostream_logger_t> {
    using request_handler_t = restinio::router::express_router_t<>;
};
#else
struct dakota_traits : public restinio::traits_t<restinio::asio_timer_manager_t, restinio::null_logger_t> {
    using request_handler_t = restinio::router::express_router_t<>;
};
#endif

enum ReflectionCacheType {
    global,
    local
};

class String {
    JNIEnv* env_;
    jstring java_str_;
    const char* str_;

public:
    String(const String&) = delete;
    String(String&&) = delete;
    String(JNIEnv* env, jstring from)
        : env_{ env }
        , java_str_(from)
        , str_{ env->GetStringUTFChars(from, JNI_FALSE) }
    {
    }
    ~String()
    {
        env_->ReleaseStringUTFChars(java_str_, str_);
    }
    const char* c_str() const noexcept { return str_; }
};

class JavaClass {

    JNIEnv* env_;
    ReflectionCacheType type_;
    jclass klass_;

public:
    JavaClass(const JavaClass&) = delete;
    JavaClass(JavaClass&&) = delete;
    ~JavaClass()
    {
        switch (type_) {
        case global:
            env_->DeleteGlobalRef(klass_);
            break;
        default:
            env_->DeleteLocalRef(klass_);
        }
    }

    JavaClass(JNIEnv* env, const char* className)
        : JavaClass(env, local, className)
    {
    }

    JavaClass(JNIEnv* env, ReflectionCacheType type, const char* className)
        : env_(env)
        , type_(type)
    {
        jclass klass = env_->FindClass(className);
        if (type_ == global) {
            klass_ = (jclass)env_->NewGlobalRef(klass);
            env->DeleteLocalRef(klass);
        } else {
            klass_ = klass;
        }
    }

    jclass get() const noexcept
    {
        return klass_;
    }
};

class JavaMethod {

    JNIEnv* env_;
    ReflectionCacheType type_;
    jmethodID method_;

public:
    JavaMethod(const JavaMethod&) = delete;
    JavaMethod(JavaMethod&&) = delete;
    ~JavaMethod()
    {
        switch (type_) {
        case global:
            env_->DeleteGlobalRef((jobject)method_);
            break;
        default:
            env_->DeleteLocalRef((jobject)method_);
        }
    }

    JavaMethod(JNIEnv* env, const char* klass,
        const char* name, const char* signature)
        : JavaMethod(env, local, klass, name, signature)
    {
    }

    JavaMethod(JNIEnv* env, ReflectionCacheType type,
        const char* klass, const char* name,
        const char* signature)
        : env_(env)
        , type_(type)
    {
        jclass klass_ = env_->FindClass(klass);
        if (klass_) {
            jmethodID method = env->GetMethodID(klass_, name, signature);
            if (type_ == global) {
                method_ = (jmethodID)env_->NewGlobalRef((jobject)method);
            } else {
                method_ = (jmethodID)env_->NewLocalRef((jobject)method);
            }
            env->DeleteLocalRef(klass_);
        }
    }

    jmethodID get() const noexcept
    {
        return method_;
    }
};

class Context {
private:
    restinio::request_handle_t* req_;
    restinio::response_builder_t<restinio::restinio_controlled_output_t>* res_;
    restinio::router::route_params_t* params_;
    jbyte* responseBuffer_;
    jint releaseMode_;
    jbyteArray body_;
    bool releaseBuffer_ = false;

public:
    Context(const Context&) = delete;
    Context(Context&&) = delete;
    ~Context()
    {
        if (req_)
            delete req_;
        if (res_)
            delete res_;
        if (params_)
            delete params_;
    }
    Context(restinio::request_handle_t* req, restinio::router::route_params_t* params)
        : req_(req)
        , params_(std::move(params))
    {
    }
    restinio::request_handle_t* request() const
    {
        return req_;
    }
    restinio::response_builder_t<restinio::restinio_controlled_output_t>* response() const
    {
        return res_;
    }
    void setResponseBuffer(jbyte* buffer)
    {
        responseBuffer_ = buffer;
        releaseBuffer_ = true;
    }
    void setResponseBody(jbyteArray body)
    {
        body_ = body;
    }
    void setReleaseMode(jint mode)
    {
        releaseMode_ = mode;
    }
    bool releaseBuffer() const
    {
        return releaseBuffer_;
    }
    jbyte* getResponseBuffer() const
    {
        return responseBuffer_;
    }
    jbyteArray getBody() const
    {
        return body_;
    }
    jint getReleaseMode() const
    {
        return releaseMode_;
    }
    void setResponse(restinio::response_builder_t<restinio::restinio_controlled_output_t>* response)
    {
        res_ = response;
    }
    jstring param(JNIEnv* env, const char* name) const
    {
        if ((*params_).has(name)) {
            auto value = restinio::cast_to<std::string>((*params_)[std::string(name)]);
            return env->NewStringUTF(value.c_str());
        } else {
            return nullptr;
        }
    }
    jstring param(JNIEnv* env, long unsigned int index) const
    {
        if ((*params_).indexed_parameters_size() > 0 && index < (*params_).indexed_parameters_size()) {
            auto value = restinio::cast_to<std::string>((*params_)[index]);
            return env->NewStringUTF(value.c_str());
        } else {
            return nullptr;
        }
    }
    jint namedParamSize() const
    {
        return (jint)(*params_).named_parameters_size();
    }
    jint indexedParamSize() const
    {
        return (jint)(*params_).indexed_parameters_size();
    }
};

class JavaField {

    jfieldID field_;

public:
    JavaField(JNIEnv* env, const char* klass, const char* name, const char* signature)
    {
        jclass javaClass = env->FindClass(klass);
        if (javaClass) {
            field_ = env->GetFieldID(javaClass, name, signature);
            env->DeleteLocalRef(javaClass);
        }
    }

    JavaField(JNIEnv* env, JavaClass* klass, const char* name, const char* signature)
    {
        field_ = env->GetFieldID((*klass).get(), name, signature);
    }

    jfieldID get() const
    {
        return field_;
    }
};

static CTSL::HashMap<std::uint64_t, Context*> connections;

struct Restinio {

    using thread_pool_t = ioctx_on_thread_pool_t<external_io_context_for_thread_pool_t>;

    Restinio(JNIEnv* env)
    {
        JNINativeMethod serverMethods[] = {
            "_run", "(Lio/webfolder/dakota/Settings;[[Ljava/lang/Object;Lio/webfolder/dakota/Handler;)V", (void*)&Restinio::run,
            "_stop", "()V", (void*)&Restinio::stop
        };

        JavaClass server{ env, "io/webfolder/dakota/WebServer" };
        env->RegisterNatives(server.get(), serverMethods, sizeof(serverMethods) / sizeof(serverMethods[0]));

        JNINativeMethod requestImpl[] = {
            "_createResponse", "(JILjava/lang/String;)V", (void*)&Restinio::createResponse,
            "_query", "(J)Ljava/util/Map;", (void*)&Restinio::query,
            "_header", "(J)Ljava/util/Map;", (void*)&Restinio::header,
            "_target", "(J)Ljava/lang/String;", (void*)&Restinio::target,
            "_param", "(JLjava/lang/String;)Ljava/lang/String;", (void*)&Restinio::paramByName,
            "_param", "(JI)Ljava/lang/String;", (void*)&Restinio::getParamByIndex,
            "_namedParamSize", "(J)I", (void*)&Restinio::namedParamSize,
            "_indexedParamSize", "(J)I", (void*)&Restinio::indexedParamSize,
            "_body", "(J)Ljava/lang/String;", (void*)&Restinio::body,
            "_length", "(J)J", (void*)&Restinio::length,
            "_bodyAsByteArray", "(J)[B", (void*)&Restinio::bodyAsByteArray,
            "_bodyAsByteBuffer", "(JLjava/nio/ByteBuffer;)V", (void*)&Restinio::bodyAsByteBuffer,
            "_connectionId", "(J)J", (void*)&Restinio::connectionId
        };

        JavaClass request = { env, "io/webfolder/dakota/RequestImpl" };
        env->RegisterNatives(request.get(), requestImpl, sizeof(requestImpl) / sizeof(requestImpl[0]));

        JNINativeMethod responseImpl[] = {
            "_done", "(J)V", (void*)&Restinio::done,
            "_body", "(JLjava/lang/String;)V", (void*)&Restinio::setBody,
            "_body", "(JLjava/nio/ByteBuffer;)V", (void*)&Restinio::setBodyByteBuffer,
            "_body", "(J[B)V", (void*)&Restinio::setBodyByteArray,
            "_appendHeader", "(JLjava/lang/String;Ljava/lang/String;)V", (void*)&Restinio::appendHeader,
            "_closeConnection", "(J)V", (void*)&Restinio::closeConnection,
            "_keepAliveConnection", "(J)V", (void*)&Restinio::keepAliveConnection,
            "_appendHeaderDateField", "(J)V", (void*)&Restinio::appendHeaderDateField
        };
        JavaClass response = { env, "io/webfolder/dakota/ResponseImpl" };
        env->RegisterNatives(response.get(), responseImpl, sizeof(responseImpl) / sizeof(responseImpl[0]));
    }

public:
    static restinio::http_method_t to_method(const char* method)
    {
        restinio::http_method_t httpMethod;
        if (strcmp(method, "get") == 0) {
            httpMethod = restinio::http_method_t::http_get;
        } else if (strcmp(method, "post") == 0) {
            httpMethod = restinio::http_method_t::http_post;
        } else if (strcmp(method, "delete") == 0) {
            httpMethod = restinio::http_method_t::http_delete;
        } else if (strcmp(method, "head") == 0) {
            httpMethod = restinio::http_method_t::http_head;
        }
        return httpMethod;
    }

    static void run(JNIEnv* env, jobject that, jobject serverSettings, jobjectArray routes, jobject nonMatchedHandler)
    {
        JavaMethod mGetPort{ env, "io/webfolder/dakota/Settings", "getPort", "()I" };
        JavaMethod mAddress{ env, "io/webfolder/dakota/Settings", "getAddress", "()Ljava/lang/String;" };
        jint port = (jint)env->CallIntMethod(serverSettings, mGetPort.get());
        jstring address = (jstring)env->CallObjectMethod(serverSettings, mAddress.get());
        String s_address{ env, address };

        JavaClass klassRequest{ env, "io/webfolder/dakota/RequestImpl" };
        JavaMethod handleMethod{ env, "io/webfolder/dakota/Handler", "handle", "(J)Lio/webfolder/dakota/HandlerStatus;" };
        JavaField statusField{ env, "io/webfolder/dakota/HandlerStatus", "value", "I" };

        std::random_device dev;
        std::mt19937 rng(dev());
        auto random = rng();

        auto execute = [&](jobject handler,
                           restinio::request_handle_t req,
                           restinio::router::route_params_t params) {
            auto context = new Context{
                new restinio::request_handle_t{ req },
                new restinio::router::route_params_t{ std::move(params) }
            };
            auto envCurrentThread = *(JNIEnv**)&envCache.at(std::move(std::this_thread::get_id()));
            if (envCurrentThread == nullptr) {
                delete context;
                return restinio::request_rejected();
            }
            jlong contextId = (jlong)(random | (std::uint64_t)context);
            connections.insert(contextId, context);
            jobject handlerStatus = envCurrentThread->CallObjectMethod(handler, handleMethod.get(), contextId);
            if (envCurrentThread->ExceptionCheck()) {
                connections.erase(contextId);
                delete context;
                return restinio::request_rejected();
            }
            jint status = (jint)envCurrentThread->GetIntField(handlerStatus, statusField.get());
            switch (status) {
            case HANDLER_STATUS_ACCEPTED:
                return restinio::request_accepted();
            default:
                return restinio::request_rejected();
            }
        };
        auto router = std::make_unique<restinio::router::express_router_t<>>();
        if (nonMatchedHandler != nullptr) {
            router->non_matched_request_handler(
                [execute, nonMatchedHandler](auto req) {
                    return execute(nonMatchedHandler, std::move(req), std::move(restinio::router::route_params_t{}));
                });
        }
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
            router->add_handler(httpMethod, path.c_str(), [execute, handler](auto req, auto params) {
                return execute(handler, std::move(req), std::move(params));
            });
        }

        auto pool_size = std::thread::hardware_concurrency();

        using settings_t = restinio::run_on_thread_pool_settings_t<dakota_traits>;
        using server_t = restinio::http_server_t<dakota_traits>;

        restinio::asio_ns::io_context ioctx;
        thread_pool_t pool{ pool_size, ioctx };

        server_t* server = nullptr;

        auto settings = restinio::on_thread_pool<dakota_traits>(pool_size)
                            .port((uint16_t)port)
                            .address(s_address.c_str())
                            .request_handler(std::move(router));

        server = new server_t{
            restinio::external_io_context(ioctx),
            std::forward<settings_t>(settings)
        };

        asio::post(ioctx, [&] {
            server->open_sync();
        });

        try {
            pool.start();
        } catch (const std::exception& ex) {
            JavaClass exceptionClass{ env, "io/webfolder/dakota/DakotaException" };
            env->ThrowNew(exceptionClass.get(), ex.what());
            if (server)
                delete server;
            return;
        }

        JavaField fServer = { env, "io/webfolder/dakota/WebServer", "server", "J" };
        env->SetLongField(that, fServer.get(), (jlong)server);

        pool.wait();
    }

    static void stop(JNIEnv* env, jobject that)
    {
        JavaField fServer = { env, "io/webfolder/dakota/WebServer", "server", "J" };
        jlong ptrServer = env->GetLongField(that, fServer.get());
        using server_t = restinio::http_server_t<dakota_traits>;
        if (ptrServer > 0) {
            auto* server = (server_t*)ptrServer;
            server->close_sync();
            server->io_context().stop();
            delete server;
        }
    }

    static void createResponse(JNIEnv* env, jobject that, jlong contextId, jint status, jstring reasonPhrase)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            restinio::request_handle_t* request = context->request();
            String str{ env, reasonPhrase };
            restinio::http_status_line_t status_line = restinio::http_status_line_t{ restinio::http_status_code_t{ (uint16_t)status }, str.c_str() };
            auto response = std::make_unique<restinio::response_builder_t<restinio::restinio_controlled_output_t>>(
                (*request)->create_response(status_line));
            context->setResponse(response.release());
        }
    }

    static jobject query(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            restinio::request_handle_t* request = context->request();
            const auto qp = restinio::parse_query((*request)->header().query());
            if (qp.empty()) {
                return nullptr;
            }
            JavaClass kMap{ env, "java/util/LinkedHashMap" };
            JavaMethod cMap{ env, "java/util/LinkedHashMap", "<init>", "(I)V" };
            JavaMethod mPut{ env, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;" };
            jobject map = env->NewObject(kMap.get(), cMap.get(), (jint)qp.size());
            for (const auto p : qp) {
                std::string first = restinio::cast_to<std::string>(p.first);
                std::string second = restinio::cast_to<std::string>(p.second);
                jstring j_first = env->NewStringUTF(first.c_str());
                jstring j_second = env->NewStringUTF(second.c_str());
                env->CallObjectMethod(map, mPut.get(), j_first, j_second);
            }
            return map;
        } else {
            return nullptr;
        }
    }

    static jobject header(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            auto* request = context->request();
            JavaClass kMap{ env, "java/util/LinkedHashMap" };
            JavaMethod cMap{ env, "java/util/LinkedHashMap", "<init>", "(I)V" };
            JavaMethod mPut{ env, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;" };
            jobject map = env->NewObject(kMap.get(), cMap.get(), (*request)->header().fields_count());
            std::for_each(
                (*request)->header().begin(),
                (*request)->header().end(),
                [&](const restinio::http_header_field_t& f) {
                    jstring j_name = env->NewStringUTF(f.name().c_str());
                    jstring j_value = env->NewStringUTF(f.value().c_str());
                    env->CallObjectMethod(map, mPut.get(), j_name, j_value);
                });
            return map;
        } else {
            return nullptr;
        }
    }

    static jstring target(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            auto request = context->request();
            auto target = restinio::cast_to<std::string>((*request)->header().request_target());
            return env->NewStringUTF(target.c_str());
        } else {
            return nullptr;
        }
    }

    static jstring paramByName(JNIEnv* env, jobject that, jlong contextId, jstring name)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            String param{ env, name };
            jstring value = context->param(env, param.c_str());
            return value;
        } else {
            return nullptr;
        }
    }

    static jstring getParamByIndex(JNIEnv* env, jobject that, jlong contextId, jint index)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            jstring value = context->param(env, index);
            return value;
        } else {
            return nullptr;
        }
    }

    static jint namedParamSize(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            return context->namedParamSize();
        } else {
            return -1;
        }
    }

    static jint indexedParamSize(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            return context->indexedParamSize();
        } else {
            return -1;
        }
    }

    static jstring body(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            auto* request = context->request();
            return env->NewStringUTF((*request)->body().c_str());
        } else {
            return nullptr;
        }
    }

    static jlong length(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            auto* request = context->request();
            return (jlong)(*request)->body().length();
        } else {
            return -1;
        }
    }

    static jbyteArray bodyAsByteArray(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            auto* request = context->request();
            size_t len = (*request)->body().length();
            if (len < INT_MAX) {
                jbyteArray body = env->NewByteArray((jsize)len);
                env->SetByteArrayRegion(body, 0, (jsize)len, (jbyte*)(*request)->body().c_str());
                return body;
            } else {
                JavaClass exceptionClass{ env, "io/webfolder/dakota/DakotaException" };
                env->ThrowNew(exceptionClass.get(),
                    fmt::format("Request body is too big to fit byte array. Request size: {}", len).c_str());
                return nullptr;
            }
        } else {
            return nullptr;
        }
    }

    static void bodyAsByteBuffer(JNIEnv* env, jobject that, jlong contextId, jobject buffer)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            auto* request = context->request();
            jlong len = env->GetDirectBufferCapacity(buffer) + 1;
            char* dest = (char*)env->GetDirectBufferAddress(buffer);
#ifdef _WIN32
            strcpy_s(dest, (size_t)len, (*request)->body().c_str());
#else
            strncpy(dest, (*request)->body().c_str(), (size_t)len);
#endif
        }
    }

    static jlong connectionId(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            return (*context->request())->connection_id();
        } else {
            return -1;
        }
    }

    static void setBody(JNIEnv* env, jobject that, jlong contextId, jstring body)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            String str{ env, body };
            context->response()->set_body(str.c_str());
        }
    }

    static void setBodyByteBuffer(JNIEnv* env, jobject that, jlong contextId, jobject body)
    {
        Context* context = nullptr;
        if (body && connections.find(contextId, context)) {
            std::size_t len = (std::size_t)env->GetDirectBufferCapacity(body);
            const char* buffer = (const char*)env->GetDirectBufferAddress(body);
            context->response()->set_body(restinio::const_buffer(buffer, len));
        }
    }

    static void setBodyByteArray(JNIEnv* env, jobject that, jlong contextId, jbyteArray body)
    {
        Context* context = nullptr;
        if (body && connections.find(contextId, context)) {
            jboolean isCopy;
            jbyte* buffer = env->GetByteArrayElements(body, &isCopy);
            if (buffer) {
                jsize size = env->GetArrayLength(body);
                context->setResponseBuffer(buffer);
                context->setResponseBody((jbyteArray) env->NewGlobalRef(body));
                context->response()->set_body(restinio::const_buffer((const char*)context->getResponseBuffer(), size));
                if (isCopy) {
                    context->setReleaseMode(0);
                } else {
                    context->setReleaseMode(JNI_ABORT);
                }
            } else {
                JavaClass exceptionClass{ env, "io/webfolder/dakota/DakotaException" };
                env->ThrowNew(exceptionClass.get(), "Can't allocate memory.");
            }
        }
    }

    static void done(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            if (context->releaseBuffer()) {
                env->ReleaseByteArrayElements(context->getBody(), context->getResponseBuffer(), context->getReleaseMode());
                env->DeleteGlobalRef(context->getBody());
            }
            context->response()->done([contextId, context](const auto& ec) {
                connections.erase(contextId);
                delete context;
            });
        }
    }

    static void appendHeader(JNIEnv* env, jobject that, jlong contextId, jstring name, jstring value)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            String s_name{ env, name };
            String s_value{ env, value };
            context->response()->append_header(s_name.c_str(), s_value.c_str());
        }
    }

    static void closeConnection(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            context->response()->connection_close();
        }
    }

    static void keepAliveConnection(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            context->response()->connection_keep_alive();
        }
    }

    static void appendHeaderDateField(JNIEnv* env, jobject that, jlong contextId)
    {
        Context* context = nullptr;
        if (connections.find(contextId, context)) {
            context->response()->append_header_date_field();
        }
    }
};

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_EVERSION;
    }
    jvm = vm;
    Restinio restinio{ env };
    return JNI_VERSION_1_8;
}
