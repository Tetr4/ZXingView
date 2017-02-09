package de.klimek.zxingfragment;

import android.app.Fragment;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.klimek.zxingfragment.Decoder.OnDecodedCallback;

public class ZxingFragment extends Fragment {
    // height and width of the reticle as fraction of the camera preview frame
    static final double RETICLE_FRACTION = .8;
    // height of the reticle in portrait mode
    static final double RETICLE_HEIGHT_FRACTION_PORTRAIT = 0.4;
    private static final String TAG = ZxingFragment.class.getSimpleName();
    private static final boolean USE_FLASH = true;
    private ScannerView mScannerView;
    private Reticle mReticle;

    private Camera mCamera;
    private int mCameraId;
    private CameraInfo mCameraInfo;
    private AsyncTask<Void, Void, Exception> mStartCameraTask;

    private Decoder mDecoder;

    private static void optimizeCameraParams(Camera camera) {
        Parameters params = camera.getParameters();

        // focus mode
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        // flash mode
        if (USE_FLASH) {
            List<String> flashModes = params.getSupportedFlashModes();
            if (flashModes != null
                    && flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
                params.setFlashMode(Parameters.FLASH_MODE_TORCH);
            }
        }

        // smoother on some devices
        params.setRecordingHint(true);

        camera.setParameters(params);
    }

    private static int getCameraDisplayOrientation(Display display, CameraInfo cameraInfo) {
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
        if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }

        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCameraId = 0; // default camera
        mCameraInfo = new CameraInfo();
        Camera.getCameraInfo(mCameraId, mCameraInfo);

        mDecoder = new Decoder(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_zxing, container,
                false);

        mScannerView = (ScannerView) rootView.findViewById(R.id.scanner_view);
        mReticle = (Reticle) rootView.findViewById(R.id.reticle);

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopScanning();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // called when display is rotated
        if (mCamera != null) {
            Display display = getActivity().getWindowManager()
                    .getDefaultDisplay();
            int displayOrientation = getCameraDisplayOrientation(display,
                    mCameraInfo);
            mCamera.setDisplayOrientation(displayOrientation);
            mReticle.setDisplayOrientation(displayOrientation);
            mScannerView.stopPreview();
            mScannerView.startPreview(mCamera, displayOrientation);
            mDecoder.stopDecoding();
            mDecoder.startDecoding(mCamera, displayOrientation);
        }
    }

    public void setOnDecodedCallback(OnDecodedCallback callback) {
        mDecoder.setOnDecodedCallback(callback);
    }

    public void startScanning() {
        // Task for smooth UI interaction while camera loads
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
                    Log.w(TAG, "Exception while opening camera", e);
                    mCamera = null;
                } else {
                    Display display = getActivity().getWindowManager()
                            .getDefaultDisplay();
                    int displayOrientation = getCameraDisplayOrientation(
                            display, mCameraInfo);
                    mCamera.setDisplayOrientation(displayOrientation);
                    mScannerView.startPreview(mCamera, displayOrientation);
                    mDecoder.startDecoding(mCamera, displayOrientation);
                    mReticle.setDisplayOrientation(displayOrientation);
                    mScannerView.setVisibility(View.VISIBLE);
                    mReticle.setVisibility(View.VISIBLE);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void stopScanning() {
        if (mStartCameraTask != null) {
            mStartCameraTask.cancel(true);
        }
        if (mCamera != null) {
            mDecoder.stopDecoding();
            mScannerView.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        mScannerView.setVisibility(View.INVISIBLE);
        mReticle.setVisibility(View.INVISIBLE);
    }
}
