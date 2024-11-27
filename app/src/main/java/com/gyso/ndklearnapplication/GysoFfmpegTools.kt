package com.gyso.ndklearnapplication

class GysoFfmpegTools {
    external fun mainTest(): String
    external fun parseSPS(spsData:ByteArray, dimensions:IntArray): Int
    external fun mainStart(): String

    companion object {
        private val TAG = "GysoFfmpegTools"

        @JvmStatic
        val instance: GysoFfmpegTools by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            GysoFfmpegTools()
        }

        init {
            System.loadLibrary("gysotools")
        }
    }
}