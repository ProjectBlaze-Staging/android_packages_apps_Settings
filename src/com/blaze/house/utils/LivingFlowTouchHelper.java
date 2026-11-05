/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.utils;

import android.annotation.SuppressLint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.android.internal.penguin.luminous.Luminous;
import com.android.internal.penguin.luminous.SpringInterpolator;

/**
 * ColorOS 17 "Living Flow Design" tactile physics engine (Flexible Feedback & Fluid Motion).
 *
 * Combines hardware-accelerated RenderThread spring compression/rebound ({@link Luminous})
 * with subtle 3D spatial touch-tilt and staggered Laminar entrance cascades.
 */
public final class LivingFlowTouchHelper {

    public static final float SCALE_CARD = 0.962f;
    public static final float SCALE_PILL = 0.935f;
    public static final float SCALE_ICON = 0.90f;

    private static final float DOWN_STIFFNESS = 440f;
    private static final float DOWN_DAMPING = 0.84f;
    private static final float UP_STIFFNESS = 285f;
    private static final float UP_DAMPING = 0.62f;
    private static final float MAX_TILT_DEG = 2.4f;

    private LivingFlowTouchHelper() {}

    /**
     * Attaches ColorOS 17 Flexible Feedback spring physics to any interactive View/Card.
     * Preserves existing OnClickListener and OnLongClickListener behavior.
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void attach(final View target, final float pressedScale, final boolean enableTilt) {
        if (target == null) return;
        final float density = target.getResources().getDisplayMetrics().density;
        target.setCameraDistance(density * 4800f);

        target.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    Luminous.spring(v, Luminous.SCALE_X, pressedScale, DOWN_STIFFNESS, DOWN_DAMPING);
                    Luminous.spring(v, Luminous.SCALE_Y, pressedScale, DOWN_STIFFNESS, DOWN_DAMPING);
                    if (enableTilt && v.getWidth() > 0 && v.getHeight() > 0) {
                        float nx = (event.getX() / v.getWidth()) * 2f - 1f;
                        float ny = (event.getY() / v.getHeight()) * 2f - 1f;
                        v.setRotationY(clamp(nx * MAX_TILT_DEG, -MAX_TILT_DEG, MAX_TILT_DEG));
                        v.setRotationX(clamp(-ny * MAX_TILT_DEG, -MAX_TILT_DEG, MAX_TILT_DEG));
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (enableTilt && v.getWidth() > 0 && v.getHeight() > 0) {
                        float nx = (event.getX() / v.getWidth()) * 2f - 1f;
                        float ny = (event.getY() / v.getHeight()) * 2f - 1f;
                        v.setRotationY(clamp(nx * MAX_TILT_DEG, -MAX_TILT_DEG, MAX_TILT_DEG));
                        v.setRotationX(clamp(-ny * MAX_TILT_DEG, -MAX_TILT_DEG, MAX_TILT_DEG));
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                    v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    // Fallthrough to spring release
                case MotionEvent.ACTION_CANCEL: {
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
            // Return false so standard click/long-click and ripple handlers still fire cleanly
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
}
