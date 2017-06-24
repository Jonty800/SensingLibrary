package uk.ac.kent.eda.jb956.sensorlibrary.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import uk.ac.kent.eda.jb956.sensorlibrary.SensorManager;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class SensingService extends Service {

    String TAG = "SensingService";

    static String INDOOR_STRING = "UNKNOWN";

    public static String getIndoorString() {
        return INDOOR_STRING;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    LocationManager locationManager;
    private final ScheduledThreadPoolExecutor executor_ =
            new ScheduledThreadPoolExecutor(1);
    static Location mLastLocation = null;

    @Override
    public void onCreate() {
        super.onCreate();
        SensorManager.getInstance(getApplicationContext()).startAllSensors();
        /*locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        long minTimeMillis = 5000;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMillis, 0, new MyLocationListenerGPS());*/
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class MyLocationListenerGPS implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Log.i(TAG,"onLocationChanged");
            mLastLocation = location;
            if (mLastLocation != null) {
                boolean isGPS = (mLastLocation.getAccuracy() <= 30);
                INDOOR_STRING = isGPS ? "OUTSIDE" : "INDOORS";
                Log.i(TAG, INDOOR_STRING);
                Toast.makeText(SensingService.this, INDOOR_STRING, Toast.LENGTH_SHORT).show();
            }


        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

}
