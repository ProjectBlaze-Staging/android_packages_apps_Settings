/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.settings.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.media.FaceDetector
import android.util.Log
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val TAG = "PortraitSegmenter"

/**
 * Edge-aware Salient Foreground & Portrait/Sky Matting Segmenter for Lockscreen Depth Wallpaper.
 *
 * Works offline with zero external TFLite dependencies:
 * 1. Preserves existing alpha channel if the input bitmap is already a transparent cutout PNG.
 * 2. Detects human faces via Android's built-in [FaceDetector] to anchor portrait head/shoulders.
 * 3. Models the upper sky/backdrop color distribution from top border seeds and runs a
 *    gradient-barrier connected-component flood fill from the top/side sky regions, followed by
 *    bilateral alpha feathering so subjects (mountains, towers, portraits, characters) cleanly
 *    overlap the lockscreen clock.
 */
class PortraitSegmenter(private val context: Context) {

    private var ready = false

    fun init() {
        ready = true
        Log.d(TAG, "PortraitSegmenter initialized (Salient Foreground + Sky Matting Engine)")
    }

    fun isReady(): Boolean = ready

    fun segment(source: Bitmap): Bitmap? {
        if (!ready || source.width <= 0 || source.height <= 0) return null
        try {
            val origW = source.width
            val origH = source.height

            // Downscale for fast segmentation (max 360px width), then upscale alpha mask
            val scale = min(1f, 360f / origW.toFloat())
            val w = max(64, (origW * scale).roundToInt())
            val h = max(64, (origH * scale).roundToInt())

            val small = if (w != origW || h != origH) {
                Bitmap.createScaledBitmap(source, w, h, true)
            } else {
                source.copy(Bitmap.Config.ARGB_8888, false)
            }

            val pixels = IntArray(w * h)
            small.getPixels(pixels, 0, w, 0, 0, w, h)

            // 1. Check if image already has a transparent background cutout
            var transparentCount = 0
            for (px in pixels) {
                if ((px ushr 24) < 128) transparentCount++
            }
            if (transparentCount > (w * h) / 10) {
                if (small !== source) small.recycle()
                return source.copy(Bitmap.Config.ARGB_8888, false)
            }

            // 2. Convert RGB to perceptual channels (L, R, G, B) & compute Sobel edge magnitude
            val rArr = IntArray(w * h)
            val gArr = IntArray(w * h)
            val bArr = IntArray(w * h)
            val luma = IntArray(w * h)
            for (i in pixels.indices) {
                val c = pixels[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                rArr[i] = r
                gArr[i] = g
                bArr[i] = b
                luma[i] = (r * 77 + g * 150 + b * 29) shr 8
            }

            val edgeMag = IntArray(w * h)
            for (y in 1 until h - 1) {
                val row = y * w
                for (x in 1 until w - 1) {
                    val idx = row + x
                    val gx = -luma[idx - w - 1] + luma[idx - w + 1] -
                            2 * luma[idx - 1] + 2 * luma[idx + 1] -
                            luma[idx + w - 1] + luma[idx + w + 1]
                    val gy = -luma[idx - w - 1] - 2 * luma[idx - w] - luma[idx - w + 1] +
                            luma[idx + w - 1] + 2 * luma[idx + w] + luma[idx + w + 1]
                    edgeMag[idx] = min(255, (abs(gx) + abs(gy)) shr 2)
                }
            }

            // 3. Optional FaceDetector anchor for portraits
            val subjectProtected = BooleanArray(w * h)
            try {
                val rgb565 = small.copy(Bitmap.Config.RGB_565, false)
                val faces = arrayOfNulls<FaceDetector.Face>(3)
                val found = FaceDetector(w, h, 3).findFaces(rgb565, faces)
                rgb565.recycle()
                val mid = PointF()
                for (i in 0 until found) {
                    val f = faces[i] ?: continue
                    f.getMidPoint(mid)
                    val ed = f.eyesDistance()
                    val cx = mid.x
                    val cy = mid.y
                    val rx = ed * 1.85f
                    val ryTop = ed * 2.1f
                    val ryBot = h.toFloat() // torso extends to bottom
                    for (y in max(0, (cy - ryTop).toInt()) until h) {
                        for (x in max(0, (cx - rx * 1.4f).toInt()) until min(w, (cx + rx * 1.4f).toInt())) {
                            val nx = (x - cx) / (if (y > cy + ed) rx * 1.45f else rx)
                            val ny = if (y <= cy) (y - cy) / ryTop else (y - cy) / ryBot
                            if (nx * nx + ny * ny <= 1.0f) {
                                subjectProtected[y * w + x] = true
                            }
                        }
                    }
                }
            } catch (ignored: Throwable) {}

            // 4. Region-growing sky/background flood fill from top border and upper side margins
            val isBg = BooleanArray(w * h)
            val visited = BooleanArray(w * h)
            val queue = ArrayDeque<Int>(w * h / 2)

            // Seed top row and upper left/right border columns
            val sideSeedMaxY = (h * 0.48f).toInt()
            for (x in 0 until w) {
                val idx = x
                if (!subjectProtected[idx]) {
                    visited[idx] = true
                    isBg[idx] = true
                    queue.add(idx)
                }
            }
            for (y in 1 until sideSeedMaxY) {
                val leftIdx = y * w
                val rightIdx = y * w + (w - 1)
                if (!visited[leftIdx] && !subjectProtected[leftIdx]) {
                    visited[leftIdx] = true
                    isBg[leftIdx] = true
                    queue.add(leftIdx)
                }
                if (!visited[rightIdx] && !subjectProtected[rightIdx]) {
                    visited[rightIdx] = true
                    isBg[rightIdx] = true
                    queue.add(rightIdx)
                }
            }

            val maxFloodY = (h * 0.78f).toInt()
            val dirs = intArrayOf(-1, 1, -w, w)

            while (!queue.isEmpty()) {
                val curr = queue.removeFirst()
                val cy = curr / w
                val cx = curr % w
                val cr = rArr[curr]
                val cg = gArr[curr]
                val cb = bArr[curr]

                for (d in dirs) {
                    val next = curr + d
                    if (next < 0 || next >= w * h) continue
                    val ny = next / w
                    val nx = next % w
                    if (abs(nx - cx) + abs(ny - cy) != 1) continue
                    if (ny >= maxFloodY || visited[next] || subjectProtected[next]) continue

                    val dr = abs(rArr[next] - cr)
                    val dg = abs(gArr[next] - cg)
                    val db = abs(bArr[next] - cb)
                    val stepDist = dr + dg + db
                    val edge = edgeMag[next]

                    // Stop at strong object boundaries or sharp color jumps
                    if (edge < 38 && stepDist < 42) {
                        visited[next] = true
                        isBg[next] = true
                        queue.add(next)
                    }
                }
            }

            // 5. Build initial alpha mask (0 = background sky, 255 = foreground subject)
            val alphaMask = IntArray(w * h)
            var fgAboveMidCount = 0
            val midY = (h * 0.45f).toInt()
            for (y in 0 until h) {
                val row = y * w
                for (x in 0 until w) {
                    val idx = row + x
                    val fg = !isBg[idx]
                    alphaMask[idx] = if (fg) 255 else 0
                    if (fg && y in (h / 10)..midY) {
                        fgAboveMidCount++
                    }
                }
            }

            // Fallback: if the wallpaper has a complex textured background where flood-fill
            // either stopped too early or ate everything, create a central salient dome mask
            // anchored on high-contrast/salient foreground structures in the lower/middle 75%.
            val minExpectedFg = (w * midY) / 18
            val maxExpectedFg = (w * midY) * 8 / 10
            if (fgAboveMidCount < minExpectedFg || fgAboveMidCount > maxExpectedFg) {
                for (y in 0 until h) {
                    val row = y * w
                    val ny = y.toFloat() / h.toFloat()
                    for (x in 0 until w) {
                        val nx = (x.toFloat() / w.toFloat()) - 0.5f
                        // Central peak curve around clock region (x near center, y from 0.16..1.0)
                        val archY = 0.18f + (nx * nx) * 0.85f
                        val inSubjectZone = ny >= archY
                        val hasDetail = edgeMag[row + x] > 18 || luma[row + x] < 200
                        alphaMask[row + x] = if (inSubjectZone && (ny > 0.36f || hasDetail)) 255 else 0
                    }
                }
            }

            // 6. 2-pass 3x3 box feathering on the alpha mask for smooth anti-aliased edges
            val smoothed = IntArray(w * h)
            for (y in 1 until h - 1) {
                val row = y * w
                for (x in 1 until w - 1) {
                    val idx = row + x
                    val sum = alphaMask[idx - w - 1] + alphaMask[idx - w] + alphaMask[idx - w + 1] +
                            alphaMask[idx - 1] + alphaMask[idx] * 2 + alphaMask[idx + 1] +
                            alphaMask[idx + w - 1] + alphaMask[idx + w] + alphaMask[idx + w + 1]
                    smoothed[idx] = sum / 10
                }
            }

            for (i in pixels.indices) {
                val a = smoothed[i].coerceIn(0, 255)
                pixels[i] = (a shl 24) or (pixels[i] and 0x00FFFFFF)
            }

            val outSmall = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            outSmall.setPixels(pixels, 0, w, 0, 0, w, h)
            if (small !== source) small.recycle()

            return if (w != origW || h != origH) {
                // Scale alpha mask back to full resolution and apply to crisp original pixels
                val scaledMaskBmp = Bitmap.createScaledBitmap(outSmall, origW, origH, true)
                outSmall.recycle()
                val outFull = source.copy(Bitmap.Config.ARGB_8888, true)
                val fullPixels = IntArray(origW * origH)
                val maskPixels = IntArray(origW * origH)
                outFull.getPixels(fullPixels, 0, origW, 0, 0, origW, origH)
                scaledMaskBmp.getPixels(maskPixels, 0, origW, 0, 0, origW, origH)
                scaledMaskBmp.recycle()
                for (i in fullPixels.indices) {
                    val a = maskPixels[i] ushr 24
                    fullPixels[i] = (a shl 24) or (fullPixels[i] and 0x00FFFFFF)
                }
                outFull.setPixels(fullPixels, 0, origW, 0, 0, origW, origH)
                outFull
            } else {
                outSmall
            }
        } catch (e: Exception) {
            Log.e(TAG, "Segmentation failed", e)
            return null
        }
    }

    fun release() {
        ready = false
        Log.d(TAG, "PortraitSegmenter released")
    }
}
