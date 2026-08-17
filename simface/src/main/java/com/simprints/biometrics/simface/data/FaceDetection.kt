package com.simprints.biometrics.simface.data

import android.graphics.Bitmap
import android.graphics.Rect
import com.simprints.biometrics.simface.detection.cropAlignFace
import com.simprints.biometrics.simface.detection.warpAlignFace

data class FaceDetection(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val absoluteBoundingBox: Rect,
    val yaw: Float,
    var roll: Float,
    val quality: Float,
    val landmarks: FacialLandmarks?,
    // 眼睛睁开概率 (0.0 ~ 1.0)，用于静默活体检测
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    // ML Kit 人脸跟踪 ID，用于时空锚定活体检测
    val trackingId: Int?,
) {
    /**
     * Crops the image according to the detections bounding box.
     *
     * NOTE: `croppedFaceImage` and `alignedFaceImage` are mutually exclusive operations.
     *
     * @param originalImage the original image
     */
    fun croppedFaceImage(originalImage: Bitmap): Bitmap = cropAlignFace(originalImage, absoluteBoundingBox)

    /**
     * Applies a warp transformation to the face image so that position of the landmarks corresponds to locations
     * expected by the face recognition model.
     *
     * In case of failure the whole pipeline returns the unchanged original image.
     *
     * NOTE: `croppedFaceImage` and `alignedFaceImage` are mutually exclusive operations.
     *
     * @param originalImage the original image
     */
    fun alignedFaceImage(originalImage: Bitmap): Bitmap = warpAlignFace(originalImage, landmarks)
}
