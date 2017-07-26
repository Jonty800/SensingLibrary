package uk.ac.kent.eda.jb956.sensorlibrary;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.GPS;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private CameraView mCameraView;
    private int CAMERA_REQUEST = 1;
    private TextView tv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        tv = (TextView) findViewById(R.id.count_field);
        File dir = new File(Settings.SAVE_PATH + "/SensorLibraryCamera");
        if (dir.list() != null)
            tv.setText(dir.list().length + "");
        mCameraView = (CameraView) findViewById(R.id.camera);

        mCameraView.addCallback(new CameraView.Callback() {
            @Override
            public void onPictureTaken(CameraView cameraView, byte[] data) {
                if (!imageTaken) {
                    super.onPictureTaken(cameraView, data);
                    new SaveImageTask().execute(data);
                    Log.d(TAG, "onPictureTaken - jpeg");
                }
            }
        });

        mCameraView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                try {
                    mCameraView.takePicture();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout);
        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, "Touch anywhere to take a picture", Snackbar.LENGTH_LONG);

        snackbar.show();

        //		buttonClick.setOnLongClickListener(new OnLongClickListener(){
        //			@Override
        //			public boolean onLongClick(View arg0) {
        //				camera.autoFocus(new AutoFocusCallback(){
        //					@Override
        //					public void onAutoFocus(boolean arg0, Camera arg1) {
        //						//camera.takePicture(shutterCallback, rawCallback, jpegCallback);
        //					}
        //				});
        //				return true;
        //			}
        //		});
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.start();
        mCameraView.setFacing(CameraView.FACING_FRONT);
    }

    @Override
    protected void onPause() {
        mCameraView.stop();
        super.onPause();
    }

    /**
     * Refreshes the gallery (updates the media scan so it actually appears on a PC)
     *
     * @param file
     */
    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    boolean imageTaken = false;

    /**
     *
     */
    private void onPhotoTaken(String imagePath, long startTime) {
        this.imageTaken = true;
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        Bitmap dst = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, false);
        dst.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        showImage(dst, startTime);
    }

    Dialog builder;

    public void showImage(Bitmap bitmap, final long startTime) {
        builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (builder.getWindow() != null) {
            builder.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));
        }
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //nothing;
                imageTaken = false;
            }
        });

        ImageView imageView = new ImageView(this);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        imageView.setImageBitmap(bitmap);
        float scale = getResources().getDisplayMetrics().density;
        int dpAsPixels = (int) (16 * scale + 0.5f);
        imageView.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);
        builder.addContentView(imageView, params);
        Window window = builder.getWindow();
        if (window != null) {
            window.setLayout(
                    (int) (window.getWindowManager().getDefaultDisplay().getWidth()),
                    (int) (window.getWindowManager().getDefaultDisplay().getHeight()));
        }
        builder.show();
        long dur = Math.max(0, 1000 - (System.currentTimeMillis() - startTime));
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

        service.schedule(new Runnable() {
            @Override
            public void run() {
                imageTaken = false;
                if (builder.isShowing())
                    builder.dismiss();
                if (tv != null) {
                    final File dir = new File(Settings.SAVE_PATH + "/SensorLibraryCamera");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (dir.list() != null)
                                tv.setText(dir.list().length + "");
                        }
                    });
                }
            }
        }, dur, TimeUnit.MILLISECONDS);

    }

    private int convertDpToPx(int dp, DisplayMetrics displayMetrics) {
        float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics);
        return Math.round(pixels);
    }

    private static final String AUTHORITY = "uk.ac.kent.eda.jb956.sensorlibrary";


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (builder != null) {
            if (builder.isShowing())
                builder.dismiss();
        }
    }

    /**
     * Task to async save an image to SD
     * Uses System.currentTimeMillis()/1000 as a filename
     */
    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream;
            final long startTime = System.currentTimeMillis();

            // Write to SD Card
            try {
                File dir = new File(Settings.SAVE_PATH + "/SensorLibraryCamera");
                dir.mkdirs();
                final long timestamp = System.currentTimeMillis() / 1000;
                String fileName = String.format(Locale.ENGLISH, "%d.jpg", timestamp);

                final File outFile = new File(dir, fileName);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                } else {
                    Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc == null) {
                        loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    if (loc != null) {
                        ExifInterface exif = new ExifInterface(outFile.getAbsolutePath());
                        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPS.convert(loc.getLatitude()));
                        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPS.latitudeRef(loc.getLatitude()));
                        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPS.convert(loc.getLongitude()));
                        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPS.longitudeRef(loc.getLongitude()));
                        exif.saveAttributes();
                    }
                }

                final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

                service.schedule(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Saving custom CSV");
                        // MySQLiteHelper.getInstance(getApplicationContext()).exportAccDBtest(timestamp);
                    }
                }, 10, TimeUnit.SECONDS);


                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());
                refreshGallery(outFile);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onPhotoTaken(outFile.getAbsolutePath(), startTime);
                    }
                });

                /*Uri photoURI = FileProvider.getUriForFile(getBaseContext(),
                        AUTHORITY,
                        outFile);
                Intent cameraIntent = new Intent(getBaseContext(), CameraActivity.class);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);*/


            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }


}
