package de.klimek.scanner;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

class Decoder implements Camera.PreviewCallback {
    private static final String TAG = Decoder.class.getSimpleName();
    private static final Long DECODE_INTERVAL = 500L;

    private Camera mCamera;
    private int mCameraDisplayOrientation;
    private byte[] mPreviewBuffer;
    private Rect mBoundingRect;

    private volatile boolean mDecoding = false;
    private Timer mDelayTimer = new Timer();
    private DecodeTask mDecodeTask;
    private OnDecodedCallback mCallback;

    private static byte[] createPreviewBuffer(Camera camera) {
        Parameters params = camera.getParameters();
        int width = params.getPreviewSize().width;
        int height = params.getPreviewSize().height;
        int bitsPerPixel = ImageFormat.getBitsPerPixel(params.getPreviewFormat());
        int sizeInBits = width * height * bitsPerPixel;
        int sizeInBytes = (int) Math.ceil((float) sizeInBits / Byte.SIZE);
        return new byte[sizeInBytes];
    }

    void setOnDecodedCallback(OnDecodedCallback callback) {
        mCallback = callback;
    }

    void startDecoding(Camera camera, int cameraDisplayOrientation, double reticleFraction) {
        mDecoding = true;

        mCamera = camera;
        mCameraDisplayOrientation = cameraDisplayOrientation;
        mBoundingRect = getBoundingRect(camera, reticleFraction);

        // add buffer to camera to prevent garbage collection spam
        mPreviewBuffer = createPreviewBuffer(camera);
        camera.addCallbackBuffer(mPreviewBuffer);
        camera.setPreviewCallbackWithBuffer(this);
    }

    void stopDecoding() {
        mDecoding = false;
        if (mDecodeTask != null) {
            mDecodeTask.cancel(true);
        }
    }

    private Rect getBoundingRect(Camera camera, double fraction) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        int height = (int) (previewSize.height * fraction);
        int width = (int) (previewSize.width * fraction);
        int reticleDim = Math.min(height, width);

        int left = (previewSize.width - reticleDim) / 2;
        int top = (previewSize.height - reticleDim) / 2;
        int right = left + reticleDim;
        int bottom = top + reticleDim;

        return new Rect(left, top, right, bottom);
    }

    /*
     * Called when the camera has a buffer, e.g. by calling
     * camera.addCallbackBuffer(buffer). This buffer is automatically removed,
     * but added again after decoding, resulting in a loop until stopDecoding()
     * is called. The data is not affected by Camera#setDisplayOrientation(),
     * so it may be rotated.
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mDecoding) {
            mDecodeTask = new DecodeTask(this, camera, mCameraDisplayOrientation, mBoundingRect);
            mDecodeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);
        }
    }

    /*
     * Called by mDecodeTask
     */
    void onDecodeSuccess(String string) {
        Log.i(Decoder.TAG, "Decode success.");
        if (mDecoding) {
            mCallback.onDecoded(string);
            // request next frame after delay
            mDelayTimer.schedule(new RequestPreviewFrameTask(), DECODE_INTERVAL);
        }
    }

    /*
     * Called by mDecodeTask
     */
    void onDecodeFail() {
        // Log.i(Decoder.TAG, "Decode fail.");
        if (mDecoding) {
            // request next frame after delay
            mDelayTimer.schedule(new RequestPreviewFrameTask(), DECODE_INTERVAL);
        }
    }

    private class RequestPreviewFrameTask extends TimerTask {
        @Override
        public void run() {
            if (mDecoding) {
                // adding in other thread is okay as onPreviewFrame will be called on main thread
                mCamera.addCallbackBuffer(mPreviewBuffer);
            }
        }
    }
}
