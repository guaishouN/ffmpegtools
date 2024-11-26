//
// Created by uidq3640 on 2024/11/25.
//

#ifndef NDKLEARNAPPLICATION_FFMPEGTOOLS_H
#define NDKLEARNAPPLICATION_FFMPEGTOOLS_H
#define LOGDD(format, ...) av_log(NULL, AV_LOG_DEBUG, "[%s] " format "\n", "FFMPEGtools", ##__VA_ARGS__)


class FFMPEGtools {
public:
    int count = 1;

    int sum(int n);
};

#endif //NDKLEARNAPPLICATION_FFMPEGTOOLS_H
