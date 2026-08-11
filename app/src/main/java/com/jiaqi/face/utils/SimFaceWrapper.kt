package com.jiaqi.face.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.simprints.biometrics.simface.SimFace
import com.simprints.biometrics.simface.SimFaceConfig
import com.simprints.biometrics.simface.data.FaceDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * SimFace Java 包装类
 * 解决 Java 与 Kotlin suspend 函数/data class 的互操作问题
 */
class SimFaceWrapper(private val context: Context) {

    companion object {
        private const val TAG = "SimFaceWrapper"
    }

    private var simFace: SimFace? = null

    /**
     * 初始化 SimFace（同步方法）
     */
    fun initialize() {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "[初始化] 开始创建 SimFace 实例")

        val configCreateTime = System.currentTimeMillis()
        val config = SimFaceConfig(
            applicationContext = context.applicationContext
        )
        Log.d(TAG, "[初始化] SimFaceConfig 创建耗时: ${System.currentTimeMillis() - configCreateTime}ms")

        val simFaceCreateTime = System.currentTimeMillis()
        simFace = SimFace().apply {
            initialize(config)
        }
        Log.d(TAG, "[初始化] SimFace 实例创建+初始化耗时: ${System.currentTimeMillis() - simFaceCreateTime}ms")

        Log.i(TAG, "[初始化] 完成，总耗时: ${System.currentTimeMillis() - startTime}ms")
    }

    /**
     * 是否已初始化
     */
    fun isInitialized(): Boolean = simFace != null

    /**
     * 检测人脸（同步方法）
     */
    fun detectFaces(bitmap: Bitmap): List<FaceDetection> {
        val sf = simFace ?: throw IllegalStateException("SimFace not initialized")

        val startTime = System.currentTimeMillis()
        val result = runBlocking {
            sf.detectFaceBlocking(bitmap)
        }
        val endTime = System.currentTimeMillis()
        Log.d(TAG, "[detectFaces] detectFaceBlocking 耗时: ${endTime - startTime}ms")

        return result
    }

    /**
     * 获取人脸质量
     */
    fun getFaceQuality(face: FaceDetection): Float = face.quality

    /**
     * 提取特征向量（接收已对齐的人脸 Bitmap）
     * 调用方负责先调用 face.alignedFaceImage() 获取对齐后的图像
     */
    fun getEmbedding(alignedFace: Bitmap): ByteArray? {
        val sf = simFace ?: throw IllegalStateException("SimFace not initialized")
        return try {
            val embedStartTime = System.currentTimeMillis()
            val embedding = sf.getEmbedding(alignedFace)
            Log.d(TAG, "[getEmbedding] getEmbedding 耗时: ${System.currentTimeMillis() - embedStartTime}ms")
            embedding
        } catch (e: Exception) {
            Log.e(TAG, "[getEmbedding] 失败", e)
            null
        }
    }

    /**
     * 1:N 比对
     * 返回 Pair<特征向量索引, 分数> 列表
     */
    fun identificationScore(probe: ByteArray, references: List<ByteArray>): List<Pair<Int, Double>> {
        val sf = simFace ?: throw IllegalStateException("SimFace not initialized")

        val startTime = System.currentTimeMillis()
        val scores = sf.identificationScore(probe, references)
        Log.d(TAG, "[identificationScore] 耗时: ${System.currentTimeMillis() - startTime}ms, 候选人数: ${references.size}")

        return scores.mapIndexed { index, pair ->
            Pair(index, pair.second)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        Log.i(TAG, "[释放] 释放 SimFace 资源")
        simFace?.release()
        simFace = null
    }
}
