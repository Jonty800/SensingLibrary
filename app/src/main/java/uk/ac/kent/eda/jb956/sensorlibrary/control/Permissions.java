package uk.ac.kent.eda.jb956.sensorlibrary.control;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import uk.ac.kent.eda.jb956.sensorlibrary.callback.PermissionsEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class Permissions extends Activity {

    private final int PERMISSIONS_REQUEST = 99;
    Activity act;
    public Permissions(Activity act) {
        this.act = act;
    }
    public void checkPermissions() {
        boolean ok = true;
        for (String permission : Settings.getPermissionCodes()) {
            if (ActivityCompat.checkSelfPermission(act, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(act,
                        Settings.getPermissionCodes(),
                        PERMISSIONS_REQUEST);
                ok = false;
                break;
            }
        }
        if (ok) {
            PermissionsEvent.getInstance().onPermissionsAccepted();
        }else{
            PermissionsEvent.getInstance().onPermissionsDenied();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermissions();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}