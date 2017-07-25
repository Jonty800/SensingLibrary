package uk.ac.kent.eda.jb956.sensorlibrary.data;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */
public enum InPocketContext {
    UNKNOWN(-1),
    IN_BAG(0),
    IN_POCKET(1),
    OUTSIDE_POCKET_BAG(2);

    private final int id;

    InPocketContext(int i){
        id = i;
    }

    public int getId() { return id; }
}
