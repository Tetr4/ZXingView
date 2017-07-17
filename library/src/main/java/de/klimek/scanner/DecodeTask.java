package de.klimek.scanner;

import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

class DecodeTask extends AsyncTask<byte[], Void, Result> {
    private Decoder mDecoder;
    private Camera.Size mPreviewSize;
    private int mCameraDisplayOrientation;
    private Rect mBoundingRect;
    private MultiFormatReader mMultiFormatReader;
    private final byte[] mRotationBuffer;

    DecodeTask(Decoder decoder, Camera.Size previewSize, int cameraDisplayOrientation, Rect boundingRect, MultiFormatReader multiFormatReader, byte[] rotationBuffer) {
        mDecoder = decoder;
        mPreviewSize = previewSize;
        mCameraDisplayOrientation = cameraDisplayOrientation;
        mBoundingRect = boundingRect;
        mMultiFormatReader = multiFormatReader;
        mRotationBuffer = rotationBuffer;
    }

    @Override
    protected Result doInBackground(byte[]... datas) {
        PlanarYUVLuminanceSource source = buildLuminanceSource(datas[0]);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            return mMultiFormatReader.decodeWithState(bitmap);
        } catch (NotFoundException e) {
            return null;
        } finally {
            mMultiFormatReader.reset();
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        if (result != null) {
            mDecoder.onDecodeSuccess(result.toString());
        } else {
            mDecoder.onDecodeFail();
        }
    }

    private PlanarYUVLuminanceSource buildLuminanceSource(byte[] data) {
        byte[] rotatedData = rotate(data, mPreviewSize.width, mPreviewSize.height, mCameraDisplayOrientation);
        boolean swap = (mCameraDisplayOrientation == 90 || mCameraDisplayOrientation == 270);
        return new PlanarYUVLuminanceSource(
                rotatedData,
                swap ? mPreviewSize.height : mPreviewSize.width,
                swap ? mPreviewSize.width : mPreviewSize.height,
                swap ? mBoundingRect.top : mBoundingRect.left,
                swap ? mBoundingRect.left : mBoundingRect.top,
                swap ? mBoundingRect.height() : mBoundingRect.width(),
                swap ? mBoundingRect.width() : mBoundingRect.height(),
                false);
    }

    private byte[] rotate(byte[] yuv, int width, int height, int rotation) {
        if (rotation == 0) {
            return yuv;
        }

        boolean swap = (rotation == 90 || rotation == 270);
        boolean flipX = (rotation == 90 || rotation == 180);
        boolean flipY = (rotation == 180 || rotation == 270);

        // rotate image in NV21 encoding, which is the default for camera preview format
        // TODO rotate in place to save memory?
        int size = width * height;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int yIn = y * width + x;
                int uIn = size + (y >> 1) * width + (x & ~1);
                int vIn = uIn + 1;

                int wSwapped = swap ? height : width;
                int hSwapped = swap ? width : height;
                int xSwapped = swap ? y : x;
                int ySwapped = swap ? x : y;
                int xFlipped = flipX ? wSwapped - xSwapped - 1 : xSwapped;
                int yFlipped = flipY ? hSwapped - ySwapped - 1 : ySwapped;

                int yOut = yFlipped * wSwapped + xFlipped;
                int uOut = size + (yFlipped >> 1) * wSwapped + (xFlipped & ~1);
                int vOut = uOut + 1;

                mRotationBuffer[yOut] = (byte) (0xff & yuv[yIn]);
                mRotationBuffer[uOut] = (byte) (0xff & yuv[uIn]);
                mRotationBuffer[vOut] = (byte) (0xff & yuv[vIn]);
            }
        }
        return mRotationBuffer;
    }
}