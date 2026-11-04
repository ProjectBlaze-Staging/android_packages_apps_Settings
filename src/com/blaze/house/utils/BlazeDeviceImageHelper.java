/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.settings.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

/**
 * Smart device render resolver, GSI brand-fallback engine, custom gallery image manager,
 * and ProjectBlaze "Updates" morphing capsule animator for About Phone & BlazeHouse.
 */
public final class BlazeDeviceImageHelper {

    private static final String TAG = "BlazeDeviceImageHelper";

    public static final String KEY_DEVICE_IMAGE_MODE = "blaze_about_device_image_mode";
    public static final String KEY_CUSTOM_DEVICE_NAME = "blaze_about_custom_device_name";
    private static final String CUSTOM_IMAGE_FILENAME = "blaze_custom_device_render.png";
    private static final String EXTERNAL_FALLBACK_PATH = "/sdcard/BlazeHouse/custom_device.png";

    public static final String MODE_AUTO = "auto";
    public static final String MODE_PIXEL_6A = "pixel_6a";
    public static final String MODE_REDMI_NOTE11 = "redmi_note11";
    public static final String MODE_SAMSUNG_S10 = "samsung_s10";
    public static final String MODE_SAMSUNG_S21 = "samsung_s21";
    public static final String MODE_CUSTOM = "custom";
    public static final String MODE_NONE = "none";

    private BlazeDeviceImageHelper() {}

    public static String getCurrentMode(Context context) {
        String mode = Settings.Global.getString(
                context.getContentResolver(), KEY_DEVICE_IMAGE_MODE);
        return TextUtils.isEmpty(mode) ? MODE_AUTO : mode;
    }

    public static void setMode(Context context, String mode) {
        Settings.Global.putString(
                context.getContentResolver(), KEY_DEVICE_IMAGE_MODE, mode);
    }

    /**
     * Resolves the best matching built-in device mode using exact model/codename match first,
     * followed by Brand Fallback for GSI installations (e.g., Galaxy S9 -> Samsung S21 render).
     */
    public static String resolveAutoMode() {
        String vendorModel = SystemProperties.get("ro.product.vendor.model", "");
        String vendorDevice = SystemProperties.get("ro.product.vendor.device", "");
        String vendorBrand = SystemProperties.get("ro.product.vendor.brand", "");
        String vendorManufacturer = SystemProperties.get("ro.product.vendor.manufacturer", "");
        String sysModel = SystemProperties.get("ro.product.model", Build.MODEL);
        String sysDevice = SystemProperties.get("ro.product.device", Build.DEVICE);
        String sysBrand = SystemProperties.get("ro.product.brand", Build.BRAND);
        String socManuf = SystemProperties.get("ro.soc.manufacturer", "");
        String platform = SystemProperties.get("ro.board.platform", "");

        String combinedModel = (vendorModel + " " + vendorDevice + " " + sysModel + " " + sysDevice)
                .toLowerCase(Locale.ROOT);
        String combinedBrand = (vendorBrand + " " + vendorManufacturer + " " + sysBrand + " "
                + socManuf + " " + platform).toLowerCase(Locale.ROOT);

        // 1. Exact / Series Custom Device List Match
        if (combinedModel.contains("s21")
                || combinedModel.contains("sm-g991")
                || combinedModel.contains("sm-g996")
                || combinedModel.contains("sm-g998")
                || combinedModel.contains("o1s")
                || combinedModel.contains("t2s")
                || combinedModel.contains("p3s")) {
            return MODE_SAMSUNG_S21;
        }
        if (combinedModel.contains("s10")
                || combinedModel.contains("sm-g970")
                || combinedModel.contains("sm-g973")
                || combinedModel.contains("sm-g975")
                || combinedModel.contains("beyond0")
                || combinedModel.contains("beyond1")
                || combinedModel.contains("beyond2")
                || combinedModel.contains("beyondx")) {
            return MODE_SAMSUNG_S10;
        }
        if (combinedModel.contains("note 11")
                || combinedModel.contains("note11")
                || combinedModel.contains("spes")
                || combinedModel.contains("2201117")) {
            return MODE_REDMI_NOTE11;
        }
        if (combinedModel.contains("pixel 6a")
                || combinedModel.contains("bluejay")) {
            return MODE_PIXEL_6A;
        }

        // 2. GSI Brand Fallback (Same brand's custom device render greets the user)
        if (combinedBrand.contains("samsung")
                || combinedModel.contains("sm-")
                || combinedModel.contains("galaxy")
                || combinedBrand.contains("exynos")
                || combinedBrand.contains("s5e")) {
            // For older S8/S9/Note9 series, S10 is closest; for newer or general Samsung, S21+
            if (combinedModel.contains("s8") || combinedModel.contains("s9")
                    || combinedModel.contains("g960") || combinedModel.contains("g965")
                    || combinedModel.contains("starlte") || combinedModel.contains("star2lte")) {
                return MODE_SAMSUNG_S21;
            }
            return MODE_SAMSUNG_S21;
        }
        if (combinedBrand.contains("xiaomi")
                || combinedBrand.contains("redmi")
                || combinedBrand.contains("poco")
                || combinedModel.contains("redmi")
                || combinedModel.contains("poco")
                || combinedModel.contains("mi ")) {
            return MODE_REDMI_NOTE11;
        }
        if (combinedBrand.contains("google")
                || combinedModel.contains("pixel")
                || combinedBrand.contains("tensor")
                || combinedBrand.contains("gs101")
                || combinedBrand.contains("gs201")
                || combinedBrand.contains("zuma")) {
            return MODE_PIXEL_6A;
        }

        // Default flagship render for generic GSI / DSU environments
        return MODE_PIXEL_6A;
    }

