/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.updates;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Renders the sculptural 3D translucent crystal "17" hero numeral inspired by ColorOS 17,
 * with pearlescent white-to-lavender glass gradient, inner refraction sheen, and specular stroke.
 */
public class LivingFlowVersionEmblemView extends View {

    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String mVersionText = "17";
    private long mStartMs;

    public LivingFlowVersionEmblemView(Context context) {
        this(context, null);
    }

    public LivingFlowVersionEmblemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LivingFlowVersionEmblemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float d = getResources().getDisplayMetrics().density;
        Typeface tf = Typeface.create("sans-serif-medium", Typeface.BOLD);

        mShadowPaint.setTypeface(tf);
        mShadowPaint.setTextAlign(Paint.Align.CENTER);
        mShadowPaint.setColor(0x385865F2);

        mFillPaint.setTypeface(tf);
        mFillPaint.setTextAlign(Paint.Align.CENTER);

        mRimPaint.setTypeface(tf);
        mRimPaint.setTextAlign(Paint.Align.CENTER);
        mRimPaint.setStyle(Paint.Style.STROKE);
        mRimPaint.setStrokeWidth(1.3f * d);
        mStartMs = SystemClock.uptimeMillis();
    }

    public void setVersionText(String text) {
        mVersionText = text != null ? text : "17";
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float textSize = h * 0.84f;
        mShadowPaint.setTextSize(textSize);
        mFillPaint.setTextSize(textSize);
        mRimPaint.setTextSize(textSize);

        float shimmerPhase = (float) Math.sin((SystemClock.uptimeMillis() - mStartMs) / 900.0);
        float shiftX = shimmerPhase * (w * 0.08f);

        mFillPaint.setShader(new LinearGradient(
                w * 0.35f + shiftX, h * 0.08f,
                w * 0.65f - shiftX, h * 0.92f,
                new int[] {0xFFFFFFFF, 0xFFEAE8FF, 0xFFC5BFFF, 0xFFE2DEFF},
                new float[] {0f, 0.42f, 0.78f, 1f},
                Shader.TileMode.CLAMP));

        mRimPaint.setShader(new LinearGradient(
                w * 0.25f, h * 0.08f,
                w * 0.75f, h * 0.92f,
                new int[] {0xEEFFFFFF, 0x55FFFFFF, 0xAA9E98FF},
                new float[] {0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));

        Paint.FontMetrics fm = mFillPaint.getFontMetrics();
        float baseline = (h / 2f) - ((fm.ascent + fm.descent) / 2f);
        float cx = w / 2f;

        // Soft depth offset shadow
        canvas.drawText(mVersionText, cx, baseline + (h * 0.03f), mShadowPaint);
        // Pearlescent crystal glass fill
        canvas.drawText(mVersionText, cx, baseline, mFillPaint);
        // Fresnel specular rim
        canvas.drawText(mVersionText, cx, baseline, mRimPaint);

        if (isAttachedToWindow()) {
            postInvalidateOnAnimation();
        }
    }
}
