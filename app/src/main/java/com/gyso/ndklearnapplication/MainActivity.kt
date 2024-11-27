package com.gyso.ndklearnapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.widget.TextView
import com.gyso.ndklearnapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {
    val ioScope = CoroutineScope(Dispatchers.IO + Job())
    val handlerThread = HandlerThread("handler_thread")
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handlerThread.start()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.sampleText.text = GysoFfmpegTools.instance.mainTest()
        val myHandler = Handler(handlerThread.looper) { false }
        myHandler.post{
            val assetsVideoStreamer =
                AssetsVideoStreamPusher(
                    applicationContext,
                    "test.mp4",
                    10002
                )
            assetsVideoStreamer.startServer()
        }
    }
}