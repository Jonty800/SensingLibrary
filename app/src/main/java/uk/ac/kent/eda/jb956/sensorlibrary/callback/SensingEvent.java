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

    public interface OnEventListener {
        void onDataSensed(SensorData sensorData);
    }
}
