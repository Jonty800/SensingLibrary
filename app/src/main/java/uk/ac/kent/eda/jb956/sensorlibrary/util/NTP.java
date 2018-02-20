package uk.ac.kent.eda.jb956.sensorlibrary.util;

import android.os.SystemClock;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class NTP {
    private long real_time = 0L;
    private long offset = 0L;
    private boolean ahead = true;
    private long lastTimeSync = 0L;
    private int interval = 120_000;

    public static NTP getInstance() {
        if(instance == null)
            instance = new NTP();
        return instance;
    }

    private static NTP instance;

    public void setUpdateInterval(int intervalInMilliseconds){
        interval = intervalInMilliseconds;
    }

    public synchronized long currentTimeMillis(){
        SntpClient client = new SntpClient();
        if (real_time == 0L && client.requestTime("pool.ntp.org", 3000)) {
            real_time = client.getNtpTime() + SystemClock.elapsedRealtime() - client.getNtpTimeReference();
            long time = System.currentTimeMillis();
            offset = Math.abs(real_time-time);
            ahead = time > real_time;
            long test = 0;
            if(!ahead){ //if clock is behind
                //add on the missing time
                test = System.currentTimeMillis() + offset;
            }else{ //if clock is ahead
                //remove the missing time
                test = System.currentTimeMillis() - offset;
            }
            lastTimeSync = real_time;
            System.out.println("Timestamp Sync Results: Offset=" + offset + " ahead="+ahead + " actual_ts=" + real_time + " old_ts=" + time + " new_ts="+test );
        }else{
            //server failed. Dont set real_time and just return currentTimeMillis
            System.out.println("Unable to get time, returning System.currentTimeMillis()");
            return System.currentTimeMillis();
        }
        long adjustedTimestamp;
        if(!ahead){ //if clock is behind
            //add on the missing time
            adjustedTimestamp = System.currentTimeMillis() + offset;
        }else{ //if clock is ahead
            //remove the missing time
            adjustedTimestamp = System.currentTimeMillis() - offset;
        }
        if(lastTimeSync != 0L && Math.abs(lastTimeSync - adjustedTimestamp) > interval)
            real_time = 0L;
        return adjustedTimestamp;
    }
}

