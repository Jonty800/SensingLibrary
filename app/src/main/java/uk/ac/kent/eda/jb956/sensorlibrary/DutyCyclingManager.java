package uk.ac.kent.eda.jb956.sensorlibrary;

import android.os.Handler;
import android.os.HandlerThread;

import uk.ac.kent.eda.jb956.sensorlibrary.control.WorkerThread;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class DutyCyclingManager {

    public boolean isSleeping() {
        return sleeping;
    }

    private WorkerThread getWorkerThread() {
        return workerThread;
    }

    private WorkerThread workerThread;

    private HandlerThread mSensorThread;
    private Handler mSensorHandler;

    private Handler getmSensorHandler() {
        return mSensorHandler;
    }

    private boolean sleeping = false;
    private SensorConfig config;

    public DutyCyclingManager(SensorConfig config) {
        this.config = config;
        mSensorThread = new HandlerThread("DutyCycling thread", Thread.NORM_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick
        workerThread = WorkerThread.create();
    }

    private void sleep(int duration) {
        sleeping = true;
        try {
            onSleep(duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void wake(int duration) {
        sleeping = false;
        try {
            onWake(duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        startDutyCycling();
    }

    public void updateSensorConfig(SensorConfig config) {
        this.config = config;
    }

    public void stop() {
        getWorkerThread().removeTask(dutyCyclingTask);
        sleepingTaskStarted = false;
        sleeping = false;
    }

    private boolean sleepingTaskStarted = false;

    private void startDutyCycling() {
        if (sleepingTaskStarted)
            return;
        getWorkerThread().postDelayed(dutyCyclingTask, getAwakeWindowSize());
        sleepingTaskStarted = true;
    }

    private int getAwakeWindowSize() {
        return config.AWAKE_WINDOW_SIZE;
    }

    private int getSleepWindowSize() {
        return config.SLEEP_WINDOW_SIZE;
    }

    private Runnable dutyCyclingTask = new Runnable() {
        @Override
        public void run() {
            if (!sleeping) {
                sleep(getSleepWindowSize());
                getWorkerThread().postDelayed(this, getSleepWindowSize());
            } else {
                wake(getAwakeWindowSize());
                getWorkerThread().postDelayed(this, getAwakeWindowSize());
            }
        }
    };

    private DutyCyclingEventListener dutyCyclingEventListener;

    private void onWake(int duration) {
        if (dutyCyclingEventListener != null)
            dutyCyclingEventListener.onWake(duration);
    }

    public synchronized DutyCyclingManager subscribeToListener(DutyCyclingEventListener listener) {
        dutyCyclingEventListener = listener;
        return this;
    }

    private void onSleep(int duration) {
        if (dutyCyclingEventListener != null)
            dutyCyclingEventListener.onSleep(duration);
    }

    public interface DutyCyclingEventListener {
        void onWake(int duration);

        void onSleep(int duration);
    }
}
