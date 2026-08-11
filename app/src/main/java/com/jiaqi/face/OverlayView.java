package com.jiaqi.face;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * 覆盖层视图 - 绘制人脸框
 */
public class OverlayView extends View {
    private Rect faceRect;
    private float quality;
    private int previewWidth;
    private int previewHeight;
    private int imageWidth;
    private int imageHeight;
    private Paint paint;
    private Paint textPaint;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setTextSize(36f);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
    }

    /**
     * 设置人脸矩形和质量
     */
    public void setFaceRect(Rect rect, float quality, int previewWidth, int previewHeight,
                            int imageWidth, int imageHeight) {
        this.faceRect = rect;
        this.quality = quality;
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        invalidate();
    }

    /**
     * 清除人脸矩形
     */
    public void clearFaceRect() {
        this.faceRect = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (faceRect == null || previewWidth <= 0 || previewHeight <= 0) {
            return;
        }

        // 计算缩放比例
        float scaleX = (float) previewWidth / imageWidth;
        float scaleY = (float) previewHeight / imageHeight;

        // 根据质量设置颜色
        int color;
        if (quality >= 0.4f) {
            color = Color.parseColor("#00C853"); // 绿色
        } else if (quality >= 0.3f) {
            color = Color.parseColor("#FFEB3B"); // 黄色
        } else {
            color = Color.parseColor("#FF1744"); // 红色
        }

        paint.setColor(color);
        textPaint.setColor(color);

        // 绘制人脸框
        float left = faceRect.left * scaleX;
        float top = faceRect.top * scaleY;
        float right = faceRect.right * scaleX;
        float bottom = faceRect.bottom * scaleY;

        canvas.drawRect(left, top, right, bottom, paint);

        // 绘制质量分数
        String qualityText = String.format("%.2f", quality);
        canvas.drawText(qualityText, left, top - 15, textPaint);
    }
}
