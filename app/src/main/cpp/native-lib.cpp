#include <jni.h>
#include <string>
#include "ffTool.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <stdint.h>
#include <libavutil/log.h>
#include <libavutil/error.h>
#include <libavutil/mem.h>
#include <libavutil/pixdesc.h>
#include <cstdint>
#include <cstring>
}
#define LOG_TAG "NDKLearnApp"
#include "GysoTools.h"
#define FF_INPUT_BUFFER_PADDING_SIZE 32

GysoTools *ff = new GysoTools();

int parse_sps(uint8_t *sps_data, size_t sps_size, int *width, int *height) {
    LOGD("parse_sps %d", 11);
    if (!sps_data || sps_size == 0 || !width || !height) {
        LOGDD("Invalid input parameters");
        return -1;
    }
    const AVCodec * codec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (codec != NULL)
    {
        AVCodecContext* ctx = avcodec_alloc_context3(codec);
        int got_frame = 0;
        AVPacket pkt;
        av_new_packet(&pkt, 200);
        pkt.data = sps_data;
        pkt.size = sizeof(sps_data);
        AVFrame* frame = av_frame_alloc();
//        avcodec_(ctx, frame, &got_frame, &pkt);
        LOGD("parse_sps final result size: %dx%d\n", ctx->width, ctx->height);
        avcodec_free_context(&ctx);
        av_free(ctx);
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_gyso_ndklearnapplication_GysoFfmpegTools_parseSPS(JNIEnv *env, jobject thiz,
                                                           jbyteArray sps_data,
                                                           jintArray dimensions) {
    LOGD("Java_com_gyso_ndklearnapplication_GysoFfmpegTools_parseSPS 1 %s", "");
    jsize sps_size = env->GetArrayLength(sps_data);
    jbyte *sps_data_ptr = env->GetByteArrayElements(sps_data, nullptr);
    jsize dim_size = env->GetArrayLength(dimensions);
    if (dim_size < 2) {
        env->ReleaseByteArrayElements(sps_data, sps_data_ptr, 0);
        return -1;
    }
    LOGD("Java_com_gyso_ndklearnapplication_GysoFfmpegTools_parseSPS 2 %d", dim_size);
    jint *dim_ptr = env->GetIntArrayElements(dimensions, nullptr);
    int width = 0, height = 0;
    int result = parse_sps(reinterpret_cast<uint8_t *>(sps_data_ptr), sps_size, &width, &height);
    dim_ptr[0] = width;
    dim_ptr[1] = height;
    env->ReleaseByteArrayElements(sps_data, sps_data_ptr, 0);
    env->ReleaseIntArrayElements(dimensions, dim_ptr, 0);
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_gyso_ndklearnapplication_GysoFfmpegTools_mainTest(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from C++ main test";
    int rs = ff->sum(3);
    const char *avInfo = av_version_info();
    hello += ", av_version_info: ";
    hello += avInfo;
    hello += ", sum result: ";
    hello += std::to_string(rs);
    LOGD("Generated string: %s", hello.c_str());
    return env->NewStringUTF(hello.c_str());
}