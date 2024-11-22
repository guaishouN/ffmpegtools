#include <jni.h>
#include <string>

extern "C"{
#include <libavcodec/avcodec.h>
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_gyso_ndklearnapplication_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    hello.c_str();
    return env->NewStringUTF(av_version_info());
}