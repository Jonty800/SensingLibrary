package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class AudioManager {
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private final String TAG = "AudioManager";
    private static AudioManager instance;

    public static synchronized AudioManager getInstance() {
        if (instance == null)
            instance = new AudioManager();
        return instance;
    }

    private AudioManager() {

    }

    private final int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private final int BytesPerElement = 2; // 2 bytes in 16bit format

    private String fileName = null;

    public void startRecording() {
        if (isRecording) {
            Log.i(TAG, "Already recording");
            return;
        }
        Log.i(TAG, "Start recording");

        fileName = Settings.SAVE_PATH + "/" + System.currentTimeMillis() / 1000;
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte

        String filePath = fileName + ".pcm";
        Log.i(TAG, "Writing audio recording to " + Settings.SAVE_PATH);
        short sData[] = new short[BufferElements2Rec];

        File sdcarddir = new File(Settings.SAVE_PATH);
        if (!sdcarddir.exists())
            sdcarddir.mkdir();

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (os != null) {
            while (isRecording) {
                // gets the voice output from microphone to byte format
                recorder.read(sData, 0, BufferElements2Rec);
                System.out.println("Short wirting to file: " + filePath);
                try {
                    // // writes the data to file from buffer
                    // // stores the voice buffer
                    byte bData[] = short2byte(sData);
                    os.write(bData, 0, BufferElements2Rec * BytesPerElement);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Error: FileOutputStream is null");
        }
    }

    public void stopRecording() {
        if (!isRecording) {
            Log.i(TAG, "Not currently recording");
            return;
        }
        Log.i(TAG, "Stopping recording");
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            fileName = null;
        }
    }
}
