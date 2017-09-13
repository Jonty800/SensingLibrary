package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class BaseSensor {

    SensorConfig config = new SensorConfig(); //load default
    public BaseSensor withConfig(SensorConfig config){ //replace default
        this.config = config;
        return this;
    }
    public BaseSensor withDefaultConfig(){ //load default
        this.config = new SensorConfig();
        return this;
    }

    public int getSamplingRateMicroseconds(){
        return config.SAMPLING_RATE * 1000;
    }

    public void setSamplingRate(int rate) {
        config.SAMPLING_RATE = rate;
    }

    public int getSamplingRate() {
        return config.SAMPLING_RATE;
    }

    public int getAwakeWindowSize(){
        return config.AWAKE_WINDOW_SIZE;
    }

    public int getSleepWindowSize(){
        return config.SLEEP_WINDOW_SIZE;
    }

    public boolean canSaveToDatabase(){
        return config.saveToDatabase;
    }

    public void setSaveToDatabase(boolean save) {
        config.saveToDatabase = save;
    }

    public void logInfo(String TAG, String text){
        if(config.logToConsole) {
            Log.i(TAG, text);
        }
    }

    public SensingEvent getSensorEvent() {
        return SensingEvent.getInstance();
    }

    public BaseSensor startSensing(){
        setEnabled(true);
        return this;
    }

    BaseSensor stopSensing(){
        setEnabled(false);
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean enabled = false;
    void setEnabled(boolean enabled){
        this.enabled = enabled;
    }
}
