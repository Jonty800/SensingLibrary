package uk.ac.kent.eda.jb956.sensorlibrary.data;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */
public class PressureSensorData extends SensorData {
    public float pressure;

    public PressureSensorData(int sensorType) {
        super(sensorType);
    }
}
