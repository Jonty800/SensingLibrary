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
    public void onSensingStarted() {
        if (sensingEventListener != null)
            sensingEventListener.onSensingStarted();
    }
    public void onSensingStopped() {
        if (sensingEventListener != null)
            sensingEventListener.onSensingStopped();
    }

    public void onSensingPaused() {
        if (sensingEventListener != null)
            sensingEventListener.onSensingPaused();
    }
    public void onSensingResumed() {
        if (sensingEventListener != null)
            sensingEventListener.onSensingResumed();
    }

    public interface SensingEventListener {
        void onDataSensed(SensorData sensorData);
        void onSensingStarted();
        void onSensingStopped();
        void onSensingPaused();
        void onSensingResumed();
    }
}
