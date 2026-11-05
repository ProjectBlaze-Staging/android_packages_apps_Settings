/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.updates;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * ColorOS 17 "Living Flow Design" 120Hz atmospheric canvas backdrop.
 *
 * Renders the deep cosmos indigo sky and two intersecting, continuously breathing 3D translucent
 * glass ribbons (Warm Amber-Gold petal + Iridescent Violet-Magenta petal) with Fresnel specular
 * rim highlights and interactive update-scan wave acceleration.
 */
public class LivingFlowBackdropView extends View {

    private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mAuraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mAmberPetalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mAmberRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mVioletPetalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mVioletRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSonarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCardBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path mAmberPath = new Path();
    private final Path mAmberRimPath = new Path();
    private final Path mVioletPath = new Path();
    private final Path mVioletRimPath = new Path();
    private final android.graphics.RectF mCardBorderRect = new android.graphics.RectF();

    private long mLastFrameMs;
    private float mPhase = 0f;
    private float mSpeedMultiplier = 1f;
    private float mTargetSpeedMultiplier = 1f;
    private float mScanPulsePhase = 0f;
    private boolean mScanning = false;

    private float mTouchOffsetX = 0f;
    private float mTouchOffsetY = 0f;
    private float mTargetTouchX = 0f;
    private float mTargetTouchY = 0f;

    public LivingFlowBackdropView(Context context) {
        this(context, null);
    }

    public LivingFlowBackdropView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LivingFlowBackdropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints() {
        float d = getResources().getDisplayMetrics().density;

        mAmberRimPaint.setStyle(Paint.Style.STROKE);
        mAmberRimPaint.setStrokeWidth(2.0f * d);
        mAmberRimPaint.setStrokeCap(Paint.Cap.ROUND);

        mVioletRimPaint.setStyle(Paint.Style.STROKE);
        mVioletRimPaint.setStrokeWidth(2.2f * d);
        mVioletRimPaint.setStrokeCap(Paint.Cap.ROUND);

        mSonarPaint.setStyle(Paint.Style.STROKE);
        mSonarPaint.setStrokeWidth(1.5f * d);

        mCardBorderPaint.setStyle(Paint.Style.STROKE);
        mCardBorderPaint.setStrokeWidth(1.2f * d);
    }

    public void setScanning(boolean scanning) {
        mScanning = scanning;
        mTargetSpeedMultiplier = scanning ? 3.4f : 1.0f;
        postInvalidateOnAnimation();
    }

    public boolean isScanning() {
        return mScanning;
    }

