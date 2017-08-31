package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.util.Log;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.AudioSensorData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class AudioSensorManager {
    private static int RECORDER_SAMPLERATE = 16000;
    private static int BUFFER_SIZE = 1024;

    public AudioDispatcher getAudioDispatcher() {
        return dispatcher;
    }

    private AudioDispatcher dispatcher;

    private final String TAG = "AudioSensorManager";
    private static AudioSensorManager instance;
    boolean sensing = false;

    public static synchronized AudioSensorManager getInstance() {
        if (instance == null)
            instance = new AudioSensorManager();
        return instance;
    }

    public void startSensing() {
        if (isSensing())
            return;
        if (Settings.AUDIO_ENABLED) {

            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(RECORDER_SAMPLERATE, BUFFER_SIZE, 0);
            AudioProcessor processor = new AudioProcessor() {
                @Override
                public boolean process(final AudioEvent audioEvent) {
                    AudioSensorData sensorData = new AudioSensorData();
                    sensorData.timestamp = System.currentTimeMillis();
                    sensorData.buffer = audioEvent.getFloatBuffer();
                    sensorData.bufferSize = getBufferSize();
                    sensorData.byte_buffer = audioEvent.getByteBuffer();
                    if (sensorEvent != null)
                        sensorEvent.onDataSensed(sensorData);
                    return true;
                }

                @Override
                public void processingFinished() {
                }
            };
            dispatcher.addAudioProcessor(processor);
            sensing = true;
            new Thread(dispatcher, "Audio Dispatcher").start();
            Log.i(TAG, "Started Audio Sensing at " + getSamplingRate() + " Hz");
        }else{
            Log.i(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        }
    }

    public void stopSensing() {
        if (!isSensing())
            return;
        Log.i(TAG, "Stopped Audio Sensing");
        dispatcher.stop();
        sensing = false;
    }

    public boolean isSensing() {
        return sensing;
    }

    public void setSamplingRate(int rate) {
        RECORDER_SAMPLERATE = rate;
    }

    public void setBufferSize(int bufferSize) {
        BUFFER_SIZE = bufferSize;
    }

    public int getBufferSize() {
        return BUFFER_SIZE;
    }

    public int getSamplingRate() {
        return RECORDER_SAMPLERATE;
    }

    public void setSensingWindowDuration() {

    }

    public void setSleepingDuration() {

    }

    private SensingEvent sensorEvent = null;

    public SensingEvent getSensorEventListener() {
        if (sensorEvent == null)
            sensorEvent = new SensingEvent();
        return sensorEvent;
    }

    public void setEnabled(boolean enabled) {
        Settings.AUDIO_ENABLED = enabled;
    }
}
