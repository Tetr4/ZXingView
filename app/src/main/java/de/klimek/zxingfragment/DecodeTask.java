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
	private MultiFormatReader mMultiFormatReader = new MultiFormatReader();

	public DecodeTask(Decoder decoder, Camera camera,
			int cameraDisplayOrientation) {
		mDecoder = decoder;
		mCamera = camera;
		mCameraDisplayOrientation = cameraDisplayOrientation;
	}

	@Override
	protected Result doInBackground(byte[]... datas) {
		return getResult(datas[0], mCamera, mCameraDisplayOrientation,
				mMultiFormatReader);
	}

	@Override
	protected void onPostExecute(Result result) {
		if (result != null) {
			mDecoder.onDecodeSuccess(result.toString());
		} else {
			mDecoder.onDecodeFail();
		}
	}

	private static Result getResult(byte[] data, Camera camera,
			int cameraDisplayOrientation, MultiFormatReader reader) {
		Camera.Size previewSize = camera.getParameters().getPreviewSize();

		Rect boundingRect = getBoundingRect(previewSize,
				cameraDisplayOrientation);
		PlanarYUVLuminanceSource source = buildLuminanceSource(data,
				previewSize, boundingRect, cameraDisplayOrientation);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

		try {
			return reader.decodeWithState(bitmap);
		} catch (NotFoundException e) {
			return null;
		} finally {
			reader.reset();
		}
	}

	private static PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
			Camera.Size previewSize, Rect boundingRect,
			int cameraDisplayOrientation) {

		switch (cameraDisplayOrientation) {
		case 0:
			// data = flip(data);
			break;

		case 90:
			rotate90(data, previewSize.width, previewSize.height);
			return new PlanarYUVLuminanceSource(data, previewSize.height,
					previewSize.width, boundingRect.top, boundingRect.left,
					boundingRect.height(), boundingRect.width(), false);
		case 180:
			break;

		case 270:
			rotate90(data, previewSize.width, previewSize.height);
			break;
		}

		return new PlanarYUVLuminanceSource(data, previewSize.width,
				previewSize.height, boundingRect.left, boundingRect.top,
				boundingRect.width(), boundingRect.height(), false);
	}

	private static Rect getBoundingRect(Camera.Size previewSize,
			int cameraDisplayOrientation) {
		double heightFraction = ZxingFragment.RETICLE_FRACTION;
		double widthFraction = ZxingFragment.RETICLE_FRACTION;
		if (cameraDisplayOrientation == 90 || cameraDisplayOrientation == 270) {
			widthFraction = ZxingFragment.RETICLE_HEIGHT_FRACTION_PORTRAIT;
		}
		int height = (int) (previewSize.height * heightFraction);
		int width = (int) (previewSize.width * widthFraction);
		int left = (int) (previewSize.width * ((1 - widthFraction) / 2));
		int top = (int) (previewSize.height * ((1 - heightFraction) / 2));
		int right = left + width;
		int bottom = top + height;
		return new Rect(left, top, right, bottom);
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
}