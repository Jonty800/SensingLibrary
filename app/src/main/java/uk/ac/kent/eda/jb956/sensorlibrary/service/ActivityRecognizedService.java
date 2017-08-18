package uk.ac.kent.eda.jb956.sensorlibrary.service;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

import uk.ac.kent.eda.jb956.sensorlibrary.data.ActivityData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.ActivitySensorManager;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class ActivityRecognizedService extends IntentService {

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getProbableActivities());
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        DetectedActivity best = null;
        int accuracy = 0;
        for (DetectedActivity activity : probableActivities) {

            if (activity.getConfidence() > accuracy) {
                accuracy = activity.getConfidence();
                best = activity;
            }

            switch (activity.getType()) {
                /*case DetectedActivity.IN_VEHICLE: {
                    Log.e("ActivityRecogition", "In Vehicle - confidence: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    Log.e("ActivityRecogition", "On Bicycle - confidence: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    Log.e("ActivityRecogition", "On Foot - confidence: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.e("ActivityRecogition", "Running - confidence: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.e("ActivityRecogition", "Still - confidence: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.TILTING: {
                    Log.e("ActivityRecogition", "Tilting - confidence: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.WALKING: {
                    Log.e("ActivityRecogition", "Walking - confidence: " + activity.getConfidence());
                    /*if( activity.getConfidence() >= 75 ) {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "Are you walking?" );
                        builder.setSmallIcon( R.mipmap.ic_launcher );
                        builder.setContentTitle( getString( R.string.app_name ) );
                        NotificationManagerCompat.from(this).notify(0, builder.build());
                    }*/
                 /*   break;
                }
                case DetectedActivity.UNKNOWN: {
                    Log.e("ActivityRecogition", "Unknown - confidence: " + activity.getConfidence());
                    break;
                }*/
            }
        }
        if (best != null) {
            ActivityData sensorData = new ActivityData();
            sensorData.activityCode = best.getType();
            sensorData.confidence = best.getConfidence();
            sensorData.timestamp = System.currentTimeMillis();
            MySQLiteHelper.getInstance(this).addToActivity(sensorData);
            ActivitySensorManager.getInstance(this).getSensorEventListener().onDataSensed(sensorData);
        }
    }
}