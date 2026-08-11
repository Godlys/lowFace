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
 * 支持前置摄像头镜像处理
 */
public class OverlayView extends View {
    private Rect faceRect;
    private float quality;
    private int previewWidth;
    private int previewHeight;
    private int imageWidth;
    private int imageHeight;
    private boolean needMirror = false;  // 是否需要镜像坐标（前置摄像头）
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
     * @param rect 人脸矩形（基于原始图像坐标）
     * @param quality 人脸质量
     * @param previewWidth 预览视图宽度
     * @param previewHeight 预览视图高度
     * @param imageWidth 原始图像宽度
     * @param imageHeight 原始图像高度
     * @param mirror 是否需要镜像坐标（前置摄像头为 true）
     */
    public void setFaceRect(Rect rect, float quality, int previewWidth, int previewHeight,
                            int imageWidth, int imageHeight, boolean mirror) {
        this.faceRect = rect;
        this.quality = quality;
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.needMirror = mirror;
        invalidate();
    }

    /**
     * 兼容旧接口（默认镜像）
     */
    public void setFaceRect(Rect rect, float quality, int previewWidth, int previewHeight,
                            int imageWidth, int imageHeight) {
        setFaceRect(rect, quality, previewWidth, previewHeight, imageWidth, imageHeight, true);
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

        // 计算人脸框坐标（考虑缩放和镜像）
        float left, top, right, bottom;

        if (needMirror) {
            // 前置摄像头：PreviewView 显示的是镜像后的预览
            // 人脸检测坐标基于原始图像，需要镜像到预览坐标系
            // newLeft = previewWidth - oldRight, newRight = previewWidth - oldLeft
            left = previewWidth - faceRect.right * scaleX;
            right = previewWidth - faceRect.left * scaleX;
        } else {
            // 后置摄像头：直接缩放
            left = faceRect.left * scaleX;
            right = faceRect.right * scaleX;
        }

        top = faceRect.top * scaleY;
        bottom = faceRect.bottom * scaleY;

        canvas.drawRect(left, top, right, bottom, paint);

        // 绘制质量分数
        String qualityText = String.format("%.2f", quality);
        canvas.drawText(qualityText, left, top - 15, textPaint);
    }
}
