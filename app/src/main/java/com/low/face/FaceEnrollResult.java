package com.low.face;

/**
 * 录入结果
 */
public class FaceEnrollResult {
    public final boolean success;
    public final String message;
    public final float quality;

    public FaceEnrollResult(boolean success, String message, float quality) {
        this.success = success;
        this.message = message;
        this.quality = quality;
    }

    public static FaceEnrollResult success(String message, float quality) {
        return new FaceEnrollResult(true, message, quality);
    }

    public static FaceEnrollResult failure(String message) {
        return new FaceEnrollResult(false, message, 0f);
    }
}
