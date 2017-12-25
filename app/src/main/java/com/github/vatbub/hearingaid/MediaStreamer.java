package com.github.vatbub.hearingaid;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

public class MediaStreamer extends Thread {
    private boolean streaming;
    private int sampleRate;

    public boolean isStreaming() {
        return streaming;
    }

    public static MediaStreamer startStreaming(){
        return startStreaming(11025);
    }

    public static MediaStreamer startStreaming(int sampleRate){
        MediaStreamer res = new MediaStreamer(sampleRate);
        res.start();
        return res;
    }

    private MediaStreamer(int sampleRate){
        this.sampleRate = sampleRate;
    }

    public void stopStreaming(){
        streaming = false;
    }

    public void run() {
        streaming = true;
        android.os.Process.setThreadPriority
                (android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        int actualBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_8BIT);


        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                MediaRecorder.AudioEncoder.AMR_NB,
                actualBufferSize);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                MediaRecorder.AudioEncoder.AMR_NB,
                actualBufferSize,
                AudioTrack.MODE_STREAM);

        Log.d("AUDIO", "sample rate = : " + audioRecord.getSampleRate());
        Log.d("AUDIO", "buffer size = : " + audioRecord.getSampleRate());

        audioTrack.setPlaybackRate(11025);

        byte[] buffer = new byte[actualBufferSize];
        audioRecord.startRecording();
        audioTrack.play();

        while (isStreaming()) {
            audioRecord.read(buffer, 0, actualBufferSize);
            audioTrack.write(buffer, 0, buffer.length);
        }
    }
}