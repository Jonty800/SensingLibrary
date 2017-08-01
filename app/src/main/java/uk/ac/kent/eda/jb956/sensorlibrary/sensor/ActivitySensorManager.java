package uk.ac.kent.eda.jb956.sensorlibrary.sensor;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

import java.util.ArrayList;
import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.callback.SensingEvent;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.WifiData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;
import uk.ac.kent.eda.jb956.sensorlibrary.service.ActivityRecognizedService;
import uk.ac.kent.eda.jb956.sensorlibrary.service.WifiService;
import uk.ac.kent.eda.jb956.sensorlibrary.service.receiver.AlarmReceiver;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class ActivitySensorManager implements SensingInterface, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    private final String TAG = "ActivitySensorManager";
    private static ActivitySensorManager instance;
    private final Context context;
    public static int SAMPLING_RATE = 1000; //ms
    public static final int SAMPLING_RATE_MICRO = SAMPLING_RATE * 1000;
    private GoogleApiClient mApiClient;

    public static synchronized ActivitySensorManager getInstance(Context context) {
        if (instance == null)
            instance = new ActivitySensorManager(context);
        return instance;
    }

    private ActivitySensorManager(Context context) {
        this.context = context.getApplicationContext();
        sensor = null;
    }

    private final Sensor sensor;

    @Override
    public Sensor getSensor() {
        Log.e(TAG, "getSensor() for this class is always null");
        return sensor;
    }

    @Override
    public SensorData getLastEntry() {
        return lastEntry;
    }

    @Override
    public void setSamplingRate(int rate) {
        SAMPLING_RATE = rate;
    }

    @Override
    public int getSamplingRate() {
        return SAMPLING_RATE;
    }

    private SensingEvent sensorEvent = null;

    @Override
    public SensingEvent getSensorEventListener() {
        if (sensorEvent == null)
            sensorEvent = new SensingEvent();
        return sensorEvent;
    }

    @Override
    public List<SensorData> getDataFromRange(long start, long end) {
        List<SensorData> temp = new ArrayList<>();
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM act where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            WifiData sensorData = new WifiData();
            sensorData.timestamp = Long.parseLong(cur.getString(3));
            sensorData.bssid= cur.getString(1);
            sensorData.rssi = Double.parseDouble(cur.getString(2));
            temp.add(sensorData);
        }
        cur.close();
        return temp;
    }

    @Override
    public void setEnabled(boolean enabled){
        Settings.WIFI_ENABLED = enabled;
    }

    @Override
    public List<SensorData> getAllData() {
        return getDataFromRange(0L, System.currentTimeMillis());
    }

    @Override
    public void removeAllDataFromDatabase() {
        removeDataFromDatabaseWithLimit(-1);
    }

    @Override
    public void removeDataFromDatabaseWithLimit(int limit) {
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM act");
        else
            database.execSQL("DELETE FROM act WHERE id IN(SELECT id FROM act ORDER BY id ASC LIMIT " + limit + ")");

        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "act";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        Log.i(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        Log.i(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }
    @Override
    public void setSaveToCSV(boolean save) {
        Settings.SAVE_WIFI_TO_DATABASE = save;
    }


    @Override
    public void startSensing() {
        if (isSensing())
            return;
        try {
            if (Settings.ACTIVITY_ENABLED) {
                mApiClient = new GoogleApiClient.Builder(context)
                        .addApi(ActivityRecognition.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();

                mApiClient.connect();
                //sensing = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, !isSensing() ? TAG + " not started: Disabled" : TAG + " started");
    }

    @Override
    public void stopSensing() {
        if (!isSensing())
            return;
        try {
            if (Settings.WIFI_ENABLED) {
                mApiClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sensing = false;
        Log.i(TAG, "Sensor stopped");
    }

    private boolean sensing = false;

    @Override
    public boolean isSensing() {
        return sensing;
    }

    private SensorData lastEntry = null;

    /**
     * Event which occurs when the GoogleAPI has connected to the GooglePlayServices
     * This is where we request activity updates
     *
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = new Intent(context, ActivityRecognizedService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, getSamplingRate(), pendingIntent);
        sensing = true;
    }

    /**
     * Event which occurs when the GoogleAPI connection has been suspended
     *
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mApiClient.connect();
        // removeActivityUpdates();
    }

    /**
     * Event which occurs when the GoogleAPI has failed to connect to the GooglePlayServices
     *
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
        mApiClient.connect();
        sensing = false;
    }
}
