package uk.ac.kent.eda.jb956.sensorlibrary.data;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */
public class XYZSensorData extends SensorData {
    public double X;
    public double Y;
    public double Z;

    public XYZSensorData(int sensorType) {
        super(sensorType);
    }
}
