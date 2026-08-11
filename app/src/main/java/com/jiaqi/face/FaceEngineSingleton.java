package com.jiaqi.face;

import android.content.Context;

/**
 * FaceEngine 单例持有者
 * 用于在 Activity 之间共享引擎实例
 */
public class FaceEngineSingleton {
    private static FaceEngineManager instance;

    public static synchronized FaceEngineManager getInstance() {
        if (instance == null) {
            instance = new FaceEngineManager();
        }
        return instance;
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new FaceEngineManager();
        }
        if (!instance.isInitialized()) {
            instance.getExecutor().execute(() -> {
                instance.initialize(context.getApplicationContext());
            });
        }
    }

    public static synchronized void release() {
        if (instance != null) {
            instance.release();
            instance = null;
        }
    }
}
