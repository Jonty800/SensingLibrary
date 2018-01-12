package uk.ac.kent.eda.jb956.sensorlibrary.callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensingEvent {

    private Map<Integer, SensingEventListener> subscriptions = new HashMap<>();
    public static SensingEvent getInstance() {
        if (instance == null)
            instance = new SensingEvent();
        return instance;
    }

    private static SensingEvent instance;


    public synchronized void subscribeToSensor(SensingEventListener listener, int id) {
        subscriptions.put(id, listener);
    }

    public synchronized void unsubscribeFromSensor(int id) {
        subscriptions.remove(id);
    }

    public void onDataSensed(SensorData sensorData) {
        for(SensingEventListener sensingEventListener : subscriptions.values()) {
            if (sensingEventListener != null)
                sensingEventListener.onDataSensed(sensorData);
        }
    }

    public void onSensingStarted(int sensorType) {
        for(SensingEventListener sensingEventListener : subscriptions.values()) {
            if (sensingEventListener != null)
                sensingEventListener.onSensingStarted(sensorType);
        }
    }

    public void onSensingStopped(int sensorType) {
        for(SensingEventListener sensingEventListener : subscriptions.values()) {
            if (sensingEventListener != null)
                sensingEventListener.onSensingStopped(sensorType);
        }
    }

    public void onSensingPaused(int sensorType) {
        for(SensingEventListener sensingEventListener : subscriptions.values()) {
            if (sensingEventListener != null)
                sensingEventListener.onSensingPaused(sensorType);
        }
    }

    public void onSensingResumed(int sensorType) {
        for(SensingEventListener sensingEventListener : subscriptions.values()) {
            if (sensingEventListener != null)
                sensingEventListener.onSensingResumed(sensorType);
        }
    }

    public interface SensingEventListener {
        void onDataSensed(SensorData sensorData);

        void onSensingStarted(int sensorType);

        void onSensingStopped(int sensorType);

        void onSensingPaused(int sensorType);

        void onSensingResumed(int sensorType);
    }
}
