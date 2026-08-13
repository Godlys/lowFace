package com.low.face;

/**
 * 人脸记录 - 内存存储模型
 */
public class FaceRecord {
    public final String userId;        // 工号
    public final String userName;      // 姓名
    public final byte[] embedding;     // 512维特征向量
    public final float qualityScore;   // 录入时的质量分数

    public FaceRecord(String userId, String userName, byte[] embedding, float qualityScore) {
        this.userId = userId;
        this.userName = userName;
        this.embedding = embedding;
        this.qualityScore = qualityScore;
    }
}
