package com.low.face;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * FaceEnrollResult 单元测试
 */
public class FaceEnrollResultTest {

    @Test
    public void testSuccess() {
        FaceEnrollResult result = FaceEnrollResult.success("录入成功", 0.85f);

        assertTrue(result.success);
        assertEquals("录入成功", result.message);
        assertEquals(0.85f, result.quality, 0.01f);
    }

    @Test
    public void testFailure() {
        FaceEnrollResult result = FaceEnrollResult.failure("人脸质量不足");

        assertFalse(result.success);
        assertEquals("人脸质量不足", result.message);
        assertEquals(0.0f, result.quality, 0.01f);
    }
}