    public static int getDrawableResForMode(String resolvedMode) {
        switch (resolvedMode) {
            case MODE_REDMI_NOTE11:
                return R.drawable.blaze_device_redmi_note11;
            case MODE_SAMSUNG_S10:
                return R.drawable.blaze_device_samsung_s10;
            case MODE_SAMSUNG_S21:
                return R.drawable.blaze_device_samsung_s21;
            case MODE_PIXEL_6A:
            default:
                return R.drawable.blaze_device_pixel_6a;
        }
    }

    public static String getModeDisplayName(String mode) {
        if (mode == null) mode = MODE_AUTO;
        switch (mode) {
            case MODE_AUTO:
                String autoTarget = resolveAutoMode();
                return "Otomatik Algıla (" + getShortModelName(autoTarget) + ")";
            case MODE_PIXEL_6A:
                return "Google Pixel 6a";
            case MODE_REDMI_NOTE11:
                return "Xiaomi Redmi Note 11";
            case MODE_SAMSUNG_S10:
                return "Samsung Galaxy S10 / S10+";
            case MODE_SAMSUNG_S21:
                return "Samsung Galaxy S21 / S21+ / S21 Ultra";
            case MODE_CUSTOM:
                return "Özel Görsel (Galeriden Seçilen PNG)";
            case MODE_NONE:
                return "Gizle (Görsel Yok)";
            default:
                return "Otomatik Algıla";
        }
    }

    private static String getShortModelName(String mode) {
        switch (mode) {
            case MODE_REDMI_NOTE11:
                return "Redmi Note 11";
            case MODE_SAMSUNG_S10:
                return "Galaxy S10 / S10+";
            case MODE_SAMSUNG_S21:
                return "Galaxy S21 Serisi";
            case MODE_PIXEL_6A:
            default:
                return "Pixel 6a";
        }
    }

    /**
     * Applies the configured device render to the About Phone hero ImageView.
     */
    public static void bindDeviceRender(Context context, ImageView renderView) {
        if (context == null || renderView == null) return;
        String mode = getCurrentMode(context);
        if (MODE_NONE.equals(mode)) {
            renderView.setVisibility(View.GONE);
            return;
        }

        if (MODE_CUSTOM.equals(mode)) {
            Bitmap customBmp = loadCustomBitmap(context);
            if (customBmp != null) {
                renderView.setImageBitmap(customBmp);
                renderView.setVisibility(View.VISIBLE);
                return;
            }
            // Fallback to auto if custom file was deleted
            mode = MODE_AUTO;
        }

        String effectiveMode = MODE_AUTO.equals(mode) ? resolveAutoMode() : mode;
        renderView.setImageResource(getDrawableResForMode(effectiveMode));
        renderView.setVisibility(View.VISIBLE);
    }

