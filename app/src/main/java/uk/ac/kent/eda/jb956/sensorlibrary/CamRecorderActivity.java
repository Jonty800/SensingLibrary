package uk.ac.kent.eda.jb956.sensorlibrary;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.PositionsData;
import uk.ac.kent.eda.jb956.sensorlibrary.database.MySQLiteHelper;
import uk.ac.kent.eda.jb956.sensorlibrary.sensor.CameraHelper;

/**
 * This activity uses the camera/camcorder as the A/V source for the {@link MediaRecorder} API.
 * A {@link TextureView} is used as the camera preview which limits the code to API 14+. This
 * can be easily replaced with a {@link SurfaceView} to run on older devices.
 */
public class CamRecorderActivity extends Activity {

    private Camera mCamera;
    private TextureView mPreview;
    private File mOutputFile;

    private boolean isRecording = false;
    private static final String TAG = "Recorder";
    private Button captureButton;
    private TextView movingTextView;
    ScheduledExecutorService service = null;
    long startTime = 0L;

    private MediaRecorder mMediaRecorder;
    private File screenFile = null;

    private static final int REQUEST_CODE = 1;

    private MediaRecorder recorder;
    private MediaProjection projection;
    private VirtualDisplay display;
    private MediaProjectionManager projectionManager;
    private static final String MIME_TYPE = "video/mp4";
    private boolean running;
    private final int durationOfTest = 60000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam_recorder);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.i(TAG, "onCreate()");
        mPreview = (TextureView) findViewById(R.id.surface_view);
        captureButton = (Button) findViewById(R.id.button_capture);
        movingTextView = (TextView) findViewById(R.id.moving_tv);
        if (mCamera == null)
            mCamera = CameraHelper.getDefaultFrontFacingCameraInstance();
        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void screenRandomAnimator(final TextView textView) {
        //movingTextView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        int tvWidth = movingTextView.getWidth() * 2;
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final Random random = new Random();
        final int xBounds = size.x - tvWidth;
        final int yBounds = size.y - ((captureButton.getHeight() * 2) + tvWidth);

        float chosenX = (float) random.nextInt(xBounds);
        float chosenY = (float) random.nextInt(yBounds);

        if (chosenX < tvWidth)
            chosenX = tvWidth;

        if (chosenY < tvWidth)
            chosenY = tvWidth;

        while (true) {
            double d = Math.sqrt(Math.pow(movingTextView.getX() - chosenX, 2) + Math.pow(movingTextView.getY() - chosenY, 2));
            if (d > xBounds / 3) {
                break;
            } else {
                chosenX = (float) random.nextInt(xBounds);
                chosenY = (float) random.nextInt(yBounds);
                if (chosenX < tvWidth)
                    chosenX = tvWidth;
                if (chosenY < tvWidth)
                    chosenY = tvWidth;
            }
        }

        final AnimatorSet mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(ObjectAnimator.ofFloat(textView, "x", textView.getX(), chosenX),
                ObjectAnimator.ofFloat(textView, "y", textView.getY(), chosenY));
        int Low = 1500;
        int High = 2500;
        int Result = random.nextInt(High - Low) + Low;
        mAnimatorSet.setDuration(Result);
        mAnimatorSet.setInterpolator(new AccelerateInterpolator(0.5f));
        // mAnimatorSet.setStartDelay(500);
        mAnimatorSet.start();
        mAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!finishAnimation) {
                    // movingTextView.setLayerType(View.LAYER_TYPE_NONE, null);
                    screenRandomAnimator(movingTextView);
                } else {
                    finishAnimation = false;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    /**
     * The capture button controls all user interaction. When recording, the button click
     * stops recording, releases {@link MediaRecorder} and {@link Camera}. When not recording,
     * it prepares the {@link MediaRecorder} and starts recording.
     *
     * @param view the view generating the event.
     */
    boolean finishAnimation = false;

    public void onCaptureClick(View view) {
        if (isRecording) {
            DemoActivity.exitCode = 2;
            stopAllStuff(true);

        } else {
            startTime = System.currentTimeMillis() / 1000;
            new MediaPrepareTask().execute(null, null, null);
        }
    }

    void stopAllStuff(boolean finish) {
        // BEGIN_INCLUDE(stop_release_media_recorder)

        // stop recording and release camera
        try {
            System.out.println("mMediaRecorder.stop()");
            finishAnimation = true;
            mMediaRecorder.stop();
            long end = System.currentTimeMillis();
            releaseMediaRecorder(); // release the MediaRecorder object
            if (stopAllRunnable != null)
                stopAllRunnable.cancel();
            if (service != null && !service.isShutdown())
                service.shutdown();
            if (running)
                stopRecordingScreen();
            //mCamera.lock();         // take camera access back from MediaRecorder

            File dir = new File(Settings.SAVE_PATH + "/" + startTime + "/SensorLibraryCamera/");
            dir.mkdirs();

            String newFileNameCamera = String.format(Locale.ENGLISH, "camera_%d.mp4", actualStartTime);
            String screenFileLocation = String.format(Locale.ENGLISH, "screen_%d.webm", startTime);
            String newFileNameScreen = String.format(Locale.ENGLISH, "screen_%d.webm", actualStartTime);
            File to = new File(dir, newFileNameCamera);
            File screenFile = new File(dir, screenFileLocation);
            if (outFile.exists())
                outFile.renameTo(to);
            refreshGallery(to);
            to = new File(dir, newFileNameScreen);
            if (screenFile.exists())
                screenFile.renameTo(to);
            refreshGallery(to);

            File oldfolder = new File(Settings.SAVE_PATH + "/" + startTime);
            File newfolder = new File(Settings.SAVE_PATH + "/" + actualStartTime);
            if (oldfolder.exists())
                oldfolder.renameTo(newfolder);
            dir = new File(Settings.SAVE_PATH + "/" + actualStartTime + "/SensorLibraryCamera/");
            File toRefresh = new File(dir, newFileNameScreen);
            refreshGallery(toRefresh);
            toRefresh = new File(dir, newFileNameCamera);
            refreshGallery(toRefresh);

            if (startTime > 0) {
                MySQLiteHelper.getInstance(getApplication()).exportAccDBWithRange(actualStartTime, end);
                MySQLiteHelper.getInstance(getApplication()).exportGyroDBWithRange(actualStartTime, end);
                MySQLiteHelper.getInstance(getApplication()).exportLightDBWithRange(actualStartTime, end);
                MySQLiteHelper.getInstance(getApplication()).exportPositionsDBWithRange(actualStartTime, end);
                startTime = 0;
            }
        } catch (RuntimeException e) {
            // RuntimeException is thrown when stop() is called immediately after start().
            // In this case the output file is not properly constructed ans should be deleted.
            e.printStackTrace();
            Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
            //noinspection ResultOfMethodCallIgnored
            //mOutputFile.delete();
        }

        // inform the user that recording has stopped
        setCaptureButtonText("Start");
        isRecording = false;
        releaseCamera();

        if (finish)
            finish();
        // END_INCLUDE(stop_release_media_recorder)
    }

    private static final String DISPLAY_NAME = "SensorLib";

    RecordingInfo recordingInfo = null;

    public void startRecordingScreen() {
        Log.d(TAG, "Starting screen recording...");
        File dir = new File(Settings.SAVE_PATH + "/" + startTime + "/SensorLibraryCamera/");
        dir.mkdirs();
        String fileName = String.format(Locale.ENGLISH, "screen_%d.webm", startTime);
        screenFile = new File(dir, fileName);

        Log.d(TAG, String.format("Recording: %s x %s @ %s", recordingInfo.width, recordingInfo.height,
                recordingInfo.density));

        recorder = new MediaRecorder();
        recorder.setVideoSource(android.media.MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
        System.out.println("FR: " + recordingInfo.frameRate);
        recorder.setVideoFrameRate(recordingInfo.frameRate);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.VP8);
        recorder.setVideoSize(recordingInfo.width, recordingInfo.height);
        recorder.setVideoEncodingBitRate(4 * 1000 * 1000);
        recorder.setOutputFile(screenFile.getPath());

        try {
            recorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException("Unable to prepare MediaRecorder.", e);
        }

        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        projection = projectionManager.getMediaProjection(resultCode, data);
        Surface surface = recorder.getSurface();
        display =
                projection.createVirtualDisplay(DISPLAY_NAME, recordingInfo.width, recordingInfo.height,
                        recordingInfo.density, android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION, surface, null, null);
        recorder.start();
        running = true;

        Log.d(TAG, "Screen recording started.");
    }

    public void stopRecordingScreen() {
        Log.d(TAG, "Stopping screen recording...");

        if (!running) {
            throw new IllegalStateException("Not running.");
        }
        running = false;

        try {
            // Stop the projection in order to flush everything to the recorder.
            projection.stop();
            // Stop the recorder which writes the contents to the file.
            recorder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        recorder.release();
        display.release();

        refreshGallery(screenFile);
    }

    private void setCaptureButtonText(String title) {
        captureButton.setText(title);
    }

    @Override
    protected void onDestroy() {
        if (service != null && !service.isShutdown())
            service.shutdown();
        super.onDestroy();
    }

    private void releaseMediaRecorder() {

        mMediaRecorder.reset();
        mMediaRecorder.release();
        mCamera.release();

        // once the objects have been released they can't be reused
        mMediaRecorder = null;
        mCamera = null;

        if (mCamera != null)
            mCamera.lock();
        if (outFile != null)
            refreshGallery(outFile);
    }

    private int getRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        int rotation = 0;
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                rotation = 90;
                break;
            case Surface.ROTATION_90:
                rotation = 0;
                break;
            case Surface.ROTATION_180:
                rotation = 270;
                break;
            case Surface.ROTATION_270:
                rotation = 180;
                break;
        }
        return rotation;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }

    File outFile = null;

    Timer timer = new Timer();
    TimerTask stopAllRunnable = new StopAllTask();

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private boolean prepareVideoRecorder() {
        Log.i(TAG, "prepareVideoRecorder()");

        // BEGIN_INCLUDE (configure_preview)
        // mCamera = CameraHelper.getDefaultFrontFacingCameraInstance();

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                mSupportedPreviewSizes, mPreview.getWidth(), mPreview.getHeight());

        for (Camera.Size s : mSupportedVideoSizes) {
            System.out.println(s.width + " " + s.height);
        }

        setCameraDisplayOrientation();

        parameters.setRotation(getRotation());
        mCamera.setParameters(parameters);

        // Use the same size for recording profile.
        parameters.setPreviewSize(mSupportedPreviewSizes.get(mSupportedPreviewSizes.size() - 1).width, mSupportedPreviewSizes.get(mSupportedPreviewSizes.size() - 1).height);

        // likewise for the camera object itself.

        mCamera.setParameters(parameters);

        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable: " + e.getMessage());
        }

        // END_INCLUDE (configure_preview)


        // BEGIN_INCLUDE (configure_media_recorder)


        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        //CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//
