package com.jiaqi.face;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局内存存储 - 重启App后清空
 */
public class FaceStore {
    private static final List<FaceRecord> records = new ArrayList<>();

    /**
     * 添加人脸记录（同一工号会覆盖）
     */
    public static synchronized void add(FaceRecord record) {
        // 移除同工号的旧记录
        for (int i = records.size() - 1; i >= 0; i--) {
            if (records.get(i).userId.equals(record.userId)) {
                records.remove(i);
            }
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
