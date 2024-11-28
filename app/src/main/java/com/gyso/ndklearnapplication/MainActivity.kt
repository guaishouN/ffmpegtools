package com.gyso.ndklearnapplication

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gyso.ndklearnapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.File

class MainActivity : AppCompatActivity() {
    lateinit var permissionUtil: PermissionUtil
    val handlerThread = HandlerThread("handler_thread")
    private lateinit var binding: ActivityMainBinding
    private lateinit var gysoFfmpegTools: GysoFfmpegTools
    companion object{
        const val TAG= "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handlerThread.start()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        permissionUtil = PermissionUtil()
        permissionUtil.init(context = this, {

        }, {

        })
        if (Build.VERSION.SDK_INT >= 33) {
            if (permissionUtil.hasStoragePermission(this, PermissionUtil.TOTAL_PERMISSION_13)) {

            }
        } else {
            if (permissionUtil.hasStoragePermission(this, PermissionUtil.TOTAL_PERMISSION)) {

            }
        }
//        binding.sampleText.text = GysoFfmpegTools.instance.mainTest()
        val myHandler = Handler(handlerThread.looper) { false }
        myHandler.post{
//            AssetsVideoStreamDecoder("127.0.0.1", 8999)
//            GysoFfmpegTools("").mainStart()
            prepareVideo()
        }
    }

    private fun prepareVideo() {
        Log.d(TAG, "prepareVideo: ")
        val filePath = Environment.getExternalStorageDirectory().toString() + File.separator + "demo.mp4"
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "playVideo: file not exist")
            return
        }
        gysoFfmpegTools = GysoFfmpegTools("tcp://172.26.4.37:8999")
        //gySoPlayer = new GySoPlayer("rtmp://59.111.90.142/myapp/");
        gysoFfmpegTools.setSurfaceView(binding.surfaceview)
        gysoFfmpegTools.prepare()
        gysoFfmpegTools.setOnStatCallback(object : GysoFfmpegTools.OnStatCallback {
            override fun onPrepared() {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "准备播放完毕",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                gysoFfmpegTools.start()
            }

            override fun onError(errorCode: Int) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "播放视频出错了!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onProgress(currentPlayTime: Int) {

            }

            override fun onYuv(nv21: ByteArray?, width: Int, height: Int, dataSize:Int) {
                Log.d(TAG, "onYuv: $width $height $dataSize ${nv21!!.size}")
            }
        })
    }

}