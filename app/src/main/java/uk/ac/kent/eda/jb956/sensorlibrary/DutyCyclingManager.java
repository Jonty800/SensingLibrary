package uk.ac.kent.eda.jb956.sensorlibrary;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
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

    private void sleep() {
        sleeping = true;
        try {
            onSleep();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void wake() {
        sleeping = false;
        try {
            onWake();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(){
        startDutyCycling();
    }

    public void updateSensorConfig(SensorConfig config){
        this.config = config;
    }

    public void stop(){
        getWorkerThread().removeDelayedTask(dutyCyclingTask);
        sleepingTaskStarted = false;
        sleeping = false;
    }

    private boolean sleepingTaskStarted = false;

    private void startDutyCycling() {
        if (sleepingTaskStarted)
            return;
        getWorkerThread().postDelayedTask(dutyCyclingTask, getAwakeWindowSize());
        sleepingTaskStarted = true;
    }

    private int getAwakeWindowSize(){
        return config.AWAKE_WINDOW_SIZE;
    }
    private int getSleepWindowSize(){
        return config.SLEEP_WINDOW_SIZE;
    }

    private Runnable dutyCyclingTask = new Runnable() {
        @Override
        public void run() {
            if (!sleeping) {
                sleep();
                getWorkerThread().postDelayedTask(this, getSleepWindowSize());
            } else {
                wake();
                getWorkerThread().postDelayedTask(this, getAwakeWindowSize());
            }
        }
    };

    private DutyCyclingEventListener dutyCyclingEventListener;
    private void onWake() {
        if (dutyCyclingEventListener != null)
            dutyCyclingEventListener.onWake();
    }

    public synchronized DutyCyclingManager subscribeToListener(DutyCyclingEventListener listener) {
        dutyCyclingEventListener = listener;
        return this;
    }

    private void onSleep() {
        if (dutyCyclingEventListener != null)
            dutyCyclingEventListener.onSleep();
    }

    public interface DutyCyclingEventListener {
        void onWake();
        void onSleep();
    }
}
