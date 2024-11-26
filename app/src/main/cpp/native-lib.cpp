#include <jni.h>
#include <string>
#include "ffTool.h"
extern "C"{
    #include <libavcodec/avcodec.h>
    #include <stdint.h>
    #include <libavutil/log.h>
    #include <libavutil/error.h>
    #include <libavutil/mem.h>
    #include <libavutil/pixdesc.h>
}
#define LOG_TAG "NDKLearnApp"
#include "FFMPEGtools.h"

FFMPEGtools * ff = new FFMPEGtools();

int parse_sps(const uint8_t *sps_data, size_t sps_size, int *width, int *height) {
    if (!sps_data || sps_size == 0 || !width || !height) {
        LOGDD("Invalid input parameters");
        return -1;
    }

    // 初始化 FFmpeg 编解码器解析器
    AVCodecParserContext *parser = av_parser_init(AV_CODEC_ID_H264);
    if (!parser) {
        LOGDD("Failed to initialize parser");
        return -1;
    }

    // 初始化编解码器上下文
    AVCodecContext *codec_ctx = avcodec_alloc_context3(NULL);
    if (!codec_ctx) {
        LOGDD("Failed to allocate codec context");
        av_parser_close(parser);
        return -1;
    }

    // 解析 SPS 数据
    const uint8_t *data = sps_data;
    int consumed = av_parser_parse2(
            parser, codec_ctx,
            NULL, NULL,        // 输出缓冲区 (我们只解析，不需要解码)
            data, sps_size,    // 输入 SPS 数据
            AV_NOPTS_VALUE, AV_NOPTS_VALUE, 0
    );

    if (consumed <= 0) {
        LOGDD("Failed to parse SPS data");
        av_parser_close(parser);
        avcodec_free_context(&codec_ctx);
        return -1;
    }

    // 提取宽度和高度
    *width = codec_ctx->width;
    *height = codec_ctx->height;
    LOGDD("Parsed width: %d, height: %d", *width, *height);

    // 释放资源
    av_parser_close(parser);
    avcodec_free_context(&codec_ctx);

    return 0;
}



extern "C"
JNIEXPORT jstring JNICALL
Java_com_gyso_ndklearnapplication_GysoFfmpegTools_mainTest(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from C++ main test";
    int rs = ff->sum(3);
    const char* avInfo = av_version_info();
    hello += ", av_version_info: ";
    hello += avInfo;
    hello += ", sum result: ";
    hello += std::to_string(rs);
    LOGD("Generated string: %s", hello.c_str());
//    parse_sps(ds)
    return env->NewStringUTF(hello.c_str());
}