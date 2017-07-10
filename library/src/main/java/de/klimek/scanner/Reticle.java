package de.klimek.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Reticle that is used as view finder over the {@link CameraPreview}
 */
class Reticle extends View {
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Rect mTargetRect = new Rect();
    private double mReticleFraction = 1.0;

    public Reticle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setSize(double reticleFraction) {
        mReticleFraction = reticleFraction;
    }

    void setColor(int color) {
        mPaint.setColor(color);
        mPaint.setAlpha(100);
    }

    void drawTargetRect(Canvas canvas) {
        int height = (int) (canvas.getHeight() * mReticleFraction);
        int width = (int) (canvas.getWidth() * mReticleFraction);
        int smallestDim = Math.min(height, width);

        int left = (canvas.getWidth() - smallestDim) / 2;
        int top = (canvas.getHeight() - smallestDim) / 2;
        int right = left + smallestDim;
        int bottom = top + smallestDim;

        mTargetRect.set(left, top, right, bottom);
        canvas.drawRect(mTargetRect, mPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTargetRect(canvas);
    }
}