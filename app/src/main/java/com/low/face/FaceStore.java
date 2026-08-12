package com.low.face;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局内存存储 - 重启App后清空
 * 最大容量限制防止内存溢出
 */
public class FaceStore {
    private static final int MAX_RECORDS = 100; // 最大记录数
    private static final List<FaceRecord> records = new ArrayList<>();

    /**
     * 添加人脸记录（同一工号会覆盖）
     * 超过最大容量时移除最旧的记录
     */
    public static synchronized void add(FaceRecord record) {
        // 移除同工号的旧记录
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).userId.equals(record.userId)) {
                records.remove(i);
            }
        }

        // 检查容量限制
        while (records.size() >= MAX_RECORDS) {
            records.remove(0); // 移除最旧的记录
        }

        records.add(record);
    }

    /**
     * 清空所有记录
     */
    public static synchronized void clear() {
        records.clear();
    }

    /**
     * 获取记录数量
     */
    public static synchronized int count() {
        return records.size();
    }

    /**
     * 获取最大容量
     */
    public static int getMaxRecords() {
        return MAX_RECORDS;
    }

    /**
     * 获取所有特征向量用于比对
     */
    public static synchronized List<byte[]> getAllEmbeddings() {
        List<byte[]> embeddings = new ArrayList<>();
        for (FaceRecord record : records) {
            embeddings.add(record.embedding);
        }
        return embeddings;
    }

    /**
     * 根据索引获取记录
     */
    public static synchronized FaceRecord get(int index) {
        if (index >= 0 && index < records.size()) {
            return records.get(index);
        }
        return null;
    }

    /**
     * 获取所有记录（只读）
     */
    public static synchronized List<FaceRecord> getAllRecords() {
        return new ArrayList<>(records);
    }
}
