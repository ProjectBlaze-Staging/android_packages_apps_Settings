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
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
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

    // Hero Card Background Modes (About Phone top card behind Device Name & Updates pill)
    public static final String KEY_HERO_BG_MODE = "blaze_about_hero_bg_mode";
    private static final String CUSTOM_HERO_BG_FILENAME = "blaze_custom_hero_bg.png";

    public static final String HERO_BG_LIVING_FLOW = "living_flow";
    public static final String HERO_BG_WALLPAPER = "wallpaper";
    public static final String HERO_BG_CUSTOM = "custom";
    public static final String HERO_BG_TERRACOTTA = "terracotta";

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
     * When hidden (MODE_NONE), renders a subtle glass restore badge so the user can always tap
     * to re-enable or change the device image directly from About Phone.
     */
    public static void bindDeviceRender(Context context, ImageView renderView) {
        if (context == null || renderView == null) return;
        String mode = getCurrentMode(context);
        if (MODE_NONE.equals(mode)) {
            renderView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            renderView.setImageBitmap(createHiddenDeviceRestoreBadge(context));
            renderView.setAlpha(0.85f);
            renderView.setVisibility(View.VISIBLE);
            return;
        }
        renderView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        renderView.setAlpha(1.0f);

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

    private static Bitmap createHiddenDeviceRestoreBadge(Context context) {
        float d = context.getResources().getDisplayMetrics().density;
        int w = Math.round(108f * d);
        int h = Math.round(40f * d);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF pill = new RectF(2f * d, 4f * d, w - 2f * d, h - 4f * d);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x40000000);
        c.drawRoundRect(pill, 18f * d, 18f * d, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.2f * d);
        p.setColor(0x66FFFFFF);
        c.drawRoundRect(pill, 18f * d, 18f * d, p);

        p.setStyle(Paint.Style.FILL);
        p.setColor(0xEEFFFFFF);
        p.setTextSize(11.5f * d);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        Paint.FontMetrics fm = p.getFontMetrics();
        float textY = pill.centerY() - (fm.ascent + fm.descent) * 0.5f;
        c.drawText("+ Cihaz Görseli", pill.centerX(), textY, p);
        return bmp;
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

    // =========================================================================================
    // Hero Card Background Customization (Living Flow / Wallpaper / Cropped Custom / Terracotta)
    // =========================================================================================

    public static String getCurrentHeroBgMode(Context context) {
        if (context == null) return HERO_BG_LIVING_FLOW;
        String mode = Settings.Global.getString(
                context.getContentResolver(), KEY_HERO_BG_MODE);
        return TextUtils.isEmpty(mode) ? HERO_BG_LIVING_FLOW : mode;
    }

    public static void setHeroBgMode(Context context, String mode) {
        if (context == null) return;
        Settings.Global.putString(
                context.getContentResolver(), KEY_HERO_BG_MODE, mode);
    }

    public static String getHeroBgModeDisplayName(String mode) {
        if (mode == null) mode = HERO_BG_LIVING_FLOW;
        switch (mode) {
            case HERO_BG_WALLPAPER:
                return "Ana Ekran Duvar Kağıdı (Otomatik Senkronize)";
            case HERO_BG_CUSTOM:
                return "Özel Görsel (Kırpılmış ve Boyutlandırılmış)";
            case HERO_BG_TERRACOTTA:
                return "Klasik Blaze Terracotta (Düz Kırmızı)";
            case HERO_BG_LIVING_FLOW:
            default:
                return "Canlı 3D Cam Dalgaları (Akıcı Arka Plan)";
        }
    }

    public static void bindHeroCardBackground(
            final Context context,
            final View heroCard,
            final View clipContainer,
            final View livingFlowView,
            final ImageView customBgImg,
            final View scrimView) {
        if (context == null || heroCard == null) return;

        final float density = context.getResources().getDisplayMetrics().density;
        final float radiusPx = 28f * density;
        ViewOutlineProvider roundedOutline = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (view.getWidth() > 0 && view.getHeight() > 0) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
                }
            }
        };
        heroCard.setOutlineProvider(roundedOutline);
        heroCard.setClipToOutline(true);
        if (clipContainer != null) {
            clipContainer.setOutlineProvider(roundedOutline);
            clipContainer.setClipToOutline(true);
        }

        String mode = getCurrentHeroBgMode(context);

        if (HERO_BG_TERRACOTTA.equals(mode)) {
            if (livingFlowView != null) livingFlowView.setVisibility(View.GONE);
            if (customBgImg != null) customBgImg.setVisibility(View.GONE);
            if (scrimView != null) scrimView.setVisibility(View.GONE);
            return;
        }

        if (HERO_BG_CUSTOM.equals(mode) && customBgImg != null) {
            Bitmap customBmp = loadCustomHeroBgBitmap(context);
            if (customBmp != null) {
                if (livingFlowView != null) livingFlowView.setVisibility(View.GONE);
                customBgImg.setImageBitmap(customBmp);
                customBgImg.setVisibility(View.VISIBLE);
                applyHeroScrim(scrimView, radiusPx, density);
                return;
            }
            mode = HERO_BG_LIVING_FLOW;
        }

        if (HERO_BG_WALLPAPER.equals(mode) && customBgImg != null) {
            Drawable wpDrawable = loadSystemWallpaperDrawable(context);
            if (wpDrawable != null) {
                if (livingFlowView != null) livingFlowView.setVisibility(View.GONE);
                customBgImg.setImageDrawable(wpDrawable);
                customBgImg.setVisibility(View.VISIBLE);
                applyHeroScrim(scrimView, radiusPx, density);
                return;
            }
            mode = HERO_BG_LIVING_FLOW;
        }

        // Default: ColorOS 17 Living Flow 3D Glass Backdrop
        if (livingFlowView != null) {
            livingFlowView.setVisibility(View.VISIBLE);
        }
        if (customBgImg != null) {
            customBgImg.setVisibility(View.GONE);
        }
        if (scrimView != null) {
            scrimView.setVisibility(View.GONE);
        }
    }

    private static void applyHeroScrim(View scrimView, float radiusPx, float density) {
        if (scrimView == null) return;
        GradientDrawable scrim = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0xB8080B18, 0x700E1326, 0x38121830});
        scrim.setCornerRadius(radiusPx);
        scrim.setStroke(Math.max(1, Math.round(1.1f * density)), 0x48FFFFFF);
        scrimView.setBackground(scrim);
        scrimView.setVisibility(View.VISIBLE);
    }

    private static Drawable loadSystemWallpaperDrawable(Context context) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(context);
            if (wm != null) {
                Drawable d = wm.getDrawable();
                if (d != null) return d;
                return wm.getBuiltInDrawable(1080, 480, true, 0.5f, 0.5f);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Unable to read live WallpaperManager drawable, using built-in", t);
            try {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                if (wm != null) {
                    return wm.getBuiltInDrawable(1080, 480, true, 0.5f, 0.5f);
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static Bitmap loadCustomHeroBgBitmap(Context context) {
        try {
            File file = new File(context.getFilesDir(), CUSTOM_HERO_BG_FILENAME);
            if (file.exists() && file.length() > 0) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load custom hero bg bitmap", e);
        }
        return null;
    }

    public static void showHeroCardBgPickerDialog(
            final Context context,
            final Runnable onModeChanged,
            final Runnable onPickCustomHeroBgRequested) {
        if (context == null) return;
        final String[] modes = new String[] {
            HERO_BG_LIVING_FLOW,
            HERO_BG_WALLPAPER,
            HERO_BG_CUSTOM,
            HERO_BG_TERRACOTTA
        };
        final CharSequence[] labels = new CharSequence[] {
            "Canlı 3D Cam Dalgaları (Akıcı Arka Plan)",
            "Ana Ekran Duvar Kağıdını Kullan (Otomatik Senkronize)",
            "Özel Görsel Seç, Kırp ve Boyutlandır (Galeriden)...",
            "Klasik Blaze Terracotta (Düz Kırmızı)"
        };

        String current = getCurrentHeroBgMode(context);
        int checkedItem = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(current)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle("Cihaz Hakkında Kart Arka Planı")
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    String selected = modes[which];
                    dialog.dismiss();
                    if (HERO_BG_CUSTOM.equals(selected) && onPickCustomHeroBgRequested != null) {
                        onPickCustomHeroBgRequested.run();
                    } else {
                        setHeroBgMode(context, selected);
                        if (onModeChanged != null) {
                            onModeChanged.run();
                        }
                        Toast.makeText(context,
                                "Kart arka planı güncellendi: " + getHeroBgModeDisplayName(selected),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Cihaz Görseli Seç...", (dialog, which) -> {
                    showDeviceImagePickerDialog(context, onModeChanged, null);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Opens an interactive touch Pan + Pinch-Zoom + SeekBar Crop & Resize dialog locked to the
     * About Phone Hero Card aspect ratio (2.35 : 1) so the user can frame any photo accurately.
     */
    public static void showHeroImageCropDialog(
            final Context context,
            final Uri sourceUri,
            final Runnable onSaved) {
        if (context == null || sourceUri == null) return;
        final Bitmap sourceBitmap = decodeSampledBitmapFromUri(context, sourceUri, 1920);
        if (sourceBitmap == null) {
            Toast.makeText(context, "Görsel okunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }

        final float d = context.getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(18f * d);
        root.setPadding(pad, Math.round(12f * d), pad, Math.round(8f * d));

        TextView subtitle = new TextView(context);
        subtitle.setText("Görseli parmağınızla sürükleyerek konumlandırın ve yakınlaştırma çubuğuyla boyutlandırın:");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        subtitle.setTextColor(0xCCFFFFFF);
        root.addView(subtitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final HeroCropPreviewView cropView = new HeroCropPreviewView(context, sourceBitmap);
        LinearLayout.LayoutParams cropLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(164f * d));
        cropLp.topMargin = Math.round(12f * d);
        cropLp.bottomMargin = Math.round(12f * d);
        root.addView(cropView, cropLp);

        LinearLayout zoomRow = new LinearLayout(context);
        zoomRow.setOrientation(LinearLayout.HORIZONTAL);
        zoomRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView zoomLabel = new TextView(context);
        zoomLabel.setText("Boyut / Yakınlaştır:");
        zoomLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        zoomLabel.setTextColor(0xEEFFFFFF);
        zoomRow.addView(zoomLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final SeekBar zoomSeek = new SeekBar(context);
        zoomSeek.setMax(250); // 1.00x -> 3.50x
        zoomSeek.setProgress(0);
        LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        seekLp.leftMargin = Math.round(8f * d);
        zoomRow.addView(zoomSeek, seekLp);

        root.addView(zoomRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        zoomSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    cropView.setZoomScale(1.0f + (progress / 100f));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        cropView.setOnScaleSyncListener(scale -> {
            int p = Math.max(0, Math.min(250, Math.round((scale - 1.0f) * 100f)));
            zoomSeek.setProgress(p);
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Kart Görselini Kırp ve Boyutlandır")
                .setView(root)
                .setNeutralButton("Sıfırla", null)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Kırp ve Uygula", (dInterface, which) -> {
                    Bitmap cropped = cropView.renderCroppedBitmap(1080, 460);
                    if (cropped != null && saveCroppedHeroBitmap(context, cropped)) {
                        setHeroBgMode(context, HERO_BG_CUSTOM);
                        if (onSaved != null) {
                            onSaved.run();
                        }
                        Toast.makeText(context,
                                "Kırpılan kart arka planı başarıyla uygulandı!",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .create();

        dialog.setOnShowListener(dInterface -> {
            android.widget.Button resetBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (resetBtn != null) {
                resetBtn.setOnClickListener(v -> {
                    cropView.resetTransform();
                    zoomSeek.setProgress(0);
                });
            }
        });
        dialog.show();
    }

    private static Bitmap decodeSampledBitmapFromUri(Context context, Uri uri, int maxDimTarget) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, bounds);
            }
            int sampleSize = 1;
            int maxDim = Math.max(bounds.outWidth, bounds.outHeight);
            while (maxDim / sampleSize > maxDimTarget) {
                sampleSize *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is, null, opts);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decoding bitmap for hero crop", e);
            return null;
        }
    }

    private static boolean saveCroppedHeroBitmap(Context context, Bitmap bmp) {
        try {
            File outFile = new File(context.getFilesDir(), CUSTOM_HERO_BG_FILENAME);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save cropped hero bitmap", e);
            return false;
        }
    }

    private interface OnScaleSyncListener {
        void onScaleChanged(float userScale);
    }

    /**
     * Interactive touch pan + pinch-to-zoom preview View locked to the Hero Card's geometry.
     */
    private static final class HeroCropPreviewView extends View {
        private final Bitmap mBitmap;
        private final Paint mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint mScrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mGuideTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mPillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mClipPath = new Path();
        private final RectF mViewRect = new RectF();
        private final RectF mPillRect = new RectF();
        private final Matrix mDrawMatrix = new Matrix();
        private final ScaleGestureDetector mScaleDetector;

        private float mBaseScale = 1f;
        private float mUserScale = 1f;
        private float mOffsetX = 0f;
        private float mOffsetY = 0f;
        private float mLastTouchX = 0f;
        private float mLastTouchY = 0f;
        private boolean mDragging = false;
        private OnScaleSyncListener mScaleListener;

        HeroCropPreviewView(Context context, Bitmap bitmap) {
            super(context);
            mBitmap = bitmap;
            float d = context.getResources().getDisplayMetrics().density;
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mBorderPaint.setStrokeWidth(1.5f * d);
            mBorderPaint.setColor(0x90FFFFFF);

            mGuideTextPaint.setColor(Color.WHITE);
            mGuideTextPaint.setTextSize(16f * d);
            mGuideTextPaint.setFakeBoldText(true);

            mPillPaint.setColor(0xCC181B28);

            mScaleDetector = new ScaleGestureDetector(context,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override
                        public boolean onScale(ScaleGestureDetector detector) {
                            setZoomScale(mUserScale * detector.getScaleFactor());
                            if (mScaleListener != null) {
                                mScaleListener.onScaleChanged(mUserScale);
                            }
                            return true;
                        }
                    });
        }

        void setOnScaleSyncListener(OnScaleSyncListener listener) {
            mScaleListener = listener;
        }

        void setZoomScale(float userScale) {
            mUserScale = Math.max(1.0f, Math.min(3.5f, userScale));
            clampOffsets();
            invalidate();
        }

        void resetTransform() {
            mUserScale = 1.0f;
            mOffsetX = 0f;
            mOffsetY = 0f;
            clampOffsets();
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w <= 0 || h <= 0 || mBitmap == null) return;
            float scaleX = (float) w / mBitmap.getWidth();
            float scaleY = (float) h / mBitmap.getHeight();
            mBaseScale = Math.max(scaleX, scaleY);
            mScrimPaint.setShader(new LinearGradient(
                    0f, 0f, w, 0f,
                    new int[] {0x88080B18, 0x38080B18, 0x18080B18},
                    new float[] {0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP));
            clampOffsets();
        }

        private void clampOffsets() {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0 || mBitmap == null) return;
            float totalScale = mBaseScale * mUserScale;
            float scaledW = mBitmap.getWidth() * totalScale;
            float scaledH = mBitmap.getHeight() * totalScale;
            float maxDx = Math.max(0f, (scaledW - w) * 0.5f);
            float maxDy = Math.max(0f, (scaledH - h) * 0.5f);
            mOffsetX = Math.max(-maxDx, Math.min(maxDx, mOffsetX));
            mOffsetY = Math.max(-maxDy, Math.min(maxDy, mOffsetY));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0 || mBitmap == null) return;
            float d = getResources().getDisplayMetrics().density;
            float r = 24f * d;

            mViewRect.set(0f, 0f, w, h);
            mClipPath.reset();
            mClipPath.addRoundRect(mViewRect, r, r, Path.Direction.CW);

            int save = canvas.save();
            canvas.clipPath(mClipPath);

            float totalScale = mBaseScale * mUserScale;
            float dx = (w - mBitmap.getWidth() * totalScale) * 0.5f + mOffsetX;
            float dy = (h - mBitmap.getHeight() * totalScale) * 0.5f + mOffsetY;
            mDrawMatrix.reset();
            mDrawMatrix.postScale(totalScale, totalScale);
            mDrawMatrix.postTranslate(dx, dy);
            canvas.drawBitmap(mBitmap, mDrawMatrix, mBitmapPaint);

            // Subtle card legibility overlay & preview guides
            canvas.drawRect(mViewRect, mScrimPaint);
            canvas.drawText(Build.MODEL, 18f * d, h * 0.42f, mGuideTextPaint);
            mPillRect.set(18f * d, h * 0.52f, 132f * d, h * 0.52f + 36f * d);
            canvas.drawRoundRect(mPillRect, 18f * d, 18f * d, mPillPaint);
            float textY = mPillRect.centerY() + 5f * d;
            mGuideTextPaint.setTextSize(13f * d);
            canvas.drawText("Updates", 44f * d, textY, mGuideTextPaint);
            mGuideTextPaint.setTextSize(16f * d);

            canvas.restoreToCount(save);

            mViewRect.inset(0.8f * d, 0.8f * d);
            canvas.drawRoundRect(mViewRect, r, r, mBorderPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            mScaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mLastTouchX = event.getX();
                    mLastTouchY = event.getY();
                    mDragging = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mDragging && !mScaleDetector.isInProgress()) {
                        float dx = event.getX() - mLastTouchX;
                        float dy = event.getY() - mLastTouchY;
                        mOffsetX += dx;
                        mOffsetY += dy;
                        mLastTouchX = event.getX();
                        mLastTouchY = event.getY();
                        clampOffsets();
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mDragging = false;
                    break;
            }
            return true;
        }

        Bitmap renderCroppedBitmap(int targetW, int targetH) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0 || mBitmap == null) return null;
            Bitmap out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(out);
            float ratioX = (float) targetW / w;
            float ratioY = (float) targetH / h;
            float ratio = Math.max(ratioX, ratioY);
            float totalScale = mBaseScale * mUserScale * ratio;
            float dx = (targetW - mBitmap.getWidth() * totalScale) * 0.5f + mOffsetX * ratioX;
            float dy = (targetH - mBitmap.getHeight() * totalScale) * 0.5f + mOffsetY * ratioY;
            Matrix m = new Matrix();
            m.postScale(totalScale, totalScale);
            m.postTranslate(dx, dy);
            c.drawBitmap(mBitmap, m, mBitmapPaint);
            return out;
        }
    }
}
