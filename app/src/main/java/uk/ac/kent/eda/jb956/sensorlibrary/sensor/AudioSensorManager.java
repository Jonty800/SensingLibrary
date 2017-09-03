package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;
import android.util.Log;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
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

    private static int SLEEP_DURATION = 20000;
    private static int AWAKE_DURATION = 10000;

    public AudioSensorManager(Context context) {
        this.context = context;
    }

    public AudioDispatcher getAudioDispatcher() {
        return dispatcher;
    }

    private AudioDispatcher dispatcher;
    private Context context;
    private final String TAG = "AudioSensorManager";
    private static AudioSensorManager instance;
    boolean sensing = false;

    public static synchronized AudioSensorManager getInstance(Context context) {
        if (instance == null)
            instance = new AudioSensorManager(context);
        return instance;
    }

    public void startSensing() {
        if (isSensing())
            return;
        if (Settings.AUDIO_ENABLED) {
            startSleepingTask();
            addNewSensingTask();
            getSensorEventListener().onSensingStarted();
        }else{
            Log.i(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
        }
    }

    private void addNewSensingTask() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(RECORDER_SAMPLERATE, BUFFER_SIZE, 0);
        dispatcher.addAudioProcessor(getAudioProcessor());
        sensing = true;
        new Thread(dispatcher, "Audio Dispatcher").start();
        Log.i(TAG, "Started Audio Sensing at " + getSamplingRate() + " Hz with buffer size " + getBufferSize());
    }

    private AudioProcessor getAudioProcessor() {
        return new AudioProcessor() {
            public boolean process ( final AudioEvent audioEvent){
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
            public void processingFinished () {
            }
        };
    }

    private boolean sleepingTaskStarted = false;
    private void startSleepingTask(){
        if(sleepingTaskStarted)
            return;
        SensorManager.getInstance(context).getWorkerThread().postDelayedTask(getSleepTask(), AWAKE_DURATION);
        sleepingTaskStarted = true;
    }

    Runnable currentTask = null;
    private Runnable getSleepTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (sensing) {
                    currentTask = getSleepTask();
                    Log.i(TAG, "Sleeping for " + SLEEP_DURATION);
                    stopSensing();
                    SensorManager.getInstance(context).getWorkerThread().postDelayedTask(currentTask, SLEEP_DURATION);
                } else {
                    currentTask = getSleepTask();
                    Log.i(TAG, "Sensing for " + AWAKE_DURATION);
                    startSensing();
                    SensorManager.getInstance(context).getWorkerThread().postDelayedTask(currentTask, AWAKE_DURATION);
                }
            }
        };
    }

    public void stopSensing() {
        if (!isSensing())
            return;
        Log.i(TAG, "Stopped Audio Sensing");
        dispatcher.stop();
        sensing = false;
        stopSensingTask();
        getSensorEventListener().onSensingStopped();
    }

    private void stopSensingTask(){
        if(currentTask!=null)
            SensorManager.getInstance(context).getWorkerThread().removeDelayedTask(currentTask);
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

    public void setSensingWindowDuration(int duration) {
        AWAKE_DURATION = duration;
    }

    public void setSleepingDuration(int duration) {
        SLEEP_DURATION = duration;
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
