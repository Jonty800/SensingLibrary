package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.content.Context;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.data.AudioSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.util.SensorUtils;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class AudioSensorManager extends BaseSensor {

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

    public AudioSensorManager startSensing() {
        if (isSensing())
            return this;
        super.startSensing();
        startSleepingTask();
        addNewSensingTask();
        sensing = true;
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

    private boolean sleepingTaskStarted = false;

    private void startSleepingTask() {
        if (sleepingTaskStarted)
            return;
        SensorManager.getInstance(context).getWorkerThread().postDelayedTask(sleepTask, getAwakeWindowSize());
        sleepingTaskStarted = true;
    }

    Runnable currentTask = null;
    private Runnable sleepTask = new Runnable() {
        @Override
        public void run() {
            if (!sleeping) {
                currentTask = sleepTask;
                logInfo(TAG, "Sleeping for " + getSleepWindowSize());
                sleep();
                SensorManager.getInstance(context).getWorkerThread().postDelayedTask(currentTask, getSleepWindowSize());
            } else {
                currentTask = sleepTask;
                logInfo(TAG, "Sensing for " + getAwakeWindowSize());
                wake();
                SensorManager.getInstance(context).getWorkerThread().postDelayedTask(currentTask, getAwakeWindowSize());
            }
        }
    };

    boolean sleeping = false;

    public AudioSensorManager stopSensing() {
        if (!isSensing())
            return this;
        logInfo(TAG, "Stopped Audio Sensing");
        dispatcher.stop();
        sensing = false;
        stopSensingTask();
        getSensorEvent().onSensingStopped(SensorUtils.SENSOR_TYPE_MICROPHONE);
        sleepingTaskStarted = false;
        return this;
    }

    public void sleep() {
        sleeping = true;
        logInfo(TAG, "Pausing Audio Sensing");
        stopSensingTask();
        getSensorEvent().onSensingPaused(SensorUtils.SENSOR_TYPE_MICROPHONE);

    }

    public void wake() {
        sleeping = false;
        startSleepingTask();
        addNewSensingTask();
        getSensorEvent().onSensingResumed(SensorUtils.SENSOR_TYPE_MICROPHONE);
    }

    private void stopSensingTask() {
        if (currentTask != null) {
            SensorManager.getInstance(context).getWorkerThread().removeDelayedTask(currentTask);
            if (!dispatcher.isStopped())
                dispatcher.stop(); //should happen in stopSensing()
        }
    }

    @Override
    public AudioSensorManager withConfig(SensorConfig config) {
        super.withConfig(config);
        return this;
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
}
