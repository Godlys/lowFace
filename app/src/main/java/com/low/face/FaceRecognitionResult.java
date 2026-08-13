package com.low.face;

/**
 * 识别结果
 */
public class FaceRecognitionResult {
    public final boolean matched;
    public final String userId;
    public final String userName;
    public final double score;
    public final String message;

    public FaceRecognitionResult(boolean matched, String userId, String userName, double score, String message) {
        this.matched = matched;
        this.userId = userId;
        this.userName = userName;
        this.score = score;
        this.message = message;
    }

    public static FaceRecognitionResult matched(String userId, String userName, double score) {
        return new FaceRecognitionResult(true, userId, userName, score,
            "匹配成功: " + userName + " (" + String.format("%.1f", score * 100) + "%)");
    }

    public static FaceRecognitionResult notMatched(double bestScore) {
        return new FaceRecognitionResult(false, null, null, bestScore,
            "未找到匹配 (最高相似度: " + String.format("%.1f", bestScore * 100) + "%)");
    }

    public static FaceRecognitionResult failure(String message) {
        return new FaceRecognitionResult(false, null, null, 0, message);
    }
}
