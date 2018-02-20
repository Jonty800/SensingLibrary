package uk.ac.kent.eda.jb956.sensorlibrary;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.security.InvalidParameterException;

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

    public DutyCyclingManager() {
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
        if(config == null){
            Log.i(TAG, "Unable to start: Config has not yet been set");
            return;
        }
        if(!config.dutyCycle){
            Log.i(TAG, "Unable to start: Duty cycling disabled for this sensor");
            return;
        }
        if (sleepingTaskStarted)
            return;
        getWorkerThread().postDelayed(dutyCyclingTask, getAwakeWindowSize());
        sleepingTaskStarted = true;
    }

    boolean debug = true;

    private long validTaskCache = 0L;
    private long getNextExpectedTimestamp(long currentTs) throws Exception{
        if(config == null){
            throw new Exception("getNextExpectedTimestamp: Config has not yet been set");
        }
        if(config.startTimestamp <= 0)
            throw new Exception("Duty cycling config missing startTimestamp");
       // long next_timestamp = config.startTimestamp;
        long next_timestamp = validTaskCache == 0L ? config.startTimestamp : validTaskCache;
        if(debug)
            Log.i(TAG, "config ts=" + config.startTimestamp);

        String initialTaskType = null;
        String pendingCycleType = sleeping ? "wake" : "sleep";
        while(true){
            if(initialTaskType == null)
                initialTaskType = sleeping ? "sleep" : "wake";
            if(pendingCycleType.equals("sleep")) {
                next_timestamp += getSleepWindowSize();
                pendingCycleType = "wake";
                if(debug)
                    Log.i(TAG, "adding sleep=" + getSleepWindowSize());
            }else{
                next_timestamp += getAwakeWindowSize();
                pendingCycleType = "sleep";
                if(debug)
                    Log.i(TAG, "adding wake=" + getAwakeWindowSize());
            }

            if(debug)
                Log.i(TAG, "next=" + next_timestamp);

            if(debug)
                Log.i(TAG,next_timestamp+ " > " + currentTs + " && " + initialTaskType.equals(pendingCycleType) + "(" + initialTaskType + "|" +pendingCycleType+")");

            if(next_timestamp > currentTs && initialTaskType.equals(pendingCycleType)){
                validTaskCache = next_timestamp;
                if(debug)
                    Log.i(TAG, "returning");
                return next_timestamp;
            }
        }

    }

    private int getAwakeWindowSize() {
        return config.AWAKE_WINDOW_SIZE;
    }

    private int getSleepWindowSize() {
        return config.SLEEP_WINDOW_SIZE;
    }

    //private long nextTaskExpectedTimestamp = 0L;
    private Runnable dutyCyclingTask = new Runnable() {
        @Override
        public void run() {
            /*long currentTs = NTP.currentTimeMillis();
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

                    if(next_timestamp > currentTs){
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
            }*/

            long currentTs = NTP.currentTimeMillis();
            long next_timestamp = 0;
            try {
                next_timestamp = getNextExpectedTimestamp(currentTs);
            } catch (Exception e) {
                e.printStackTrace();
            }
            long delay = Math.abs(currentTs - next_timestamp);
            //boolean late = nextTaskExpectedTimestamp < currentTs;

            if (!sleeping) {
                sleep((int) delay);
                getWorkerThread().postDelayed(this, (int) delay);
            } else {
                wake((int) delay);
                getWorkerThread().postDelayed(this, (int) delay);
            }

            /*if (!sleeping) {
                int newDuration = (int) (getSleepWindowSize() + diff);
                if(late)
                    newDuration = (int) (getSleepWindowSize() - diff);
                if(very_late)
                    newDuration = getSleepWindowSize();
                Log.i(TAG,"Type=getSleepWindowSize() offset=" + diff + " late="+late + " very_late="+very_late + " actual_ts=" + currentTs + " expected_ts=" + nextTaskExpectedTimestamp + " new_ts=" + newDuration);
                sleep(newDuration);
                nextTaskExpectedTimestamp = NTP.currentTimeMillis() + newDuration;
                getWorkerThread().postDelayed(this, newDuration);
            } else {
                int newDuration = (int) (getAwakeWindowSize() + diff);
                if(late)
                    newDuration = (int) (getAwakeWindowSize() - diff);
                if(very_late)
                    newDuration = getAwakeWindowSize();
                Log.i(TAG,"Type=getAwakeWindowSize() offset=" + diff + " late="+late + " very_late="+very_late +" actual_ts=" + currentTs + " expected_ts=" + nextTaskExpectedTimestamp + " new_ts=" + newDuration);
                wake(newDuration);
                getWorkerThread().postDelayed(this, newDuration);
                nextTaskExpectedTimestamp = NTP.currentTimeMillis() + newDuration;
            }*/
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

    public synchronized void unsubscribeListener(){
        dutyCyclingEventListener = null;
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
