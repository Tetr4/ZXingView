package de.klimek.scanner;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

class Decoder implements Camera.PreviewCallback {
    private static final String TAG = Decoder.class.getSimpleName();

    private Camera mCamera;
    private Camera.Size mPreviewSize;
    private int mCameraDisplayOrientation;
    private byte[] mPreviewBuffer;
    private float mReticleFraction;
    private Rect mBoundingRect;

    private volatile boolean mDecoding = false;
    private int mDecodeInterval;
    private Timer mDelayTimer;
    private DecodeTask mDecodeTask;
    private OnDecodedCallback mCallback;


    Decoder(int decodeInterval, float reticleFraction) {
        mDecodeInterval = decodeInterval;
        mReticleFraction = reticleFraction;
    }

    void setOnDecodedCallback(OnDecodedCallback callback) {
        mCallback = callback;
    }

    void startDecoding(Camera camera, int cameraDisplayOrientation) {
        mDecoding = true;
        mDelayTimer = new Timer();

        mCamera = camera;
        mCameraDisplayOrientation = cameraDisplayOrientation;
        mPreviewSize = camera.getParameters().getPreviewSize();
        mBoundingRect = getBoundingRect(mPreviewSize, mReticleFraction);

        // add buffer to camera to prevent garbage collection spam
        mPreviewBuffer = createPreviewBuffer(mPreviewSize);
        camera.addCallbackBuffer(mPreviewBuffer);
        camera.setPreviewCallbackWithBuffer(this);
    }

    void stopDecoding() {
        mDecoding = false;
        mDelayTimer.cancel();
        mDelayTimer = null;
        if (mDecodeTask != null) {
            mDecodeTask.cancel(true);
        }
    }

    private static Rect getBoundingRect(Camera.Size previewSize, double fraction) {
        int height = (int) (previewSize.height * fraction);
        int width = (int) (previewSize.width * fraction);
        int reticleDim = Math.min(height, width);

        int left = (previewSize.width - reticleDim) / 2;
        int top = (previewSize.height - reticleDim) / 2;
        int right = left + reticleDim;
        int bottom = top + reticleDim;

        return new Rect(left, top, right, bottom);
    }

    private static byte[] createPreviewBuffer(Camera.Size mPreviewSize) {
        int width = mPreviewSize.width;
        int height = mPreviewSize.height;
        int bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21);  // default camera format
        int sizeInBits = width * height * bitsPerPixel;
        int sizeInBytes = (int) Math.ceil((float) sizeInBits / Byte.SIZE);
        return new byte[sizeInBytes];
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
            mDecodeTask = new DecodeTask(this, mPreviewSize, mCameraDisplayOrientation, mBoundingRect);
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
            mDelayTimer.schedule(new RequestPreviewFrameTask(), mDecodeInterval);
        }
    }

    /*
     * Called by mDecodeTask
     */
    void onDecodeFail() {
        // Log.i(Decoder.TAG, "Decode fail.");
        if (mDecoding) {
            // request next frame after delay
            mDelayTimer.schedule(new RequestPreviewFrameTask(), mDecodeInterval);
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
