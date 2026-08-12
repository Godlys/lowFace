package com.low.face;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * FaceRecognitionResult 单元测试
 */
public class FaceRecognitionResultTest {

    @Test
    public void testMatched() {
        FaceRecognitionResult result = FaceRecognitionResult.matched("001", "张三", 0.95);

        assertTrue(result.matched);
        assertEquals("001", result.userId);
        assertEquals("张三", result.userName);
        assertEquals(0.95, result.score, 0.01);
    }

    @Test
    public void testNotMatched() {
        FaceRecognitionResult result = FaceRecognitionResult.notMatched(0.3);

        assertFalse(result.matched);
        assertNull(result.userId);
        assertNull(result.userName);
        assertEquals(0.3, result.score, 0.01);
    }

    @Test
    public void testFailure() {
        FaceRecognitionResult result = FaceRecognitionResult.failure("请先录入人脸");

        assertFalse(result.matched);
        assertNull(result.userId);
        assertNull(result.userName);
        assertEquals(0.0, result.score, 0.01);
        assertEquals("请先录入人脸", result.message);
    }
}
