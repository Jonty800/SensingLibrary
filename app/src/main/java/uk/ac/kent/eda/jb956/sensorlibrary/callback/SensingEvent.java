package uk.ac.kent.eda.jb956.sensorlibrary.callback;

import android.util.SparseArray;

import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensingEvent {

    private final SparseArray<SensingEventListener> subscriptions = new SparseArray<>();

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

    public synchronized void onDataSensed(SensorData sensorData) {
        for (int i = 0; i < subscriptions.size(); i++) {
            if (subscriptions.get(subscriptions.keyAt(i)) != null)
                subscriptions.get(subscriptions.keyAt(i)).onDataSensed(sensorData);
        }
    }

    public synchronized void onSensingStarted(int sensorType) {
        for (int i = 0; i < subscriptions.size(); i++) {
            if (subscriptions.get(subscriptions.keyAt(i)) != null)
                subscriptions.get(subscriptions.keyAt(i)).onSensingStarted(sensorType);
        }
    }

    public synchronized void onSensingStopped(int sensorType) {
        for (int i = 0; i < subscriptions.size(); i++) {
            if (subscriptions.get(subscriptions.keyAt(i)) != null)
                subscriptions.get(subscriptions.keyAt(i)).onSensingStopped(sensorType);
        }
    }

    public synchronized void onSensingPaused(int sensorType) {
        for (int i = 0; i < subscriptions.size(); i++) {
            if (subscriptions.get(subscriptions.keyAt(i)) != null)
                subscriptions.get(subscriptions.keyAt(i)).onSensingPaused(sensorType);
        }
    }

    public synchronized void onSensingResumed(int sensorType) {
        for (int i = 0; i < subscriptions.size(); i++) {
            if (subscriptions.get(subscriptions.keyAt(i)) != null)
                subscriptions.get(subscriptions.keyAt(i)).onSensingResumed(sensorType);
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
