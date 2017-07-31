package uk.ac.kent.eda.jb956.sensorlibrary.data;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */
public class SensorData {
    public long timestamp;

    public ActivityData toActivityData(){
        return (ActivityData)this;
    }

    public LightSensorData toLightSensorData(){
        return (LightSensorData)this;
    }

    public PositionsData toPositionsData(){
        return (PositionsData)this;
    }

    public PressureSensorData toPressureSensorData(){
        return (PressureSensorData)this;
    }

    public TemeratureSensorData toTemeratureSensorData(){
        return (TemeratureSensorData)this;
    }

    public WifiData toWifiData(){
        return (WifiData)this;
    }

    public XYZSensorData toXYZSensorData(){
        return (XYZSensorData)this;
    }
}
