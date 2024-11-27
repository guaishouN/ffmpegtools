#include <jni.h>
#include <string>
#include "ffTool.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <stdint.h>
#include <libavutil/log.h>
#include <libavutil/error.h>
#include <libavutil/mem.h>
#include <libavutil/pixdesc.h>
#include <cstdint>
#include <cstring>
#include <libavutil/avutil.h>
#include <libavutil/pixfmt.h>
#include <libswscale/swscale.h>
#include <stdio.h>
#include <stdlib.h>
}
#define LOG_TAG "NDKLearnApp"
#include "GysoTools.h"
#define FF_INPUT_BUFFER_PADDING_SIZE 32

GysoTools *ff = new GysoTools();

int play_h264_stream(const char *url) {
    int count = 0;
    AVFormatContext *format_ctx = NULL;
    int video_stream_index = -1;
    AVCodecContext *codec_ctx = NULL;
    AVPacket packet;
    AVFrame *frame = NULL;
    struct SwsContext *sws_ctx = NULL;
    int ret;

    // Register all formats and codecs
    avformat_network_init();

    // Open the input stream
    if (avformat_open_input(&format_ctx, url, NULL, NULL) < 0) {
        LOGD("Could not open input stream\n");
        return -1;
    }

    // Retrieve stream information
    if (avformat_find_stream_info(format_ctx, NULL) < 0) {
        LOGD("Could not find stream information\n");
        return -1;
    }

    // Find the first video stream
    for (int i = 0; i < format_ctx->nb_streams; i++) {
        if (format_ctx->streams[i]->codecpar->codec_id == AV_CODEC_ID_H264) {
            video_stream_index = i;
            break;
        }
    }

    if (video_stream_index == -1) {
        LOGD("Could not find H.264 video stream\n");
        return -1;
    }

    // Get the codec context
    codec_ctx = avcodec_alloc_context3(NULL);
    if (!codec_ctx) {
        LOGD("Could not allocate codec context\n");
        return -1;
    }

    // Copy codec parameters from the stream to the codec context
    ret = avcodec_parameters_to_context(codec_ctx, format_ctx->streams[video_stream_index]->codecpar);
    if (ret < 0) {
        LOGD("Could not copy codec parameters to context\n");
        return -1;
    }

    // Find the decoder for H.264
    const AVCodec *codec = avcodec_find_decoder(codec_ctx->codec_id);
    if (!codec) {
        LOGD("Codec not found\n");
        return -1;
    }

    // Open the decoder
    ret = avcodec_open2(codec_ctx, codec, NULL);
    if (ret < 0) {
        LOGD("Could not open codec\n");
        return -1;
    }

    // Allocate frame for storing decoded data
    frame = av_frame_alloc();
    if (!frame) {
        LOGD( "Could not allocate frame\n");
        return -1;
    }

    // Initialize packet
    av_init_packet(&packet);
    packet.data = NULL;
    packet.size = 0;

    // Read and decode video frames
    while (av_read_frame(format_ctx, &packet) >= 0) {
        if (packet.stream_index == video_stream_index) {
            ret = avcodec_send_packet(codec_ctx, &packet);
            if (ret < 0) {
                LOGD("Error sending packet to decoder\n");
                return -1;
            }

            // Receive decoded frame
            ret = avcodec_receive_frame(codec_ctx, frame);
            if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                continue;  // Need more data or end of stream
            } else if (ret < 0) {
                LOGD("Error receiving frame from decoder\n");
                return -1;
            }
            LOGD("Decoded frame with count:%d  width: %d, height: %d\n", count,frame->width, frame->height);
            LOGD("PTS: %lld, DTS: %lld", frame->pts, frame->pkt_dts);
            LOGD("Keyframe: %s", frame->key_frame ? "Yes" : "No");
            LOGD("Frame type: %c", av_get_picture_type_char(frame->pict_type));
            count++;
        }

        // Free the packet
        av_packet_unref(&packet);
    }

    // Clean up
    av_frame_free(&frame);
    avcodec_free_context(&codec_ctx);
    avformat_close_input(&format_ctx);

    return 0;
}

int parse_sps(uint8_t *sps_data, size_t sps_size, int *width, int *height) {
    LOGD("parse_sps: Start parsing SPS data %d", (int)sps_size);
    if (!sps_data || sps_size == 0 || !width || !height) {
        LOGD("Invalid input parameters");
        return -1;
    }

    const AVCodec *codec = avcodec_find_decoder(AV_CODEC_ID_H264);
    AVCodecParserContext *parser;
    if (!codec) {
        LOGD("Failed to find H.264 decoder");
        return -1;
    }
    parser = av_parser_init(codec->id);
    AVCodecContext *ctx = avcodec_alloc_context3(codec);
    if (!ctx) {
        LOGD("Failed to allocate codec context");
        return -1;
    }

    // 打开解码器
    if (avcodec_open2(ctx, codec, NULL) < 0) {
        LOGD("Failed to open codec");
        avcodec_free_context(&ctx);
        return -1;
    }

    AVPacket *pPacket = av_packet_alloc();
    if (!pPacket) {
        LOGD("Failed to allocate AVPacket");
        avcodec_free_context(&ctx);
        return -1;
    }
    pPacket->data = sps_data;
    pPacket->size = sps_size; // 使用正确的 sps_size

    // 分配 AVFrame 以接收解码后的帧
    AVFrame *frame = av_frame_alloc();
    if (!frame) {
        LOGD("Failed to allocate AVFrame");
        av_packet_free(&pPacket);
        avcodec_free_context(&ctx);
        return -1;
    }
    int ret1 = av_parser_parse2(parser, ctx, &pPacket->data, &pPacket->size,sps_data, sps_size, AV_NOPTS_VALUE, AV_NOPTS_VALUE, 0);
    // 发送数据包给解码器
    int ret = avcodec_send_packet(ctx, pPacket);
    if (ret < 0) {
        LOGD("Error sending packet to decoder");
        av_frame_free(&frame);
        av_packet_free(&pPacket);
        avcodec_free_context(&ctx);
        return -1;
    }

    // 接收解码帧
    LOGD("Waiting for decoded frames...");
    ret = avcodec_receive_frame(ctx, frame);
    if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
        LOGD("Need more data or end of stream");
        av_frame_free(&frame);
        av_packet_free(&pPacket);
        avcodec_free_context(&ctx);
        return -1;
    } else if (ret < 0) {
        LOGD("Error receiving frame from decoder");
        av_frame_free(&frame);
        av_packet_free(&pPacket);
        avcodec_free_context(&ctx);
        return -1;
    }

    // 打印解码后的帧的宽高信息
    LOGD("Decoded frame with width: %d, height: %d", frame->width, frame->height);
    LOGD(
            "Frame %c (%d) pts %d dts %d",
            av_get_picture_type_char(frame->pict_type),
            (int)(ctx->frame_num),
            (int)(frame->pts),
            (int)(frame->pkt_dts)
    );

    // 返回解析结果
    *width = frame->width;
    *height = frame->height;

    // 清理资源
    av_frame_free(&frame);
    av_packet_free(&pPacket);
    avcodec_free_context(&ctx);

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
extern "C"
JNIEXPORT jstring JNICALL
Java_com_gyso_ndklearnapplication_GysoFfmpegTools_mainStart(JNIEnv *env, jobject thiz) {
    // TODO: implement mainStart()
    const char *url = "tcp://127.0.0.1:10002";
    if (play_h264_stream(url) < 0) {
        fprintf(stderr, "Error playing H.264 stream\n");
    }
}