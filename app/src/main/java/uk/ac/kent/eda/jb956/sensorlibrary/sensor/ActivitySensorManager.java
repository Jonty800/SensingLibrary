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

import uk.ac.kent.eda.jb956.sensorlibrary.DutyCyclingManager;
import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;
import uk.ac.kent.eda.jb956.sensorlibrary.data.SensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.WifiData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;
import uk.ac.kent.eda.jb956.sensorlibrary.service.ActivityRecognizedService;
import uk.ac.kent.eda.jb956.sensorlibrary.util.NTP;
import uk.ac.kent.eda.jb956.sensorlibrary.util.SensorUtils;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class ActivitySensorManager extends BaseSensor implements SensingInterface, DutyCyclingManager.DutyCyclingEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final String TAG = "ActivitySensorManager";
    private final Context context;
    private GoogleApiClient mApiClient;

    public ActivitySensorManager(Context context) {
        this.context = context.getApplicationContext();
        sensor = null;
        setSamplingRate(1000);
        dutyCyclingManager.subscribeToListener(this);
        mApiClient = new GoogleApiClient.Builder(context)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private final Sensor sensor;

    @Override
    public Sensor getSensor() {
        Log.e(TAG, "getSensor() for this class is always null");
        return sensor;
    }

    @Override
    public List<SensorData> getDataFromRange(long start, long end) {
        List<SensorData> temp = new ArrayList<>();
        Cursor cur = MySQLiteHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM act where timestamp >=" + start + " and timestamp <=" + end, null);
        while (cur.moveToNext()) {
            //Which column you want to export
            WifiData sensorData = new WifiData(SensorUtils.SENSOR_TYPE_ACTIVITY);
            sensorData.timestamp = Long.parseLong(cur.getString(3));
            sensorData.bssid = cur.getString(1);
            sensorData.rssi = Double.parseDouble(cur.getString(2));
            temp.add(sensorData);
        }
        cur.close();
        return temp;
    }

    @Override
    public List<SensorData> getAllData() {
        return getDataFromRange(0L, NTP.currentTimeMillis());
    }

    @Override
    public void removeAllDataFromDatabase() {
        removeDataFromDatabaseWithLimit(-1);
    }

    @Override
    public void removeDataFromDatabaseWithLimit(int limit) {
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM act");
        else
            database.execSQL("DELETE FROM act WHERE id IN(SELECT id FROM act ORDER BY id ASC LIMIT " + limit + ")");

        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public void removeDataFromDatabaseWithRange(long start, long end) {
        String dbName = "act";
        SQLiteDatabase database = MySQLiteHelper.getInstance(context).getWritableDatabase();
        logInfo(TAG, "Database size before delete: " + MySQLiteHelper.getInstance(context).getSize());
        database.execSQL("DELETE FROM " + dbName + " where timestamp >=" + start + " and timestamp <=" + end);
        logInfo(TAG, "Database size after delete: " + MySQLiteHelper.getInstance(context).getSize());
    }

    @Override
    public ActivitySensorManager startSensing() {
        if (isSensing())
            return this;
        try {
            logInfo(TAG, "Attempting to start sensor");
            mApiClient.connect();
            //sensing = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public ActivitySensorManager stopSensing() {
        if (!isSensing())
            return this;

        try {
            dutyCyclingManager.stop();
            mApiClient.disconnect();
            getSensorEvent().onSensingStopped(SensorUtils.SENSOR_TYPE_ACTIVITY);

        } catch (Exception e) {
            e.printStackTrace();
        }
        sensing = false;
        SensorManager.getInstance(context).stopSensor(SensorUtils.SENSOR_TYPE_ACTIVITY);
        logInfo(TAG, "Sensor stopped");
        return this;
    }

    private boolean sensing = false;

    @Override
    public boolean isSensing() {
        return sensing;
    }

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
        logInfo(TAG, "Began sensing");
        dutyCyclingManager.run();
        getSensorEvent().onSensingStarted(SensorUtils.SENSOR_TYPE_ACTIVITY);
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
        logInfo(TAG, "Connection suspended");
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
        logInfo(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
        mApiClient.connect();
        sensing = false;
    }

    @Override
    public void onWake(int duration) {
        logInfo(TAG, "Resuming sensor for " + duration);
        if (sensing)
            mApiClient.connect();
    }

    @Override
    public void onSleep(int duration) {
        logInfo(TAG, "Pausing sensor for " + duration);
        if (sensing)
            mApiClient.disconnect();
    }
}