    /**
     * Saves a user-picked gallery Uri as an optimized transparent PNG for the About Phone render.
     */
    public static boolean saveCustomImageFromUri(Context context, Uri uri) {
        if (context == null || uri == null) return false;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, bounds);
            }
            int sampleSize = 1;
            int maxDim = Math.max(bounds.outWidth, bounds.outHeight);
            while (maxDim / sampleSize > 720) {
                sampleSize *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap decoded;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                decoded = BitmapFactory.decodeStream(is, null, opts);
            }
            if (decoded == null) return false;

            File outFile = new File(context.getFilesDir(), CUSTOM_IMAGE_FILENAME);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                decoded.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            setMode(context, MODE_CUSTOM);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save custom device render", e);
            return false;
        }
    }

    private static Bitmap loadCustomBitmap(Context context) {
        try {
            File internalFile = new File(context.getFilesDir(), CUSTOM_IMAGE_FILENAME);
            if (internalFile.exists() && internalFile.length() > 0) {
                return BitmapFactory.decodeFile(internalFile.getAbsolutePath());
            }
            File extFile = new File(EXTERNAL_FALLBACK_PATH);
            if (extFile.exists() && extFile.length() > 0) {
                return BitmapFactory.decodeFile(extFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.w(TAG, "Error loading custom device bitmap", e);
        }
        return null;
    }

    /**
     * Starts the signature ProjectBlaze "Updates" capsule morphing wipe animation
     * (reproducing the concept GIF/video in crisp 120Hz native vector motion).
     */
    public static void startUpdatesPillAnimation(
            final View pillContainer,
            final View whiteMorphOverlay,
            final View flameIcon,
            final View updatesText) {
        if (pillContainer == null || whiteMorphOverlay == null || flameIcon == null) return;

        pillContainer.post(new Runnable() {
            @Override
            public void run() {
                if (!pillContainer.isAttachedToWindow()) return;
                final float width = pillContainer.getWidth() > 0
                        ? pillContainer.getWidth()
                        : pillContainer.getResources().getDisplayMetrics().density * 150f;

                Object tag = pillContainer.getTag();
                if (tag instanceof Animator) {
                    ((Animator) tag).cancel();
                }

                // Initial state: White pill covering the dark capsule, Blaze flame centered
                whiteMorphOverlay.setTranslationX(0f);
                whiteMorphOverlay.setAlpha(1f);
                flameIcon.setScaleX(0.65f);
                flameIcon.setScaleY(0.65f);

                // Phase 1: Spring pop-in of ProjectBlaze Flame (0 -> 550ms)
                ObjectAnimator flameScaleX = ObjectAnimator.ofFloat(flameIcon, View.SCALE_X, 0.65f, 1.0f);
                ObjectAnimator flameScaleY = ObjectAnimator.ofFloat(flameIcon, View.SCALE_Y, 0.65f, 1.0f);
                AnimatorSet phase1Pop = new AnimatorSet();
                phase1Pop.playTogether(flameScaleX, flameScaleY);
                phase1Pop.setDuration(550L);
                phase1Pop.setInterpolator(new OvershootInterpolator(1.8f));

                // Phase 2: Hold ProjectBlaze Flame on white pill (650ms)
                ObjectAnimator phase2Hold = ObjectAnimator.ofFloat(flameIcon, View.ALPHA, 1f, 1f);
                phase2Hold.setDuration(650L);

                // Phase 3: Wipe white overlay from Left -> Right to reveal "Updates" (720ms)
                ObjectAnimator wipeOutX = ObjectAnimator.ofFloat(whiteMorphOverlay, View.TRANSLATION_X, 0f, width);
                ObjectAnimator flameSpinOut = ObjectAnimator.ofFloat(flameIcon, View.SCALE_X, 1.0f, 0.8f);
                AnimatorSet phase3WipeReveal = new AnimatorSet();
                if (updatesText != null) {
                    ObjectAnimator textScaleX = ObjectAnimator.ofFloat(updatesText, View.SCALE_X, 0.90f, 1.0f);
                    ObjectAnimator textScaleY = ObjectAnimator.ofFloat(updatesText, View.SCALE_Y, 0.90f, 1.0f);
                    ObjectAnimator textAlpha = ObjectAnimator.ofFloat(updatesText, View.ALPHA, 0.4f, 1.0f);
                    phase3WipeReveal.playTogether(wipeOutX, flameSpinOut, textScaleX, textScaleY, textAlpha);
                } else {
                    phase3WipeReveal.playTogether(wipeOutX, flameSpinOut);
                }
                phase3WipeReveal.setDuration(720L);
                phase3WipeReveal.setInterpolator(new AccelerateDecelerateInterpolator());

                // Phase 4: Hold "Updates" dark capsule (2600ms)
                ObjectAnimator phase4HoldUpdates = ObjectAnimator.ofFloat(
                        whiteMorphOverlay, View.TRANSLATION_X, width, width);
                phase4HoldUpdates.setDuration(2600L);

                // Phase 5: Wipe white ProjectBlaze capsule back in from Left -> Center (650ms)
                ObjectAnimator wipeBackIn = ObjectAnimator.ofFloat(
                        whiteMorphOverlay, View.TRANSLATION_X, -width, 0f);
                ObjectAnimator flameResetX = ObjectAnimator.ofFloat(flameIcon, View.SCALE_X, 0.75f, 1.0f);
                ObjectAnimator flameResetY = ObjectAnimator.ofFloat(flameIcon, View.SCALE_Y, 0.75f, 1.0f);
                AnimatorSet phase5Return = new AnimatorSet();
                phase5Return.playTogether(wipeBackIn, flameResetX, flameResetY);
                phase5Return.setDuration(650L);
                phase5Return.setInterpolator(new AccelerateDecelerateInterpolator());

                final AnimatorSet fullCycle = new AnimatorSet();
                fullCycle.playSequentially(
                        phase1Pop,
                        phase2Hold,
                        phase3WipeReveal,
                        phase4HoldUpdates,
                        phase5Return
                );
                fullCycle.addListener(new AnimatorListenerAdapter() {
                    private boolean mCanceled = false;

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        mCanceled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!mCanceled && pillContainer.isAttachedToWindow()) {
                            pillContainer.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (pillContainer.isAttachedToWindow()) {
                                        fullCycle.start();
                                    }
                                }
                            }, 150L);
                        }
                    }
                });

                pillContainer.setTag(fullCycle);
                fullCycle.start();
            }
        });
    }

    /**
     * Shows a selection dialog for choosing the About Phone device render (or launching the gallery picker).
     */
    public static void showDeviceImagePickerDialog(
            final Context context,
            final Runnable onModeChanged,
            final Runnable onPickCustomImageRequested) {
        final String[] modes = new String[] {
            MODE_AUTO,
            MODE_PIXEL_6A,
            MODE_REDMI_NOTE11,
            MODE_SAMSUNG_S10,
            MODE_SAMSUNG_S21,
            MODE_CUSTOM,
            MODE_NONE
        };
        final CharSequence[] labels = new CharSequence[] {
            getModeDisplayName(MODE_AUTO),
            "Google Pixel 6a",
            "Xiaomi Redmi Note 11",
            "Samsung Galaxy S10 / S10+",
            "Samsung Galaxy S21 / S21+ / S21 Ultra",
            "Özel Görsel Seç (Galeriden PNG / WebP)...",
            "Gizle (Cihaz Görseli Gösterme)"
        };

        String current = getCurrentMode(context);
        int checkedItem = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(current)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle("Telefon Hakkında Cihaz Görseli")
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    String selected = modes[which];
                    dialog.dismiss();
                    if (MODE_CUSTOM.equals(selected) && onPickCustomImageRequested != null) {
                        onPickCustomImageRequested.run();
                    } else {
                        setMode(context, selected);
                        if (onModeChanged != null) {
                            onModeChanged.run();
                        }
                        Toast.makeText(context,
                                "Cihaz görseli güncellendi: " + getModeDisplayName(selected),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
