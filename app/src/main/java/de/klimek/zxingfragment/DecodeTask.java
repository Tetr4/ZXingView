package de.klimek.zxingfragment;

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
    private Camera mCamera;
    private int mCameraDisplayOrientation = 0;
    private double mReticleFraction;
    private MultiFormatReader mMultiFormatReader = new MultiFormatReader();

    public DecodeTask(Decoder decoder, Camera camera,
                      int cameraDisplayOrientation, double reticleFraction) {
        mDecoder = decoder;
        mCamera = camera;
        mCameraDisplayOrientation = cameraDisplayOrientation;
        mReticleFraction = reticleFraction;
    }

    private static Rect getReticleRect(Camera.Size previewSize, double reticleFraction) {
        int height = (int) (previewSize.height * reticleFraction);
        int width = (int) (previewSize.width * reticleFraction);
        int smallestDim = Math.min(height, width);

        int left = (previewSize.width - smallestDim) / 2;
        int top = (previewSize.height - smallestDim) / 2;
        int right = left + smallestDim;
        int bottom = top + smallestDim;

        return new Rect(left, top, right, bottom);
    }

    private static PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, Camera.Size previewSize, Rect boundingRect, int cameraDisplayOrientation) {
        switch (cameraDisplayOrientation) {
            case 0:
                // data = flip(data);
                break;

            case 90:
                rotate90(data, previewSize.width, previewSize.height);
                return new PlanarYUVLuminanceSource(data,
                        previewSize.height, previewSize.width,
                        boundingRect.top, boundingRect.left,
                        boundingRect.height(), boundingRect.width(),
                        false);
            case 180:
                break;

            case 270:
                rotate90(data, previewSize.width, previewSize.height);
                break;
        }

        return new PlanarYUVLuminanceSource(data,
                previewSize.width, previewSize.height,
                boundingRect.left, boundingRect.top,
                boundingRect.width(), boundingRect.height(),
                false);
    }

    private static void rotate90(byte[] data, int width, int height) {
        int length = height * width;
        int lengthDec = length - 1;
        int i = 0;
        do {
            int k = (i * height) % lengthDec;
            while (k > i)
                k = (height * k) % lengthDec;
            if (k != i)
                swap(data, k, i);
        } while (++i <= (length - 2));
    }

    private static void swap(byte[] data, int k, int i) {
        byte temp = data[k];
        data[k] = data[i];
        data[i] = temp;
    }

    @Override
    protected Result doInBackground(byte[]... datas) {
        byte[] data = datas[0];
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();

        Rect boundingRect = getReticleRect(previewSize, mReticleFraction);
        PlanarYUVLuminanceSource source = buildLuminanceSource(data, previewSize, boundingRect, mCameraDisplayOrientation);
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