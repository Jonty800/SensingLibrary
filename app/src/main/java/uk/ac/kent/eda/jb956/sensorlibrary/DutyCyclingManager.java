package uk.ac.kent.eda.jb956.sensorlibrary;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.control.WorkerThread;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.util.NTC;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class DutyCyclingManager {

    public static String TAG = "DutyCyclingManager";

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

    private long nextTaskExpectedTimstamp = 0L;
    private Runnable dutyCyclingTask = new Runnable() {
        @Override
        public void run() {
            long currentTs = NTC.currentTimeMillis();
            long diff = Math.abs(currentTs-nextTaskExpectedTimstamp);
            boolean ahead;
            if(nextTaskExpectedTimstamp == 0L)
                diff = 0;
            ahead = nextTaskExpectedTimstamp > currentTs;

            if (!sleeping) {
                int newDuration;
                if(!ahead){ //if clock is behind
                    newDuration = (int) (getSleepWindowSize() + diff);
                }else{ //if clock is ahead
                    newDuration = (int) (getSleepWindowSize() - diff);
                }
                Log.i(TAG,"Type=getSleepWindowSize() offset=" + diff + " ahead=" + ahead + " actual_ts=" + currentTs + " old_ts=" + nextTaskExpectedTimstamp + " new_ts=" + newDuration);
                sleep(newDuration);
                nextTaskExpectedTimstamp = NTC.currentTimeMillis() + newDuration;
                getWorkerThread().postDelayed(this, newDuration);
            } else {
                int newDuration;
                if(!ahead){ //if clock is behind
                    newDuration = (int) (getAwakeWindowSize() + diff);
                }else{ //if clock is ahead
                    newDuration = (int) (getAwakeWindowSize() - diff);
                }
                Log.i(TAG,"Type=getAwakeWindowSize() offset=" + diff + " ahead=" + ahead + " actual_ts=" + currentTs + " old_ts=" + nextTaskExpectedTimstamp + " new_ts=" + newDuration);
                wake(newDuration);
                getWorkerThread().postDelayed(this, newDuration);
                nextTaskExpectedTimstamp = NTC.currentTimeMillis() + newDuration;
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
