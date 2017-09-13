package uk.ac.kent.eda.jb956.sensorlibrary.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;

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
            boolean ok = true;
            for (String permission : Settings.getPermissionCodes()) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                //TODO update this code and uncomment
                //SensorManager.getInstance(context).startSensingService();
            } else {
                Log.i(TAG, "Failed to start sensors: Permissions are missing!");
            }
        }
    }
}
