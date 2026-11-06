/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.android.internal.penguin.luminous.Luminous;
import com.android.internal.penguin.luminous.SpringInterpolator;

/**
 * ColorOS 17 "Living Flow Design" & "Polar Light Engine" tactile physics + O-Haptics engine.
 *
 * Combines hardware-accelerated RenderThread spring compression/rebound ({@link Luminous})
 * with 3D spatial touch-tilt, finger-tracking Specular Polar Light Glow, and O-Haptics
 * Crisp/Gentle envelope profiles.
 */
public final class LivingFlowTouchHelper {

    public static final float SCALE_CARD = 0.962f;
    public static final float SCALE_PILL = 0.935f;
    public static final float SCALE_ICON = 0.90f;

    private static final float DOWN_STIFFNESS = 440f;
    private static final float DOWN_DAMPING = 0.84f;
    private static final float UP_STIFFNESS = 285f;
    private static final float UP_DAMPING = 0.62f;
    private static final float MAX_TILT_DEG = 2.6f;

    public static final int OHAPTICS_OFF = 0;
    public static final int OHAPTICS_CRISP = 1;
    public static final int OHAPTICS_GENTLE = 2;

    private LivingFlowTouchHelper() {}

    /**
     * Triggers ColorOS 17 O-Haptics envelope feedback according to user's selected profile
     * (1 = Crisp / Mekanik Keskin, 2 = Gentle / Kadife Yumuşak, 0 = Off).
     */
    public static void triggerOHaptic(Context context, boolean isSliderTick) {
        if (context == null) return;
        try {
            int style = Settings.System.getIntForUser(
                    context.getContentResolver(), "blaze_ohaptics_style", OHAPTICS_CRISP, UserHandle.USER_CURRENT);
            if (style == OHAPTICS_OFF) return;
            Vibrator vib = context.getSystemService(Vibrator.class);
            if (vib == null || !vib.hasVibrator()) return;

            if (style == OHAPTICS_CRISP) {
                if (isSliderTick) {
                    vib.vibrate(VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.75f)
                            .compose());
                } else {
                    vib.vibrate(VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.90f)
                            .compose());
                }
            } else {
                // Gentle / Soft envelope
                if (isSliderTick) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
                } else {
                    vib.vibrate(VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.55f)
                            .compose());
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Attaches ColorOS 17 Flexible Feedback spring physics + Polar Light Touch Glow + 3D Tilt.
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void attach(final View target, final float pressedScale, final boolean enableTilt) {
        if (target == null) return;
        final float density = target.getResources().getDisplayMetrics().density;
        target.setCameraDistance(density * 4800f);
        final PolarLightGlowDrawable glowDrawable = new PolarLightGlowDrawable(density);

        target.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    Luminous.spring(v, Luminous.SCALE_X, pressedScale, DOWN_STIFFNESS, DOWN_DAMPING);
                    Luminous.spring(v, Luminous.SCALE_Y, pressedScale, DOWN_STIFFNESS, DOWN_DAMPING);
                    if (v.getWidth() > 0 && v.getHeight() > 0) {
                        glowDrawable.setBounds(0, 0, v.getWidth(), v.getHeight());
                        glowDrawable.updateTouchPoint(event.getX(), event.getY(), true);
                        v.getOverlay().remove(glowDrawable);
                        v.getOverlay().add(glowDrawable);
                        if (enableTilt) {
                            float nx = (event.getX() / v.getWidth()) * 2f - 1f;
                            float ny = (event.getY() / v.getHeight()) * 2f - 1f;
                            v.setRotationY(clamp(nx * MAX_TILT_DEG, -MAX_TILT_DEG, MAX_TILT_DEG));
                            v.setRotationX(clamp(-ny * MAX_TILT_DEG, -MAX_TILT_DEG, MAX_TILT_DEG));
                        }
                    }
                    triggerOHaptic(v.getContext(), true);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (v.getWidth() > 0 && v.getHeight() > 0) {
                        glowDrawable.updateTouchPoint(event.getX(), event.getY(), true);
                        if (enableTilt) {
                            float nx = (event.getX() / v.getWidth()) * 2f - 1f;
                            float ny = (event.getY() / v.getHeight()) * 2f - 1f;
                            v.setRotationY(clamp(nx * MAX_TILT_DEG, -MAX_TILT_DEG, MAX_TILT_DEG));
                            v.setRotationX(clamp(-ny * MAX_TILT_DEG, -MAX_TILT_DEG, MAX_TILT_DEG));
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                    triggerOHaptic(v.getContext(), false);
                    // Fallthrough to spring release
                case MotionEvent.ACTION_CANCEL: {
                    glowDrawable.updateTouchPoint(event.getX(), event.getY(), false);
                    v.getOverlay().remove(glowDrawable);
                    Luminous.spring(v, Luminous.SCALE_X, 1.0f, UP_STIFFNESS, UP_DAMPING);
                    Luminous.spring(v, Luminous.SCALE_Y, 1.0f, UP_STIFFNESS, UP_DAMPING);
                    if (enableTilt) {
                        v.animate()
                                .rotationX(0f)
                                .rotationY(0f)
                                .setDuration(260L)
                                .setInterpolator(new SpringInterpolator(260f, 0.75f, 0f))
                                .start();
                    }
                    break;
                }
            }
            return false;
        });
    }

    public static void attachCard(View cardView) {
        attach(cardView, SCALE_CARD, true);
    }

    public static void attachPill(View pillView) {
        attach(pillView, SCALE_PILL, false);
    }

    /**
     * Plays a staggered ColorOS 17 Laminar Flow entrance cascade on the given views.
     */
    public static void playEntranceCascade(long baseDelayMs, View... views) {
        if (views == null || views.length == 0) return;
        SpringInterpolator spring = new SpringInterpolator(250f, 0.78f, 0f);
        long duration = Math.min(spring.getDurationMillis(), 560L);

        for (int i = 0; i < views.length; i++) {
            final View v = views[i];
            if (v == null || v.getVisibility() != View.VISIBLE) continue;
            float offsetPx = v.getResources().getDisplayMetrics().density * (22f + i * 4f);
            v.setAlpha(0f);
            v.setTranslationY(offsetPx);
            v.setScaleX(0.965f);
            v.setScaleY(0.965f);
            long delay = baseDelayMs + (i * 45L);
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(delay)
                    .setDuration(duration)
                    .setInterpolator(spring)
                    .start();
        }
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * ColorOS 17 Polar Light Engine — renders a specular radial highlight that tracks the finger.
     */
    private static final class PolarLightGlowDrawable extends Drawable {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mRect = new RectF();
        private final float mRadiusPx;
        private final float mCornerPx;
        private float mTouchX;
        private float mTouchY;
        private boolean mActive;

        PolarLightGlowDrawable(float density) {
            mRadiusPx = 135f * density;
            mCornerPx = 22f * density;
        }

        void updateTouchPoint(float x, float y, boolean active) {
            mTouchX = x;
            mTouchY = y;
            mActive = active;
            invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            if (!mActive || getBounds().isEmpty()) return;
            mRect.set(getBounds());
            RadialGradient grad = new RadialGradient(
                    mTouchX, mTouchY, mRadiusPx,
                    new int[]{0x4438BDF8, 0x1A38BDF8, 0x0038BDF8},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP);
            mPaint.setShader(grad);
            canvas.drawRoundRect(mRect, mCornerPx, mCornerPx, mPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
