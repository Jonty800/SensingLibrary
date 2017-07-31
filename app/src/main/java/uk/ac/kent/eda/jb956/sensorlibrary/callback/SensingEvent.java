package uk.ac.kent.eda.jb956.sensorlibrary.callback;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensingEvent {
    private OnEventListener mOnEventListener;

    public void setOnEventListener(OnEventListener listener) {
        mOnEventListener = listener;
    }

    public void doEvent(SensingCallbackData sensingCallbackData) {
        if (mOnEventListener != null)
            mOnEventListener.onEvent(sensingCallbackData);
    }

    public interface OnEventListener {
        void onEvent(SensingCallbackData sensingCallbackData);
    }
}
