package uk.ac.kent.eda.jb956.sensorlibrary;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.control.WorkerThread;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.util.NTP;

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
        if(getAwakeWindowSize() != -1 && getSleepWindowSize() != -1) {
            getWorkerThread().postDelayed(dutyCyclingTask, getAwakeWindowSize());
            sleepingTaskStarted = true;
        }
    }

    private int getAwakeWindowSize() {
        return config.AWAKE_WINDOW_SIZE;
    }

    private int getSleepWindowSize() {
        return config.SLEEP_WINDOW_SIZE;
    }

    private long nextTaskExpectedTimestamp = 0L;
    private Runnable dutyCyclingTask = new Runnable() {
        @Override
        public void run() {
            long currentTs = NTP.currentTimeMillis();
            long diff = Math.abs(currentTs-nextTaskExpectedTimestamp);
            if(nextTaskExpectedTimestamp == 0L)
                diff = 0;

            long next_timestamp = nextTaskExpectedTimestamp;
            String nextCycleType = sleeping ? "wake" : "sleep";
            boolean very_late = false;
            if(nextTaskExpectedTimestamp > 0 && currentTs > nextTaskExpectedTimestamp + (sleeping ? getSleepWindowSize() : getAwakeWindowSize())){
                //if the delay was so long that it has missed a task
                //find the next task
                very_late = true;
                while(true){
                    if(nextCycleType.equals("sleep")) {
                        next_timestamp += getSleepWindowSize();
                        nextCycleType = "wake";
                    }else{
                        next_timestamp += getAwakeWindowSize();
                        nextCycleType = "sleep";
                    }

                    if(next_timestamp < currentTs){
                        if(sleeping && nextCycleType.equals("wake")){
                            nextTaskExpectedTimestamp = next_timestamp;
                            diff = Math.abs(currentTs-nextTaskExpectedTimestamp);
                            break;
                        }
                        if(!sleeping && nextCycleType.equals("sleep")){
                            nextTaskExpectedTimestamp = next_timestamp;
                            diff = Math.abs(currentTs-nextTaskExpectedTimestamp);
                            break;
                        }
                    }
                }
            }

            boolean ahead = nextTaskExpectedTimestamp < currentTs;
            if (!sleeping) {
                int newDuration = (int) (getSleepWindowSize() + diff);
                if(ahead)
                    newDuration = (int) (getSleepWindowSize() - diff);
                if(very_late)
                    newDuration = getSleepWindowSize();
                Log.i(TAG,"Type=getSleepWindowSize() offset=" + diff + " ahead="+ahead + " very_late="+very_late + " actual_ts=" + currentTs + " expected_ts=" + nextTaskExpectedTimestamp + " new_ts=" + newDuration);
                sleep(newDuration);
                nextTaskExpectedTimestamp = NTP.currentTimeMillis() + newDuration;
                getWorkerThread().postDelayed(this, newDuration);
            } else {
                int newDuration = (int) (getAwakeWindowSize() + diff);
                if(ahead)
                    newDuration = (int) (getAwakeWindowSize() - diff);
                if(very_late)
                    newDuration = getAwakeWindowSize();
                Log.i(TAG,"Type=getAwakeWindowSize() offset=" + diff + " ahead="+ahead + " very_late="+very_late +" actual_ts=" + currentTs + " expected_ts=" + nextTaskExpectedTimestamp + " new_ts=" + newDuration);
                wake(newDuration);
                nextTaskExpectedTimestamp = NTP.currentTimeMillis() + newDuration;
                getWorkerThread().postDelayed(this, newDuration);
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
