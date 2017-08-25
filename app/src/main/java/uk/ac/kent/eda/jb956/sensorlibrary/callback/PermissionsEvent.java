package uk.ac.kent.eda.jb956.sensorlibrary.callback;

import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class PermissionsEvent {
    private static PermissionsEvent instance = null;
    public static PermissionsEvent getInstance(){
        if(instance == null)
            instance=new PermissionsEvent();
        return instance;
    }
    private OnEventListener mOnEventListener;

    public synchronized void startListening(OnEventListener listener) {
        mOnEventListener = listener;
    }

    public synchronized void stopListening() {
        mOnEventListener = null;
    }

    public void onPermissionsAccepted() {
        if (mOnEventListener != null)
            mOnEventListener.onPermissionsAccepted();
    }

    public void onPermissionsDenied() {
        if (mOnEventListener != null)
            mOnEventListener.onPermissionsDenied();
    }

    public interface OnEventListener {
        void onPermissionsAccepted();
        void onPermissionsDenied();
    }
}
