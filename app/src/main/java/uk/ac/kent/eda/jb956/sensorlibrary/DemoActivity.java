package uk.ac.kent.eda.jb956.sensorlibrary;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class DemoActivity extends AppCompatActivity {

    String TAG = "DemoActivity";
    private SensorManager sensorManager;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1972;
    private static final int REQUEST_SCAN_ALWAYS_AVAILABLE = 98;
    private final int PERMISSIONS_REQUEST = 99;

    /**
     * Manager for the Fingerprint API TODO: test these
     */
    //private FingerprintManager fingerprintManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        sensorManager = SensorManager.getInstance(this);

        // fingerprintManager = FingerprintManager.getInstance(this);
        checkPermissions();

      /*  Button mFingerprintSignInButton = (Button) findViewById(R.id.fingerprint_sign_in_button);
        mFingerprintSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 23) {
                    fingerprintManager.attemptFingerprintLogin();
                }
            }
        });*/
        Button cameraButton = (Button) findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(DemoActivity.this, CamRecorderActivity.class);
                startActivity(i);
            }
        });

       /* Button startButton = (Button) findViewById(R.id.start_recording_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sensorManager.audioManager.startRecording();
            }
        });

        Button stopButton = (Button) findViewById(R.id.stop_recording_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sensorManager.audioManager.stopRecording();
            }
        });*/

        Button saveButton = (Button) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("Admin");
                final EditText input = new EditText(view.getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = input.getText().toString();
                        if (m_Text.equalsIgnoreCase("save")) {
                            Log.i(TAG, "Saving all CSV");
                            MySQLiteHelper.getInstance(getApplicationContext()).exportWifiDB();
                            MySQLiteHelper.getInstance(getApplicationContext()).exportAccDB();
                            MySQLiteHelper.getInstance(getApplicationContext()).exportGyroDB();
                            MySQLiteHelper.getInstance(getApplicationContext()).exportActDB();
                            MySQLiteHelper.getInstance(getApplicationContext()).exportProximityDB();

                            MySQLiteHelper.getInstance(getApplicationContext()).exportMagDB();
                            MySQLiteHelper.getInstance(getApplicationContext()).exportPocketDB();
                            MySQLiteHelper.getInstance(getApplicationContext()).exportHumidityDB();
                            MySQLiteHelper.getInstance(getApplicationContext()).exportPressureDB();
                            MySQLiteHelper.getInstance(getApplicationContext()).exportTemperatureDB();
                        }
                        if (m_Text.equalsIgnoreCase("clear")) {
                            Log.i(TAG, "Deleting everything");
                            MySQLiteHelper.getInstance(getApplication()).clearAll();
                        }
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();


            }
        });
    }

    /**
     * Callback for when the permissions request action is called
     * Will start checkPermissions() if permissions need accepting
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermissions();
                }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        checkPlayServices();
        checkOtherServices();
    }

    /**
     * Checks if google play services needs updating
     */
    private void checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        REQUEST_GOOGLE_PLAY_SERVICES).show();
            }
        }
    }

    /**
     * Checks if there is a need for ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS (23+)
     * and also ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE (18+)
     */
    private void checkOtherServices() {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (Build.VERSION.SDK_INT >= 18 && !wifiManager.isScanAlwaysAvailable()) {
            startActivityForResult(new Intent(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE), REQUEST_SCAN_ALWAYS_AVAILABLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        switch (DemoActivity.exitCode) {
            case 0:
                break;
            case 1:
                Snackbar snack1 = Snackbar.make(findViewById(android.R.id.content),
                        "Experiment completed", Snackbar.LENGTH_LONG);
                snack1.getView().setBackgroundColor(Color.parseColor("#38BF2F"));
                snack1.show();
                break;
            case 2:
                Snackbar snack2 = Snackbar.make(findViewById(android.R.id.content),
                        "Experiment stopped unexpectedly", Snackbar.LENGTH_LONG);
                snack2.getView().setBackgroundColor(Color.parseColor("#FF8000"));
                snack2.show();
                break;
        }
        DemoActivity.exitCode = 0;
    }

    public static int exitCode = 0;

    /**
     * Checks what permissions need to be requested (M+)
     * See permissionCodes in Settings.java
     * If all permissions have been accepted, it will try to start the sensors
     */
    private void checkPermissions() {
        boolean ok = true;
        for (String permission : Settings.permissionCodes) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        Settings.permissionCodes,
                        PERMISSIONS_REQUEST);
                ok = false;
                break;
            }
        }
        if (ok) {
            // permission has been granted, continue as usual
            sensorManager.startSensingService();
        }
    }
}
