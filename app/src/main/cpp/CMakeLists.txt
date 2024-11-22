# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)
project("ndklearnapplication")
set(FFMPEG_DIR ${CMAKE_SOURCE_DIR}/ffmpeg)
set(FFMPEG_LIB_DIR ${FFMPEG_DIR}/libs/${ANDROID_ABI})
message(WARNING "@@@@@@@@@@@@@@@@@@@@@@@@: ${FFMPEG_LIB_DIR}")
file(GLOB FFMPEG_LIBS "${FFMPEG_LIB_DIR}/*.a")
message(WARNING "@@@@@@@@@@@@@@@@@@@@@@@@Found precompiled libraries: ${FFMPEG_LIBS}")

include_directories(${FFMPEG_DIR}/include/)
file(GLOB src_files *.cpp *.h)
add_library(gysotools SHARED
        ${src_files})

target_link_libraries(
        gysotools
        ${FFMPEG_LIBS}
        z
        android
        log)