package uk.ac.kent.eda.jb956.sensorlibrary.config;

import android.Manifest;
import android.os.Environment;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class Settings {
    public static final String appName = "SensingLibrary";

    /**
     * The database name (SQLite)
     */
    public static final String databaseName = appName + "DB";

    /**
     * The database version
     */
    public static final int DATABASE_VERSION = 3;

    /*
     * Sensor toggles
     */
    public static boolean ACC_ENABLED = false;
    public static boolean GYRO_ENABLED = false;
    public static boolean WIFI_ENABLED = false;
    public static boolean ACTIVITY_ENABLED = false;
    public static boolean LIGHT_ENABLED = false;
    public static boolean PROXIMITY_ENABLED = false;
    public static boolean POCKET_ENABLED = false;
    public static boolean HUMIDITY_ENABLED = false;
    public static boolean PRESSURE_ENABLED = false;
    public static boolean TEMP_ENABLED = false;
    public static boolean MAG_ENABLED = false;
    public static boolean AUDIO_ENABLED = false;
    public static boolean GPS_ENABLED = false; //TODO implement GPS

    public static final String SAVE_PATH = Environment.getExternalStorageDirectory().getPath() + "/" + appName;

    public static void setPermissionCodes(String... permissionCodes) {
        Settings.permissionCodes = permissionCodes;
    }

    public static String[] getPermissionCodes() {
        return permissionCodes;
    }

    /*
             *  Permission codes for M+
             */
    private static String[] permissionCodes = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
    };
}
