package de.klimek.scanner;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.List;

/**
 * Wraps a {@link CameraPreview} and {@link Reticle} and handles scanning.
 */
public class ScannerView extends FrameLayout {
    // TODO as XML attributes
    // width and height of the reticle as fraction of the camera preview frame
    public static final double RETICLE_FRACTION = .6;
    public static final boolean USE_FLASH = true;
    private static final String TAG = ScannerView.class.getSimpleName();
    private CameraPreview mCameraPreview;
    private Reticle mReticle;

    private Camera mCamera;
    private int mCameraId;
    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private AsyncTask<Void, Void, Exception> mStartCameraTask;

    private Decoder mDecoder;

    public ScannerView(Context context) {
        this(context, null);
    }

    public ScannerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScannerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        inflate(getContext(), R.layout.scanner, this);
        mCameraPreview = (CameraPreview) findViewById(R.id.camera_preview);
        mReticle = (Reticle) findViewById(R.id.reticle);
        mReticle.setSize(RETICLE_FRACTION);

        mCameraId = selectCamera();
        Camera.getCameraInfo(mCameraId, mCameraInfo);

        mDecoder = new Decoder();
    }

    private static void optimizeCameraParams(Camera camera) {
        Camera.Parameters params = camera.getParameters();

        // focus mode
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        // flash mode
        if (USE_FLASH) {
            List<String> flashModes = params.getSupportedFlashModes();
            if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
        }

        // smoother on some devices
        params.setRecordingHint(true);

        camera.setParameters(params);
    }

    private static int getCameraDisplayOrientation(Display display, Camera.CameraInfo cameraInfo) {
        int degrees = 0;
        switch (display.getRotation()) {
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
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private int selectCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

		/*
         * Barcodes will be blurry and thus unreadably on the back camera,
		 * should the device not support autofocus (e.g. Samsung Galaxy Tab 2
		 * 7.0). In that case we use the front camera which usually has a closer
		 * focus.
		 */
        PackageManager packageManager = getContext().getPackageManager();
        boolean hasAutoFocus = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
        boolean hasFrontCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
        if (!hasAutoFocus && hasFrontCamera) {
            // use front camera
            for (int curCameraId = 0; curCameraId < Camera.getNumberOfCameras(); curCameraId++) {
                Camera.getCameraInfo(curCameraId, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    return curCameraId;
                }
            }
        }

        // use default camera;
        return 0;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // called when display is rotated
        // FIXME not called when flipping from landscape to reverse landscape
        super.onConfigurationChanged(newConfig);
        if (mCamera != null) {
            WindowManager windowManager = (WindowManager) getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            int displayOrientation = getCameraDisplayOrientation(display, mCameraInfo);
            mCamera.setDisplayOrientation(displayOrientation);
            mCameraPreview.stopPreview();
            mCameraPreview.startPreview(mCamera, displayOrientation);
            mDecoder.stopDecoding();
            mDecoder.startDecoding(mCamera, displayOrientation, RETICLE_FRACTION);
        }
    }

    public final void startScanning() {
        // Task for smooth UI while camera loads
        mStartCameraTask = new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... v) {
                try {
                    mCamera = Camera.open(mCameraId);
                } catch (RuntimeException e) {
                    return e;
                }
                optimizeCameraParams(mCamera);
                return null;
            }

            @Override
            protected void onPostExecute(Exception e) {
                if (e != null) {
                    Log.w(TAG, "Exception while opening camera: " + e.getMessage());
                    mCamera = null;
                    // TODO report error to callback?
                    return;
                }
                WindowManager windowManager = (WindowManager) getContext()
                        .getSystemService(Context.WINDOW_SERVICE);
                Display display = windowManager.getDefaultDisplay();
                int displayOrientation = getCameraDisplayOrientation(display, mCameraInfo);
                mCamera.setDisplayOrientation(displayOrientation);
                mCameraPreview.startPreview(mCamera, displayOrientation);
                mDecoder.startDecoding(mCamera, displayOrientation, RETICLE_FRACTION);
                mCameraPreview.setVisibility(View.VISIBLE);
                mReticle.setVisibility(View.VISIBLE);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public final void stopScanning() {
        if (mStartCameraTask != null) {
            mStartCameraTask.cancel(true);
        }
        if (mCamera != null) {
            mDecoder.stopDecoding();
            mCameraPreview.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        mCameraPreview.setVisibility(View.INVISIBLE);
        mReticle.setVisibility(View.INVISIBLE);
    }

    public void setOnDecodedCallback(OnDecodedCallback callback) {
        mDecoder.setOnDecodedCallback(callback);
    }
}