    public void pulseInteractiveWave(float normX, float normY) {
        mTargetTouchX = (normX - 0.5f) * 36f;
        mTargetTouchY = (normY - 0.5f) * 28f;
        postInvalidateOnAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;

        // Base cosmos night indigo gradient
        mBgPaint.setShader(new LinearGradient(
                0f, 0f, 0f, h,
                new int[] {0xFF080B1E, 0xFF161C48, 0xFF121636, 0xFF070914},
                new float[] {0f, 0.34f, 0.72f, 1f},
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        long now = SystemClock.uptimeMillis();
        if (mLastFrameMs == 0L) {
            mLastFrameMs = now;
        }
        float dt = Math.min((now - mLastFrameMs) / 1000f, 0.05f);
        mLastFrameMs = now;

        mSpeedMultiplier += (mTargetSpeedMultiplier - mSpeedMultiplier) * Math.min(1f, dt * 5f);
        mPhase += dt * 0.85f * mSpeedMultiplier;
        if (mScanning) {
            mScanPulsePhase += dt * 1.45f;
        }

        mTouchOffsetX += (mTargetTouchX - mTouchOffsetX) * Math.min(1f, dt * 6f);
        mTouchOffsetY += (mTargetTouchY - mTouchOffsetY) * Math.min(1f, dt * 6f);
        mTargetTouchX *= (1f - Math.min(1f, dt * 2.5f));
        mTargetTouchY *= (1f - Math.min(1f, dt * 2.5f));

        // 1. Base Cosmos Indigo Sky
        canvas.drawRect(0f, 0f, w, h, mBgPaint);

        float wave1 = (float) Math.sin(mPhase);
        float wave2 = (float) Math.cos(mPhase * 0.78f);
        float wave3 = (float) Math.sin(mPhase * 0.55f + 1.1f);
        boolean isLandscapeCard = w > h * 1.15f;

        // 2. Ambient Upper Indigo-Violet Halo
        float heroCx = w * (isLandscapeCard ? 0.34f : 0.50f) + wave2 * (w * 0.03f) + mTouchOffsetX;
        float heroCy = h * (isLandscapeCard ? 0.28f : 0.25f) + wave1 * (h * 0.015f) + mTouchOffsetY;
        float heroRadius = Math.max(w, h) * (isLandscapeCard ? 0.52f : 0.42f);
        mAuraPaint.setShader(new RadialGradient(
                heroCx, heroCy, heroRadius,
                new int[] {0x525865F2, 0x283B44B0, 0x00101432},
                new float[] {0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(heroCx, heroCy, heroRadius, mAuraPaint);

        // Optional Laminar Scan Sonar Rings when checking for updates
        if (mScanning || mSpeedMultiplier > 1.15f) {
            float scanIntensity = Math.min(1f, (mSpeedMultiplier - 1f) / 2.4f);
            for (int i = 0; i < 3; i++) {
                float p = (mScanPulsePhase + i * 0.33f) % 1f;
                float r = w * (0.14f + p * 0.42f);
                int alpha = Math.round((1f - p) * (1f - p) * 95f * scanIntensity);
                if (alpha > 2) {
                    mSonarPaint.setColor((alpha << 24) | 0x8AB4FF);
                    canvas.drawCircle(w * 0.5f, h * 0.24f, r, mSonarPaint);
                }
            }
        }

        // 3. Upper-Right Warm Amber-Gold-Coral 3D Glass Petal (ColorOS 17 Signature Ribbon)
        float ax0 = w * 1.12f;
        float ay0 = h * ((isLandscapeCard ? 0.06f : 0.31f) + 0.025f * wave1) + mTouchOffsetY * 0.6f;
        float ac1x = w * ((isLandscapeCard ? 0.62f : 0.64f) + 0.035f * wave2) + mTouchOffsetX;
        float ac1y = h * ((isLandscapeCard ? 0.16f : 0.38f) + 0.028f * wave3);
        float ac2x = w * ((isLandscapeCard ? 0.24f : 0.14f) + 0.025f * wave1);
        float ac2y = h * ((isLandscapeCard ? 0.48f : 0.58f) + 0.025f * wave2);
        float axMid = w * ((isLandscapeCard ? 0.18f : 0.12f) + 0.020f * wave3);
        float ayMid = h * ((isLandscapeCard ? 0.68f : 0.67f) + 0.020f * wave1);

        mAmberPath.reset();
        mAmberPath.moveTo(ax0, ay0);
        mAmberPath.cubicTo(ac1x, ac1y, ac2x, ac2y, axMid, ayMid);
        mAmberPath.cubicTo(
                w * 0.38f, h * ((isLandscapeCard ? 0.84f : 0.76f) + 0.020f * wave2),
                w * 0.78f, h * ((isLandscapeCard ? 0.76f : 0.70f) + 0.018f * wave1),
                w * 1.15f, h * (isLandscapeCard ? 0.56f : 0.58f));
        mAmberPath.close();

        mAmberPetalPaint.setShader(new LinearGradient(
                w * 0.95f, h * (isLandscapeCard ? 0.12f : 0.34f),
                w * 0.20f, h * (isLandscapeCard ? 0.78f : 0.72f),
                new int[] {0xF2FF6A1A, 0xECFF9A3C, 0xCCFFD26E, 0x384B2468},
                new float[] {0f, 0.38f, 0.74f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawPath(mAmberPath, mAmberPetalPaint);

        // Specular Fresnel Rim along the upper curve of the Amber Petal
        mAmberRimPath.reset();
        mAmberRimPath.moveTo(ax0, ay0);
        mAmberRimPath.cubicTo(ac1x, ac1y, ac2x, ac2y, axMid, ayMid);
        mAmberRimPaint.setShader(new LinearGradient(
                w * 0.95f, ay0, axMid, ayMid,
                new int[] {0xD8FFF3D1, 0xA8FFD080, 0x00FFFFFF},
                new float[] {0f, 0.65f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawPath(mAmberRimPath, mAmberRimPaint);

        // 4. Lower Foreground Iridescent Violet-Magenta-Peach Glass Petal
        float vx0 = -w * 0.14f;
        float vy0 = h * ((isLandscapeCard ? 0.74f : 0.76f) + 0.022f * wave2);
        float vc1x = w * ((isLandscapeCard ? 0.10f : 0.08f) + 0.025f * wave1) + mTouchOffsetX * 0.8f;
        float vc1y = h * ((isLandscapeCard ? 0.44f : 0.55f) + 0.028f * wave3) + mTouchOffsetY * 0.5f;
        float vc2x = w * ((isLandscapeCard ? 0.56f : 0.56f) + 0.030f * wave2);
        float vc2y = h * ((isLandscapeCard ? 0.48f : 0.57f) - 0.022f * wave1);
        float vxEnd = w * 0.98f;
        float vyEnd = h * ((isLandscapeCard ? 0.96f : 0.94f) + 0.016f * wave3);

        mVioletPath.reset();
        mVioletPath.moveTo(vx0, vy0);
        mVioletPath.cubicTo(vc1x, vc1y, vc2x, vc2y, vxEnd, vyEnd);
        mVioletPath.lineTo(w * 1.05f, h * 1.08f);
        mVioletPath.lineTo(-w * 0.14f, h * 1.08f);
        mVioletPath.close();

        mVioletPetalPaint.setShader(new LinearGradient(
                w * 0.25f, h * (isLandscapeCard ? 0.46f : 0.56f),
                w * 0.72f, h * 0.98f,
                new int[] {0xE6FFC49B, 0xD8D462FF, 0xC86B2CF5, 0x90181240},
                new float[] {0f, 0.28f, 0.68f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawPath(mVioletPath, mVioletPetalPaint);

        // Fresnel Glass Rim along the upper crest of the Violet Petal
        mVioletRimPath.reset();
        mVioletRimPath.moveTo(vx0, vy0);
        mVioletRimPath.cubicTo(vc1x, vc1y, vc2x, vc2y, vxEnd, vyEnd);
        mVioletRimPaint.setShader(new LinearGradient(
                0f, h * (isLandscapeCard ? 0.50f : 0.60f), w * 0.90f, h * 0.88f,
                new int[] {0x50FFFFFF, 0xEEFFF0FF, 0xAA9C72FF, 0x00FFFFFF},
                new float[] {0f, 0.35f, 0.75f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawPath(mVioletRimPath, mVioletRimPaint);

        if (isLandscapeCard) {
            float d = getResources().getDisplayMetrics().density;
            float r = 28f * d;
            float inset = 0.6f * d;
            mCardBorderRect.set(inset, inset, w - inset, h - inset);
            mCardBorderPaint.setShader(new LinearGradient(
                    0f, 0f, w, h,
                    new int[] {0x55FFFFFF, 0x28FFB76B, 0x45B388FF},
                    new float[] {0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(mCardBorderRect, r, r, mCardBorderPaint);
        }

        if (isAttachedToWindow()) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getWidth() > 0 && getHeight() > 0) {
            pulseInteractiveWave(event.getX() / getWidth(), event.getY() / getHeight());
        }
        return super.onTouchEvent(event);
    }
}
