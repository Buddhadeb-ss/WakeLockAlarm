package com.antigravity.riselock.mission

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

object ImageSimilarityMatcher {

    /**
     * Computes a 64-bit perceptual difference hash (dHash).
     * 1. Scales bitmap to 9x8 grayscale.
     * 2. Checks if left pixel > right pixel for 64 bit comparisons.
     */
    fun computeDHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash: Long = 0L

        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val leftPixel = scaled.getPixel(x, y)
                val rightPixel = scaled.getPixel(x + 1, y)

                val leftLuma = (Color.red(leftPixel) * 299 + Color.green(leftPixel) * 587 + Color.blue(leftPixel) * 114) / 1000
                val rightLuma = (Color.red(rightPixel) * 299 + Color.green(rightPixel) * 587 + Color.blue(rightPixel) * 114) / 1000

                if (leftLuma > rightLuma) {
                    hash = hash or (1L shl (y * 8 + x))
                }
            }
        }
        return hash
    }

    /**
     * Calculates similarity percentage (0 to 100) between two bitmaps.
     * Combines dHash Hamming distance and 16x16 block luminance correlation.
     */
    suspend fun calculateSimilarity(candidate: Bitmap, reference: Bitmap): Int = withContext(Dispatchers.Default) {
        val hash1 = computeDHash(candidate)
        val hash2 = computeDHash(reference)

        // Hamming distance between two 64-bit hashes
        val diffBits = java.lang.Long.bitCount(hash1 xor hash2)
        val hashSimilarity = ((64 - diffBits) / 64.0f) * 100f

        // Structural 16x16 luminance matrix comparison
        val size = 16
        val b1 = Bitmap.createScaledBitmap(candidate, size, size, true)
        val b2 = Bitmap.createScaledBitmap(reference, size, size, true)

        var totalError = 0.0f
        for (y in 0 until size) {
            for (x in 0 until size) {
                val p1 = b1.getPixel(x, y)
                val p2 = b2.getPixel(x, y)

                val l1 = (Color.red(p1) * 299 + Color.green(p1) * 587 + Color.blue(p1) * 114) / 1000f
                val l2 = (Color.red(p2) * 299 + Color.green(p2) * 587 + Color.blue(p2) * 114) / 1000f

                totalError += abs(l1 - l2) / 255.0f
            }
        }

        val lumaSimilarity = (1.0f - (totalError / (size * size))) * 100f
        val weightedScore = (hashSimilarity * 0.55f + lumaSimilarity * 0.45f).toInt()
        return@withContext weightedScore.coerceIn(0, 100)
    }

    /**
     * Saves a reference spot photo into internal app storage (sandboxed, secure).
     */
    suspend fun saveReferencePhoto(context: Context, spotName: String, bitmap: Bitmap): Pair<String, Long> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "reference_spots").apply { if (!exists()) mkdirs() }
        val filename = "spot_${System.currentTimeMillis()}.jpg"
        val file = File(dir, filename)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        val hash = computeDHash(bitmap)
        return@withContext Pair(file.absolutePath, hash)
    }

    /**
     * Loads a bitmap from local file path.
     */
    fun loadReferencePhoto(filePath: String): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }
}

