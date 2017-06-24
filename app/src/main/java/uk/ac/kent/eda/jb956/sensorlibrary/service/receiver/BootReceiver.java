package uk.ac.kent.eda.jb956.sensorlibrary.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class BootReceiver extends BroadcastReceiver {

    private final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Object val = SensorManager.getInstance(context).getFromSharedPref("enabled", boolean.class, SensorManager.getInstance(context).getUserID());
            boolean active = true;
            if (val != null)
                active = (boolean) val;
            Log.d(TAG, "active:" + active);
            if (!active) {
                return;
            }
            SensorManager.getInstance(context).startSensingService();
        }
    }
}
