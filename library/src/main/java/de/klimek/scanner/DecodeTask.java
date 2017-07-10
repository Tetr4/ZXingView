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
    private MultiFormatReader mMultiFormatReader = new MultiFormatReader();

    DecodeTask(Decoder decoder, Camera camera, int cameraDisplayOrientation, Rect boundingRect) {
        mDecoder = decoder;
        mPreviewSize = camera.getParameters().getPreviewSize();
        mCameraDisplayOrientation = cameraDisplayOrientation;
        mBoundingRect = boundingRect;
    }

    private static PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, Camera.Size previewSize, Rect boundingRect, int cameraDisplayOrientation) {
        byte[] rotatedData = rotate(data, previewSize.width, previewSize.height, cameraDisplayOrientation);
        boolean swap = (cameraDisplayOrientation == 90 || cameraDisplayOrientation == 270);
        return new PlanarYUVLuminanceSource(
                rotatedData,
                swap ? previewSize.height : previewSize.width,
                swap ? previewSize.width : previewSize.height,
                swap ? boundingRect.top : boundingRect.left,
                swap ? boundingRect.left : boundingRect.top,
                swap ? boundingRect.height() : boundingRect.width(),
                swap ? boundingRect.width() : boundingRect.height(),
                false);
    }

    private static byte[] rotate(byte[] yuv, int width, int height, int rotation) {
        if (rotation == 0) {
            return yuv;
        }

        boolean swap = (rotation == 90 || rotation == 270);
        boolean flipX = (rotation == 90 || rotation == 180);
        boolean flipY = (rotation == 180 || rotation == 270);

        // rotate image in NV21 encoding, which is the default for camera preview format
        byte[] rotated = new byte[yuv.length];
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

                rotated[yOut] = (byte) (0xff & yuv[yIn]);
                rotated[uOut] = (byte) (0xff & yuv[uIn]);
                rotated[vOut] = (byte) (0xff & yuv[vIn]);
            }
        }
        return rotated;
    }

    @Override
    protected Result doInBackground(byte[]... datas) {
        PlanarYUVLuminanceSource source = buildLuminanceSource(datas[0], mPreviewSize, mBoundingRect, mCameraDisplayOrientation);
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
}