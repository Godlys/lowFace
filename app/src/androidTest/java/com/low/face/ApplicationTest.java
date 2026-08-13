package com.low.face;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * 仪器测试 - 验证应用上下文
 */
@RunWith(AndroidJUnit4.class)
public class ApplicationTest {

    @Test
    public void useAppContext() {
        // 验证应用包名正确
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.low.face", appContext.getPackageName());
    }
}
