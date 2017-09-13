package uk.ac.kent.eda.jb956.sensorlibrary.data;

import java.io.Serializable;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensorConfig implements Serializable{

    public int SAMPLING_RATE = 100; //ms
    public int SLEEP_WINDOW_SIZE = 20000; //ms
    public int AWAKE_WINDOW_SIZE = 30000; //ms
    public boolean saveToDatabase = true;
    public boolean logToConsole = true;

    public AudioConfig audioConfig = new AudioConfig();

    public class AudioConfig implements Serializable{
        public int BUFFER_SIZE = 2000;
        public int RECORDER_SAMPLERATE = 16000;
    }
}
