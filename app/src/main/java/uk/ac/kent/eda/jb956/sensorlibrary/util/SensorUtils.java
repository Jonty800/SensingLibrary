package uk.ac.kent.eda.jb956.sensorlibrary.util;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensorUtils {
    public final static int SENSOR_TYPE_ACCELEROMETER = 5001;
    public final static int SENSOR_TYPE_LOCATION = 5002;
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

    public static String getSensorName(int sensorId){
        switch(sensorId){
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
}
