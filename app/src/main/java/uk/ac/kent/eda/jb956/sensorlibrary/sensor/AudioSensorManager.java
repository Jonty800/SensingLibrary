package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
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

    private AudioSensorManager() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(RECORDER_SAMPLERATE, BUFFER_SIZE, 0);
        Thread mainThread = new Thread(dispatcher, "Audio Dispatcher");
        mainThread.start();
    }

    public void startSensing() {
        AudioProcessor processor = new AudioProcessor() {
            @Override
            public boolean process(final AudioEvent audioEvent) {
                AudioSensorData sensorData = new AudioSensorData();
                sensorData.timestamp = System.currentTimeMillis();
                sensorData.buffer = audioEvent.getFloatBuffer();
                sensorData.bufferSize = getBufferSize();
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
    }

    public void stopSensing() {
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

    public void setDutyCyclingIntervalPattern(int... args) {

    }

    private SensingEvent sensorEvent = null;

    public SensingEvent getSensorEventListener() {
        if (sensorEvent == null)
            sensorEvent = new SensingEvent();
        return sensorEvent;
    }
}
