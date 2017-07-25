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
    public static final int DATABASE_VERSION = 2;

    /*
     * Sensor toggles
     */
    public static final boolean ACC_ENABLED = true;
    public static final boolean GYRO_ENABLED = true;
    public static final boolean WIFI_ENABLED = false;
    public static final boolean ACTIVITY_ENABLED = false;
    public static final boolean LIGHT_ENABLED = true;
    public static final boolean PROXIMITY_ENABLED = true;
    public static final boolean POCKET_ENABLED = true;
    public static boolean GPS_ENABLED = false; //TODO implement GPS

    /*
     * Sensor sampling rates
     */
    public static final int ACTIVITY_SAMPLING_RATE = 3000; //ms
    public static final int WIFI_SENSING_RATE = 10000; //ms

    /*
     * Database toggles and path
     */
    public static final boolean SAVE_GYRO_TO_DATABASE = true;
    public static final boolean SAVE_ACCELEROMETER_TO_DATABASE = true;
    public static final boolean SAVE_ACTIVITY_TO_DATABASE = true;
    public static final boolean SAVE_WIFI_TO_DATABASE = true;
    public static final boolean SAVE_LIGHT_TO_DATABASE = true;
    public static final boolean SAVE_PROXIMITY_TO_DATABASE = true;
    public static final boolean SAVE_POSITIONS_TO_DATABASE = true;
    public static final boolean SAVE_POCKET_TO_DATABASE = true;
    public static final String SAVE_PATH = Environment.getExternalStorageDirectory().getPath() + "/" + appName;

    /*
     *  Permission codes for M+
     */
    public static final String[] permissionCodes = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };
}
