package uk.ac.kent.eda.jb956.sensorlibrary.data;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */
public class TemperatureSensorData extends SensorData {
    public float degreesC;

    public TemperatureSensorData(int sensorType) {
        super(sensorType);
    }
}
