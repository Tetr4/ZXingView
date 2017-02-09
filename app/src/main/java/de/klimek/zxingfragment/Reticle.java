package de.klimek.zxingfragment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class Reticle extends View {
    private Paint mPaint;
    private Rect mTargetRect = new Rect();
    private int mDisplayOrientation;

    public Reticle(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(5);
        mPaint.setARGB(100, 0x9F, 0xCD, 0x46); // TODO Resource
    }

    public void drawTargetRect(Canvas canvas) {
        double heightFraction = ZxingFragment.RETICLE_FRACTION;
        double widthFraction = ZxingFragment.RETICLE_FRACTION;
        if (mDisplayOrientation == 90 || mDisplayOrientation == 270) {
            heightFraction = ZxingFragment.RETICLE_HEIGHT_FRACTION_PORTRAIT;
        }

        int height = (int) (canvas.getHeight() * heightFraction);
        int width = (int) (canvas.getWidth() * widthFraction);
        int left = (int) (canvas.getWidth() * ((1 - widthFraction) / 2));
        int top = (int) (canvas.getHeight() * ((1 - heightFraction) / 2));
        int right = left + width;
        int bottom = top + height;
        mTargetRect.set(left, top, right, bottom);

        canvas.drawRect(mTargetRect, mPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTargetRect(canvas);
    }

    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
    }
}