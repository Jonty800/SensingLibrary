package uk.ac.kent.eda.jb956.sensorlibrary.callback;

import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensingEvent {
    private OnEventListener mOnEventListener;

    public synchronized void subscribeToSensor(OnEventListener listener) {
        mOnEventListener = listener;
    }

    public synchronized void unsubscribeFromSensor() {
        mOnEventListener = null;
    }

    public void onDataSensed(SensorData sensorData) {
        if (mOnEventListener != null)
            mOnEventListener.onDataSensed(sensorData);
    }
    public void onSensingStarted() {
        if (mOnEventListener != null)
            mOnEventListener.onSensingStarted();
    }
    public void onSensingStopped() {
        if (mOnEventListener != null)
            mOnEventListener.onSensingStopped();
    }

    public void onSensingPaused() {
        if (mOnEventListener != null)
            mOnEventListener.onSensingPaused();
    }
    public void onSensingResumed() {
        if (mOnEventListener != null)
            mOnEventListener.onSensingResumed();
    }

    public interface OnEventListener {
        void onDataSensed(SensorData sensorData);
        void onSensingStarted();
        void onSensingStopped();
        void onSensingPaused();
        void onSensingResumed();
    }
}
