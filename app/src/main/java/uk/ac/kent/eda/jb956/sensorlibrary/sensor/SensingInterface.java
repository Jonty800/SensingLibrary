package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.hardware.Sensor;

import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

interface SensingInterface {

    void startSensing();

    void stopSensing();

    boolean isSensing();

    Sensor getSensor();

    SensorData getLastEntry();

    void setSamplingRate(int rate);

    int getSamplingRate();

    SensingEvent getSensorEventListener();

    List<SensorData> getDataFromRange(long start, long end);

    List<SensorData> getAllData();

    void removeDataFromDatabaseWithLimit(int limit);

    void removeDataFromDatabaseWithRange(long start, long end);

    void removeAllDataFromDatabase();

}
