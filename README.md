# README #

### How do I get set up? ###

        <service android:name="uk.ac.kent.eda.jb956.sensorlibrary.service.ActivityRecognizedService" />
        <service
            android:name="uk.ac.kent.eda.jb956.sensorlibrary.service.WifiService"
            android:exported="false" />
        <service
            android:name="uk.ac.kent.eda.jb956.sensorlibrary.service.SensingService"
            android:exported="false" />
        <service
            android:name="uk.ac.kent.eda.jb956.sensorlibrary.service.RecordingService"
            android:exported="false" />

### Example Sensor Subscription ###

    AccelerometerManager.getInstance(this).getSensorEventListener().setOnEventListener(new SensingEvent.OnEventListener() {
            @Override
            public void onEvent(SensorEvent sensorEvent) {
                System.out.println(sensorEvent.values[0]);
            }
        });

### Who do I talk to? ###

* Repo owner or admin
* Other community or team contact