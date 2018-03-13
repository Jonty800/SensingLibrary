package uk.ac.kent.eda.jb956.sensorlibrary.util;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

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
    boolean fetching = false;
    private GetTimeAsyncTask timeAsyncTask;

    public final String TAG = getClass().getSimpleName();

    public static NTP getInstance() {
        if (instance == null)
            instance = new NTP();
        return instance;
    }

    private static NTP instance;

    public void setUpdateInterval(int intervalInMilliseconds) {
        interval = intervalInMilliseconds;
    }

    public synchronized long currentTimeMillis() {
        if (real_time == 0L) {
            if (!fetching) {
                fetching = true;
                if (timeAsyncTask == null)
                    timeAsyncTask = new GetTimeAsyncTask();
                timeAsyncTask.execute();
            }
        }
        long adjustedTimestamp;
        if (!ahead) { //if clock is behind
            //add on the missing time
            adjustedTimestamp = System.currentTimeMillis() + offset;
        } else { //if clock is ahead
            //remove the missing time
            adjustedTimestamp = System.currentTimeMillis() - offset;
        }
        if (lastTimeSync != 0L && Math.abs(lastTimeSync - adjustedTimestamp) > interval)
            real_time = 0L;
        return adjustedTimestamp;
    }

    private class GetTimeAsyncTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            SntpClient client = new SntpClient();
            if (client.requestTime("pool.ntp.org", 3000)) {
                real_time = client.getNtpTime() + SystemClock.elapsedRealtime() - client.getNtpTimeReference();
                long test = 0;
                if (!ahead) { //if clock is behind
                    //add on the missing time
                    test = System.currentTimeMillis() + offset;
                } else { //if clock is ahead
                    //remove the missing time
                    test = System.currentTimeMillis() - offset;
                }
                long time = System.currentTimeMillis();
                offset = Math.abs(real_time - time);
                ahead = time > real_time;
                System.out.println("Timestamp Sync Results: Offset=" + offset + " ahead=" + ahead + " actual_ts=" + real_time + " old_ts=" + time + " new_ts=" + test);
                lastTimeSync = real_time;
            } else {
                Log.e(TAG, "Unable to download time");
            }
            return null;
        }

        protected void onPostExecute(Void real_time) {
            fetching = false;
        }
    }
}

