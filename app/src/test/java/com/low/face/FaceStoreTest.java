package com.low.face;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * FaceStore 单元测试
 */
public class FaceStoreTest {

    @Test
    public void testAddAndCount() {
        // 清空
        FaceStore.clear();

        // 初始数量为 0
        assertEquals(0, FaceStore.count());

        // 添加一条记录
        byte[] embedding = new byte[512];
        FaceRecord record = new FaceRecord("001", "张三", embedding, 0.8f);
        FaceStore.add(record);

        assertEquals(1, FaceStore.count());

        // 清空
        FaceStore.clear();
        assertEquals(0, FaceStore.count());
    }

    @Test
    public void testGet() {
        FaceStore.clear();

        byte[] embedding = new byte[512];
        FaceRecord record = new FaceRecord("002", "李四", embedding, 0.9f);
        FaceStore.add(record);

        // 正常获取
        FaceRecord retrieved = FaceStore.get(0);
        assertNotNull(retrieved);
        assertEquals("002", retrieved.userId);
        assertEquals("李四", retrieved.userName);

        // 越界返回 null
        FaceRecord invalid = FaceStore.get(100);
        assertNull(invalid);

        FaceStore.clear();
    }

    @Test
    public void testGetAllEmbeddings() {
        FaceStore.clear();

        // 空列表
        assertTrue(FaceStore.getAllEmbeddings().isEmpty());

        // 添加记录
        byte[] embedding1 = new byte[512];
        byte[] embedding2 = new byte[512];
        embedding1[0] = 1;
        embedding2[0] = 2;

        FaceStore.add(new FaceRecord("001", "张三", embedding1, 0.8f));
        FaceStore.add(new FaceRecord("002", "李四", embedding2, 0.9f));

        assertEquals(2, FaceStore.getAllEmbeddings().size());

        FaceStore.clear();
    }
}
