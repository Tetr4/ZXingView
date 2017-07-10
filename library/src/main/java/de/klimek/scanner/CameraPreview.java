package de.klimek.scanner;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Surface on which the camera preview display is drawn.
 */
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private boolean mSurfaceCreated = false;

    private Camera mCamera;
    private Size mPreviewSize;
    private int mDisplayOrientation;

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        if (mPreviewSize == null && mCamera != null) {
            mPreviewSize = mCamera.getParameters().getPreviewSize();
        }

		/*
         * Set height and width for the SurfaceView. Width and height depend on
		 * the aspect ratio (no stretching).
		 */
        if (mPreviewSize != null) {
            double aspectRatio;
            if (mDisplayOrientation == 90 || mDisplayOrientation == 270) {
                aspectRatio = (double) mPreviewSize.height / mPreviewSize.width;
            } else {
                aspectRatio = (double) mPreviewSize.width / mPreviewSize.height;
            }

            int newWidth = (int) (height * aspectRatio);
            int newHeight = (int) (width / aspectRatio);

            if (newHeight < height) {
                setMeasuredDimension(newWidth, height);
            } else {
                setMeasuredDimension(width, newHeight);
            }
        } else {
            setMeasuredDimension(width, height);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceCreated = true;
        startPreview(mCamera, mDisplayOrientation);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceCreated = false;
    }

    void startPreview(Camera camera, int displayOrientation) {
        mCamera = camera;
        mDisplayOrientation = displayOrientation;
        if (mCamera != null && mSurfaceCreated) {
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        mCamera = null;
        mPreviewSize = null;
    }
}