//        System.out.println("Setting camera size: " + mSupportedVideoSizes.get(5).width + " x " + mSupportedVideoSizes.get(5).height);
        // profile.videoFrameWidth = mSupportedVideoSizes.get(5).width;
        // profile.videoFrameHeight = mSupportedVideoSizes.get(5).height;
//
//
//        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        // mMediaRecorder.setProfile(profile);

        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoFrameRate(recordingInfo.frameRate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoSize(mSupportedVideoSizes.get(4).width, mSupportedVideoSizes.get(4).height);
        mMediaRecorder.setVideoEncodingBitRate(8 * 1000 * 1000);
        mMediaRecorder.setOrientationHint(90);

        // Step 4: Set output file
        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
        if (mOutputFile == null) {
            return false;
        }


        // END_INCLUDE (configure_media_recorder)

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            public void run() {
                PositionsData data = new PositionsData();
                data.timestamp = System.currentTimeMillis();
                data.X = movingTextView.getX();
                data.Y = movingTextView.getY();
                MySQLiteHelper.getInstance(getApplicationContext()).addToPositions(data);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        return true;
    }

    private void setCameraDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(1, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        Log.d(TAG, "Orientation: " + String.valueOf(result));

        mCamera.setDisplayOrientation(result);
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Log.i(TAG, "onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        //disable back button
    }

    class StopAllTask extends TimerTask {

        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "stopTestRunnable()");
                    DemoActivity.exitCode = 1;
                    stopAllStuff(true);
                }
            });
        }
    }

    /*@Override
    public void onStop() {
        releaseCamera();
        if(running)
            stopRecordingScreen();
        super.onStop();
    }*/

    public long actualStartTime = 0L;

    /**
     * Asynchronous task for preparing the {@link MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            //init screen recording
            // initialize video camera
            startTime = System.currentTimeMillis() / 1000;
            File dir = new File(Settings.SAVE_PATH + "/" + startTime + "/SensorLibraryCamera/");
            dir.mkdirs();
            String fileName = String.format(Locale.ENGLISH, "camera_%d.mp4", startTime);

            outFile = new File(dir, fileName);
            mMediaRecorder.setOutputFile(outFile.getPath());
            recordingInfo = getRecordingInfo();
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                startRecordingScreen();
                mMediaRecorder.start();
                actualStartTime = System.currentTimeMillis();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        screenRandomAnimator(movingTextView);
                    }
                });

                isRecording = true;
                timer.schedule(stopAllRunnable, durationOfTest);
               /*handler.postDelayed(new Runnable() {
                    public void run() {
                        runOnUiThread(stopTestRunnable);
                    }}, durationOfTest);*/
            } else {
                // prepare didn't work, release the camera
                System.out.println("Couldn't prepareVideoRecorder");
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                // CamRecorderActivity.this.finish();
            }
            // inform the user that recording has started
            setCaptureButtonText("Experiment Running...");
        }
    }

    private RecordingInfo getRecordingInfo() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        int displayHeight = displayMetrics.heightPixels;
        int displayDensity = displayMetrics.densityDpi;
        Log.d(TAG, String.format("Display size: %s x %s @ %s", displayWidth, displayHeight, displayDensity));

        Configuration configuration = getResources().getConfiguration();
        boolean isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;

        // Get the best camera profile available. We assume MediaRecorder supports the highest.
        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        int cameraWidth = camcorderProfile != null ? camcorderProfile.videoFrameWidth : -1;
        int cameraHeight = camcorderProfile != null ? camcorderProfile.videoFrameHeight : -1;
        int cameraFrameRate = camcorderProfile != null ? camcorderProfile.videoFrameRate : 30;
        Log.d(TAG, String.format("Camera size: %s x %s framerate: %s", cameraWidth, cameraHeight, cameraFrameRate));


        return calculateRecordingInfo(displayWidth, displayHeight, displayDensity, false,
                cameraWidth, cameraHeight, cameraFrameRate);
    }

    private static RecordingInfo calculateRecordingInfo(int displayWidth, int displayHeight,
                                                        int displayDensity, boolean isLandscapeDevice, int cameraWidth, int cameraHeight,
                                                        int cameraFrameRate) {
        // Scale the display size before any maximum size calculations.
        displayWidth = 1280;
        displayHeight = 720;

        if (cameraWidth == -1 && cameraHeight == -1) {
            // No cameras. Fall back to the display size.
            return new RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity);
        }

        int frameWidth = isLandscapeDevice ? cameraWidth : cameraHeight;
        int frameHeight = isLandscapeDevice ? cameraHeight : cameraWidth;
        if (frameWidth >= displayWidth && frameHeight >= displayHeight) {
            // Frame can hold the entire display. Use exact values.
            return new RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity);
        }

        // Calculate new width or height to preserve aspect ratio.
        if (isLandscapeDevice) {
            frameWidth = displayWidth * frameHeight / displayHeight;
        } else {
            frameHeight = displayHeight * frameWidth / displayWidth;
        }
        return new RecordingInfo(frameWidth, frameHeight, cameraFrameRate, displayDensity);
    }

    static final class RecordingInfo {
        final int width;
        final int height;
        final int frameRate;
        final int density;

        RecordingInfo(int width, int height, int frameRate, int density) {
            this.width = width;
            this.height = height;
            this.frameRate = frameRate;
            this.density = density;
        }
    }
}