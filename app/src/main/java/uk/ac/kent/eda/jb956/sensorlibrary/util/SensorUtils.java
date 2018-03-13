package uk.ac.kent.eda.jb956.sensorlibrary.util;

import com.google.android.gms.location.DetectedActivity;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensorUtils {
    public final static int SENSOR_TYPE_ACCELEROMETER = 5001;
    private final static int SENSOR_TYPE_LOCATION = 5002;
    public final static int SENSOR_TYPE_MICROPHONE = 5003;
    public final static int SENSOR_TYPE_PROXIMITY = 5004;
    public final static int SENSOR_TYPE_WIFI = 5005;
    public final static int SENSOR_TYPE_GYROSCOPE = 5006;
    public final static int SENSOR_TYPE_LIGHT = 5007;
    public final static int SENSOR_TYPE_AMBIENT_TEMPERATURE = 5008;
    public final static int SENSOR_TYPE_PRESSURE = 5009;
    public final static int SENSOR_TYPE_HUMIDITY = 5010;
    public final static int SENSOR_TYPE_MAGNETIC_FIELD = 5011;
    public final static int SENSOR_TYPE_ACTIVITY = 5012;

    public static String getSensorName(int sensorId) {
        switch (sensorId) {
            case SENSOR_TYPE_ACCELEROMETER:
                return "SENSOR_TYPE_ACCELEROMETER";
            case SENSOR_TYPE_LOCATION:
                return "SENSOR_TYPE_LOCATION";
            case SENSOR_TYPE_MICROPHONE:
                return "SENSOR_TYPE_MICROPHONE";
            case SENSOR_TYPE_PROXIMITY:
                return "SENSOR_TYPE_PROXIMITY";
            case SENSOR_TYPE_WIFI:
                return "SENSOR_TYPE_WIFI";
            case SENSOR_TYPE_GYROSCOPE:
                return "SENSOR_TYPE_GYROSCOPE";
            case SENSOR_TYPE_LIGHT:
                return "SENSOR_TYPE_LIGHT";
            case SENSOR_TYPE_AMBIENT_TEMPERATURE:
                return "SENSOR_TYPE_AMBIENT_TEMPERATURE";
            case SENSOR_TYPE_PRESSURE:
                return "SENSOR_TYPE_PRESSURE";
            case SENSOR_TYPE_HUMIDITY:
                return "SENSOR_TYPE_HUMIDITY";
            case SENSOR_TYPE_MAGNETIC_FIELD:
                return "SENSOR_TYPE_MAGNETIC_FIELD";
            case SENSOR_TYPE_ACTIVITY:
                return "SENSOR_TYPE_ACTIVITY";
            default:
                return "UNKNOWN";
        }
    }

    public static String getSensorShortName(int sensorId) {
        switch (sensorId) {
            case SENSOR_TYPE_ACCELEROMETER:
                return "ACCELEROMETER".toLowerCase();
            case SENSOR_TYPE_LOCATION:
                return "LOCATION".toLowerCase();
            case SENSOR_TYPE_MICROPHONE:
                return "MICROPHONE".toLowerCase();
            case SENSOR_TYPE_PROXIMITY:
                return "PROXIMITY".toLowerCase();
            case SENSOR_TYPE_WIFI:
                return "WIFI".toLowerCase();
            case SENSOR_TYPE_GYROSCOPE:
                return "GYROSCOPE".toLowerCase();
            case SENSOR_TYPE_LIGHT:
                return "LIGHT".toLowerCase();
            case SENSOR_TYPE_AMBIENT_TEMPERATURE:
                return "TEMPERATURE".toLowerCase();
            case SENSOR_TYPE_PRESSURE:
                return "PRESSURE".toLowerCase();
            case SENSOR_TYPE_HUMIDITY:
                return "HUMIDITY".toLowerCase();
            case SENSOR_TYPE_MAGNETIC_FIELD:
                return "MAGNETIC_FIELD".toLowerCase();
            case SENSOR_TYPE_ACTIVITY:
                return "ACTIVITY".toLowerCase();
            default:
                return "UNKNOWN".toLowerCase();
        }
    }

    public String getActivityFromType(int activityType) {
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "IN_VEHICLE";
            case DetectedActivity.ON_BICYCLE:
                return "ON_BICYCLE";
            case DetectedActivity.ON_FOOT:
                return "ON_FOOT";
            case DetectedActivity.RUNNING:
                return "RUNNING";
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.TILTING:
                return "TILTING";
            case DetectedActivity.UNKNOWN:
                return "UNKNOWN";
            case DetectedActivity.WALKING:
                return "WALKING";
            default:
                return "UNKNOWN";
        }
    }
}
