package uk.ac.kent.eda.jb956.sensorlibrary.callback;

import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensingEvent {
    private SensingEventListener sensingEventListener;

    public synchronized void subscribeToSensor(SensingEventListener listener) {
        sensingEventListener = listener;
    }

    public synchronized void unsubscribeFromSensor() {
        sensingEventListener = null;
    }

    public void onDataSensed(SensorData sensorData) {
        if (sensingEventListener != null)
            sensingEventListener.onDataSensed(sensorData);
    }
    public void onSensingStarted(int sensorType) {
        if (sensingEventListener != null)
            sensingEventListener.onSensingStarted(sensorType);
    }
    public void onSensingStopped(int sensorType) {
        if (sensingEventListener != null)
            sensingEventListener.onSensingStopped(sensorType);
    }

    public void onSensingPaused(int sensorType) {
        if (sensingEventListener != null)
            sensingEventListener.onSensingPaused(sensorType);
    }
    public void onSensingResumed(int sensorType) {
        if (sensingEventListener != null)
            sensingEventListener.onSensingResumed(sensorType);
    }

    public interface SensingEventListener {
        void onDataSensed(SensorData sensorData);
        void onSensingStarted(int sensorType);
        void onSensingStopped(int sensorType);
        void onSensingPaused(int sensorType);
        void onSensingResumed(int sensorType);
    }
}
