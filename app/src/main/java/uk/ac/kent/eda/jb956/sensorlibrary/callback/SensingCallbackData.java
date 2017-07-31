package uk.ac.kent.eda.jb956.sensorlibrary.callback;

import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensingCallbackData {
    private SensorData sensorData;
    private long timestamp;

    public SensingCallbackData(SensorData sensorData, long timestamp){
        this.sensorData = sensorData;
        this.timestamp = timestamp;
    }
}
