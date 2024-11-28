package com.gyso.ndklearnapplication;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;

public class AssetsVideoStreamDecoder {
    private static final String TAG = "H264TEST";
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private int width = 640;  // 视频宽度
    private int height = 360;  // 视频高度
    private MediaCodec mediaCodec;
    private Socket socket;
    private byte[] sps = null;
    private InputStream inputStream;
    private final ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(1000);
    private int decodeCount =0 ;
    public AssetsVideoStreamDecoder(String host, int port){
        loopReceive(host,port);
        new Thread(() -> {
            try {
                while (true){
                    byte[] bs = queue.take();
                    doDecode(bs);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void loopReceive(final String host, final int port){
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Log.i(TAG, "loopReceive: start!!!");
                socket = new Socket(host, port);
                inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024*1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byte[] nal = new byte[bytesRead];
                    System.arraycopy(buffer, 0, nal, 0, bytesRead);
                    queue.put(nal);
                }
            } catch (Exception e) {
                Log.e(TAG, "loopReceive: error on receive data!!!", e);
                stopDecoding();
                loopReceive(host,port);
            }
        }).start();
    }


    public void doDecode(byte[] buffer) throws Exception {
        int bytesRead = buffer.length;
        Log.i(TAG, "doDecode: len=["+bytesRead+"]"+Arrays.toString(buffer));
        if(buffer[4]==(byte)0xFF){
            ByteBuffer bf = ByteBuffer.wrap(buffer);
            bf.get(new byte[4]);//header
            bf.get();//id
            width = bf.getInt();
            height = bf.getInt();
            Log.i(TAG, "doDecode: got width="+width+"  height="+height);
        } else if(buffer[4]==0x67){//sps
            sps = new byte[bytesRead];
            System.arraycopy(buffer, 0, sps, 0, bytesRead);
            Log.i(TAG, "read sps len["+bytesRead+"]"+ Arrays.toString(sps));
        } else if (buffer[4]==0x68) {//pps
            byte[] pps = new byte[bytesRead];
            System.arraycopy(buffer, 0, pps, 0, bytesRead);
            Log.i(TAG, "read pps len["+bytesRead+"]"+ Arrays.toString(pps));
            configureDecoderWithSpsPps(sps, pps);
        } else {//0x65 || 0x41
            Log.i(TAG, "startDecoding: len["+bytesRead+"]");
            decodeH264Frame(buffer, bytesRead);
        }
    }

    private void configureDecoderWithSpsPps(byte[] sps, byte[] pps) {
        try {
            mediaCodec = MediaCodec.createDecoderByType(VIDEO_MIME_TYPE);
            MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height);
            format.setByteBuffer("csd-0", ByteBuffer.wrap(sps));  // SPS
            format.setByteBuffer("csd-1", ByteBuffer.wrap(pps));  // PPS
            mediaCodec.configure(format, null, null, 0);
            mediaCodec.start();
            Log.d(TAG, "解码器已配置 SPS 和 PPS");
        } catch (Exception e) {
            Log.e(TAG, "配置解码器失败: " + e.getMessage());
        }
    }

    private void decodeH264Frame(byte[] data, int length) {
        try {
            // 获取输入缓冲区
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                if (inputBuffer != null) {
                    inputBuffer.clear();
                    inputBuffer.put(data, 0, length);

                    // 将数据送入解码器
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, System.currentTimeMillis(), 0);
                }
            }

            // 获取解码后的输出
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                if (outputBuffer != null) {
                    byte[] yuvData = new byte[bufferInfo.size];
                    outputBuffer.get(yuvData);

                    // 此处可以将 YUV 数据交给渲染或保存
                    processYUVData(yuvData);

                    // 释放输出缓冲区
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                }
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            }
        } catch (Exception e) {
            Log.e(TAG, "解码失败: " + e.getMessage());
        }
    }

    private void processYUVData(byte[] yuvData) {
        // 在这里处理YUV数据，例如将其保存为文件或显示到屏幕上
        Log.d(TAG, "count ={"+decodeCount+"}YUV Data Length: " + yuvData.length);
        final File FILES_DIR = NdkLearnApplication.appContext.getFilesDir();
        File outputFile = new File(FILES_DIR, String.format(Locale.CHINESE, "mmframe-%02d.yuv", decodeCount));
        try {
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(yuvData);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        decodeCount++;
    }

    public void stopDecoding() {
        try {
            queue.clear();
            if(mediaCodec!=null){
                mediaCodec.stop();
                mediaCodec.release();
            }
            if (inputStream!=null){
                inputStream.close();
            }
            if(socket!=null){
                socket.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "关闭解码器或Socket失败: " + e.getMessage());
        }
    }
}

