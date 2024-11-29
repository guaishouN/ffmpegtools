package com.gyso.ndklearnapplication

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gyso.ndklearnapplication.databinding.ActivityMainBinding
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {
    lateinit var permissionUtil: PermissionUtil
    val handlerThread = HandlerThread("handler_thread")
    private lateinit var binding: ActivityMainBinding
    private lateinit var gysoFfmpegTools: GysoFfmpegTools
    private var vWidth = -1
    private var vHeight = -1

    companion object {
        const val TAG = "MainActivity"
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
        myHandler.post {
            prepareVideo()
        }
    }


    override fun onResume() {
        super.onResume()
//        binding.yuvNv21View.onResume()
    }

    override fun onPause() {
        super.onPause()
//        binding.yuvNv21View.onPause()
    }

    private fun prepareVideo() {
        Log.d(TAG, "prepareVideo: ")
        gysoFfmpegTools = GysoFfmpegTools()
        gysoFfmpegTools.setOnStatCallback(object : GysoFfmpegTools.OnStatCallback {
            override fun onPrepared() {
                binding.yuvNv21View
                binding.yuvNv21View.setBuffer(ByteBuffer.allocate(460800), 480, 640)
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

            override fun onYuv(nv21: ByteArray?, width: Int, height: Int, dataSize: Int) {
                Log.d(TAG, "onYuv: $width $height ${nv21!!.size}")
                if ((width > 0 && height > 0) && (vWidth != width || vHeight != height)) {
                    binding.yuvNv21View.setBuffer(ByteBuffer.allocate(dataSize), width, height)
                }
//                binding.yuvNv21View.newDataArrived(nv21)
                runOnUiThread {
                    binding.yuvNv21BitmapView.setImageBitmap(BitmapUtils.getBitmap(ByteBuffer.wrap(nv21),width,height,0 ))
                }
            }
        })
    }
}