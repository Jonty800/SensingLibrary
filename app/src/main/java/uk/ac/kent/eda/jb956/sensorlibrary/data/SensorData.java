package uk.ac.kent.eda.jb956.sensorlibrary.data;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */
public class SensorData {
    public long timestamp;

    public int getSensorType() {
        return sensorType;
    }

    private int sensorType;

    public SensorData(int sensorType) {
        this.sensorType = sensorType;
    }

    public ActivityData toActivityData() {
        return (ActivityData) this;
    }

    public LightSensorData toLightSensorData() {
        return (LightSensorData) this;
    }

    public PositionsData toPositionsData() {
        return (PositionsData) this;
    }

    public PressureSensorData toPressureSensorData() {
        return (PressureSensorData) this;
    }

    public TemperatureSensorData toTemperatureSensorData() {
        return (TemperatureSensorData) this;
    }

    public WifiData toWifiData() {
        return (WifiData) this;
    }

    public ProximitySensorData toProximityData() {
        return (ProximitySensorData) this;
    }

    public AudioSensorData toAudioData() {
        return (AudioSensorData) this;
    }

    public XYZSensorData toXYZSensorData() {
        return (XYZSensorData) this;
    }
}
