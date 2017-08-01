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
    public static boolean ACC_ENABLED = true;
    public static boolean GYRO_ENABLED = true;
    public static boolean WIFI_ENABLED = true;
    public static boolean ACTIVITY_ENABLED = true;
    public static boolean LIGHT_ENABLED = true;
    public static boolean PROXIMITY_ENABLED = true;
    public static boolean POCKET_ENABLED = true;
    public static boolean HUMIDITY_ENABLED = true;
    public static boolean PRESSURE_ENABLED = true;
    public static boolean TEMP_ENABLED = true;
    public static boolean MAG_ENABLED = true;
    public static boolean GPS_ENABLED = false; //TODO implement GPS

    /*
     * Database toggles and path
     */
    public static boolean SAVE_GYRO_TO_DATABASE = true;
    public static boolean SAVE_ACCELEROMETER_TO_DATABASE = true;
    public static boolean SAVE_ACTIVITY_TO_DATABASE = true;
    public static boolean SAVE_WIFI_TO_DATABASE = true;
    public static boolean SAVE_LIGHT_TO_DATABASE = true;
    public static boolean SAVE_PROXIMITY_TO_DATABASE = true;
    public static boolean SAVE_POSITIONS_TO_DATABASE = true;
    public static boolean SAVE_POCKET_TO_DATABASE = true;
    public static boolean SAVE_PRESSURE_TO_DATABASE = true;
    public static boolean SAVE_HUMIDITY_TO_DATABASE = true;
    public static boolean SAVE_TEMP_TO_DATABASE = true;
    public static boolean SAVE_MAG_TO_DATABASE = true;
    public static final String SAVE_PATH = Environment.getExternalStorageDirectory().getPath() + "/" + appName;

    /*
     *  Permission codes for M+
     */
    public static String[] permissionCodes = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };
}
