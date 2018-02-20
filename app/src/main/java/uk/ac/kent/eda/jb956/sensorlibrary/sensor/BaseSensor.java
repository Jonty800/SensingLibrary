package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.DutyCyclingManager;
import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class BaseSensor {

    SensorConfig config; //load default

    public BaseSensor() {

    }

    public boolean isSensing() {
        return sensing;
    }

    public boolean isSleeping() {
        return dutyCyclingManager != null && dutyCyclingManager.isSleeping();
    }

    boolean sensing = false;
    DutyCyclingManager dutyCyclingManager;

    public void setLastEntry(SensorData lastEntry) {
        this.lastEntry = lastEntry;
    }

    private SensorData lastEntry = null;

    public SensorData getLastEntry() {
        return lastEntry;
    }

    public BaseSensor withConfig(SensorConfig config) { //replace default
        updateConfig(config);
        return this;
    }

    public void updateConfig(SensorConfig config) { //replace default
        this.config = config;
        if (dutyCyclingManager == null) {
            dutyCyclingManager = new DutyCyclingManager();
            dutyCyclingManager.updateSensorConfig(config);
        }
        else
            dutyCyclingManager.updateSensorConfig(config);
    }

    public int getSamplingRateMicroseconds() {
        return config.SAMPLING_RATE * 1000;
    }

    public void setSamplingRate(int rate) {
        config.SAMPLING_RATE = rate;
    }

    public int getSamplingRate() {
        return config.SAMPLING_RATE;
    }

    public int getAwakeWindowSize() {
        return config.AWAKE_WINDOW_SIZE;
    }

    public int getSleepWindowSize() {
        return config.SLEEP_WINDOW_SIZE;
    }

    public boolean canSaveToDatabase() {
        return config.saveToDatabase;
    }

    public void logInfo(String TAG, String text) {
        if (config.logToConsole) {
            Log.i(TAG, text);
        }
    }

    public SensingEvent getSensorEvent() {
        return SensingEvent.getInstance();
    }

    public BaseSensor startSensing() {
        return this;
    }

    public BaseSensor stopSensing() {
        return this;
    }
}
