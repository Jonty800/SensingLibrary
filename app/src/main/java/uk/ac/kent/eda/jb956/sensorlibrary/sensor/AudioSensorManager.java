package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;
import android.util.Log;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import uk.ac.kent.eda.jb956.sensorlibrary.DutyCyclingManager;
import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.data.AudioSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.util.SensorUtils;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class AudioSensorManager extends BaseSensor implements DutyCyclingManager.DutyCyclingEventListener {

    public AudioSensorManager(Context context) {
        this.context = context;
        dutyCyclingManager.subscribeToListener(this);
    }

    private AudioDispatcher dispatcher;
    private final String TAG = "AudioSensorManager";
    boolean sensing = false;
    Context context;

    public AudioSensorManager startSensing() {
        if (isSensing())
            return this;
        sensing = true;
        dutyCyclingManager.run();
        addNewSensingTask();
        getSensorEvent().onSensingStarted(SensorUtils.SENSOR_TYPE_MICROPHONE);
        logInfo(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        return this;
    }

    private void addNewSensingTask() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(getSamplingRate(), getBufferSize(), 0);
        dispatcher.addAudioProcessor(audioProcessor);
        sensing = true;
        new Thread(dispatcher, "Audio Dispatcher").start();
        logInfo(TAG, "Started Audio Sensing at " + getSamplingRate() + " Hz with buffer size " + getBufferSize());
    }

    private AudioProcessor audioProcessor = new AudioProcessor() {
        @Override
        public boolean process(final AudioEvent audioEvent) {
            AudioSensorData sensorData = new AudioSensorData(SensorUtils.SENSOR_TYPE_MICROPHONE);
            sensorData.timestamp = System.currentTimeMillis();
            sensorData.buffer = audioEvent.getFloatBuffer();
            sensorData.bufferSize = getBufferSize();
            sensorData.byte_buffer = audioEvent.getByteBuffer();
            if (getSensorEvent() != null)
                getSensorEvent().onDataSensed(sensorData);
            return true;
        }

        @Override
        public void processingFinished() {
        }
    };

    public AudioSensorManager stopSensing() {
        if (!isSensing())
            return this;
        logInfo(TAG, "Stopped Audio Sensing");
        stopSensingTask();
        dutyCyclingManager.stop();
        sensing = false;
        getSensorEvent().onSensingStopped(SensorUtils.SENSOR_TYPE_MICROPHONE);
        SensorManager.getInstance(context).stopSensor(SensorUtils.SENSOR_TYPE_MICROPHONE);
        return this;
    }

    private void stopSensingTask(){
        if (!dispatcher.isStopped())
            dispatcher.stop(); //should happen in stopSensing()
    }

    private void beginSensingTask() {
        addNewSensingTask();
        getSensorEvent().onSensingResumed(SensorUtils.SENSOR_TYPE_MICROPHONE);
    }

    public boolean isSensing() {
        return sensing;
    }

    @Override
    public void setSamplingRate(int rate) {
        config.audioConfig.RECORDER_SAMPLERATE = rate;
    }

    public void setBufferSize(int bufferSize) {
        config.audioConfig.BUFFER_SIZE = bufferSize;
    }

    public int getBufferSize() {
        return config.audioConfig.BUFFER_SIZE;
    }

    @Override
    public int getSamplingRate() {
        return config.audioConfig.RECORDER_SAMPLERATE;
    }

    public void setSensingWindowDuration(int duration) {
        config.AWAKE_WINDOW_SIZE = duration;
    }

    public void setSleepingDuration(int duration) {
        config.SLEEP_WINDOW_SIZE = duration;
    }

    @Override
    public void onWake(int duration) {
        Log.i(TAG, "Resuming sensor for " + duration);
        getSensorEvent().onSensingPaused(SensorUtils.SENSOR_TYPE_MICROPHONE);
        beginSensingTask();
    }

    @Override
    public void onSleep(int duration) {
        Log.i(TAG, "Pausing sensor for " + duration);
        getSensorEvent().onSensingPaused(SensorUtils.SENSOR_TYPE_MICROPHONE);
        stopSensingTask();
    }
}
