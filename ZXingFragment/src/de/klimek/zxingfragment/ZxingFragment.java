package de.klimek.zxingfragment;

import java.util.List;

import android.app.Fragment;
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
import de.klimek.zxingfragment.Decoder.OnDecodedCallback;

public class ZxingFragment extends Fragment {
	private static final String TAG = ZxingFragment.class.getSimpleName();

	private static final boolean USE_FLASH = true;

	private ScannerView mScannerView;
	private Reticle mReticle;

	private Camera mCamera;
	private int mCameraId;
	private CameraInfo mCameraInfo;
	private AsyncTask<Void, Void, Exception> mStartCameraTask;

	private Decoder mDecoder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCameraId = chooseCamera();
		mCameraInfo = new CameraInfo();
		Camera.getCameraInfo(mCameraId, mCameraInfo);

		mDecoder = new Decoder(getActivity());
	}

	private int chooseCamera() {
		// TODO choose camera here
		return 0;
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
		};

		mStartCameraTask.execute();
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

	private static void optimizeCameraParams(Camera camera) {
		Parameters params = camera.getParameters();
		
		// focus mode
		List<String> focusModes = params.getSupportedFocusModes();
		if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
			params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		}
		
		// flash mode
		if(USE_FLASH) {
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

}
