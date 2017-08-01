package uk.ac.kent.eda.jb956.sensorlibrary.callback;

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

    public void onDataSensed(SensingCallbackData sensingCallbackData) {
        if (mOnEventListener != null)
            mOnEventListener.onDataSensed(sensingCallbackData);
    }

    public interface OnEventListener {
        void onDataSensed(SensingCallbackData sensingCallbackData);
    }
}
