package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorConfig;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class BaseSensor {

    SensorConfig config = new SensorConfig(); //load default
    BaseSensor withConfig(SensorConfig config){ //replace default
        this.config = config;
        return this;
    }
    BaseSensor withDefaultConfig(){ //load default
        this.config = new SensorConfig();
        return this;
    }

    int getSamplingRateMicroseconds(){
        return config.SAMPLING_RATE * 1000;
    }

    public void setSamplingRate(int rate) {
        config.SAMPLING_RATE = rate;
    }

    public int getSamplingRate() {
        return config.SAMPLING_RATE;
    }

    public int getAwakeWindowSize(){
        return config.AWAKE_WINDOW_SIZE;
    }

    public int getSleepWindowSize(){
        return config.SLEEP_WINDOW_SIZE;
    }
}
