package uk.ac.kent.eda.jb956.sensorlibrary.util;

import android.content.Context;

import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.data.PressureSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.TemperatureSensorData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

class Util {

    /*  Dv: Absolute humidity in grams/meter3
        m: Mass constant
        Tn: Temperature constant
        Ta: Temperature constant
        Rh: Actual relative humidity in percent (%) from phone’s sensor
        Tc: Current temperature in degrees C from phone’ sensor
        A: Pressure constant in hP
        K: Temperature constant for converting to kelvin
    */
    private static float calculateAbsoluteHumidity(float temperature, float relativeHumidity) {
        float Dv;
        float m = 17.62f;
        float Tn = 243.12f;
        float Ta = 216.7f;
        float A = 6.112f;
        float K = 273.15f;

        Dv = (float) (Ta * (relativeHumidity / 100) * A * Math.exp(m * temperature / (Tn + temperature)) / (K + temperature));

        return Dv;
    }

    public static float calculateAbsoluteHumidityFromExistingSensorData(Context context) {
        float temp;
        float relativeHumidity;
        try {
            temp = ((TemperatureSensorData) SensorManager.getInstance(context).getSensorById(SensorUtils.SENSOR_TYPE_AMBIENT_TEMPERATURE).getLastEntry()).degreesC;
            relativeHumidity = ((PressureSensorData) SensorManager.getInstance(context).getSensorById(SensorUtils.SENSOR_TYPE_AMBIENT_TEMPERATURE).getLastEntry()).pressure;
        } catch (Exception e) {
            return -1;
        }
        return calculateAbsoluteHumidity(temp, relativeHumidity);
    }

    /*  Td: Dew point temperature in degrees Celsius
        m: Mass constant
        Tn: Temperature constant
        Rh: Actual relative humidity in percent (%) from phone’s sensor
        Tc: Current temperature in degrees C from phone’ sensor
    */
    public static float calculateDewPoint(float temperature, float relativeHumidity) {
        float m = 17.62f;
        float Tn = 243.12f;
        return (float) (Tn * ((Math.log(relativeHumidity / 100) + m * temperature / (Tn + temperature)) / (m - (Math.log(relativeHumidity / 100) + m * temperature / (Tn + temperature)))));
    }
}
