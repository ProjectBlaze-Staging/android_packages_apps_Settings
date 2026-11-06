/*
 * Copyright (C) 2023-2024 the risingOS Android Project
 * Copyright (C) 2026 The BlazeAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evolution.settings.fragments.themes;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SearchIndexable
public class Wallpaper extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    public static final String TAG = "Wallpaper";
    private static final String CLEAN_WALLPAPER_BACKUP = "blaze_clean_wallpaper_backup.png";

    private Preference mBlurWpPref;
    private Preference mBlurWpStylePref;
    private Preference mEffectTypePref;
    private Preference mEffectTargetPref;
    private Preference mSaturationPref;
    private Preference mPixelationPref;
    private Preference mVignettePref;
    private Preference mPosterizePref;
    private Preference mDimPref;
    private Preference mDimLvlPref;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wallpaper_settings);

        mBlurWpPref = findPreference("persist.sys.wallpaper.blur_enabled");
        if (mBlurWpPref != null) mBlurWpPref.setOnPreferenceChangeListener(this);

        mBlurWpStylePref = findPreference("persist.sys.wallpaper.blur_type");
        if (mBlurWpStylePref != null) mBlurWpStylePref.setOnPreferenceChangeListener(this);

        mEffectTypePref = findPreference("persist.sys.wallpaper.effect_type");
        if (mEffectTypePref != null) mEffectTypePref.setOnPreferenceChangeListener(this);

        mEffectTargetPref = findPreference("persist.sys.wallpaper.effect_target");
        if (mEffectTargetPref != null) mEffectTargetPref.setOnPreferenceChangeListener(this);

        mSaturationPref = findPreference("persist.sys.wallpaper.saturation_level");
        if (mSaturationPref != null) mSaturationPref.setOnPreferenceChangeListener(this);

        mPixelationPref = findPreference("persist.sys.wallpaper.pixelation_size");
        if (mPixelationPref != null) mPixelationPref.setOnPreferenceChangeListener(this);

        mVignettePref = findPreference("persist.sys.wallpaper.vignette_intensity");
        if (mVignettePref != null) mVignettePref.setOnPreferenceChangeListener(this);

        mPosterizePref = findPreference("persist.sys.wallpaper.posterize_levels");
        if (mPosterizePref != null) mPosterizePref.setOnPreferenceChangeListener(this);

        mDimPref = findPreference("persist.sys.wallpaper.dim_enabled");
        if (mDimPref != null) mDimPref.setOnPreferenceChangeListener(this);

        mDimLvlPref = findPreference("persist.sys.wallpaper.dim_level");
        if (mDimLvlPref != null) mDimLvlPref.setOnPreferenceChangeListener(this);

        updateEffectDependencies();
        updateBlurDependencies();
        updateDimDependencies();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == null || newValue == null) return false;
        String key = preference.getKey();
        if (key != null && key.startsWith("persist.sys.wallpaper.")) {
            SystemProperties.set(key, newValue.toString());

            updateEffectDependencies();
            updateBlurDependencies();
            updateDimDependencies();

            Context ctx = getContext();
            if (ctx != null) {
                int ver = Settings.System.getInt(ctx.getContentResolver(),
                        "blaze_wallpaper_effects_version", 0) + 1;
                Settings.System.putInt(ctx.getContentResolver(),
                        "blaze_wallpaper_effects_version", ver);
                applyLiveWallpaperEffectAsync(ctx.getApplicationContext());
                Toast.makeText(ctx,
                        "Görsel efekt canlı olarak uygulandı",
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    private void applyLiveWallpaperEffectAsync(final Context appContext) {
        mExecutor.execute(() -> {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(appContext);
                if (wm == null) return;
                File backupFile = new File(appContext.getFilesDir(), CLEAN_WALLPAPER_BACKUP);

                int blurTarget = SystemProperties.getInt("persist.sys.wallpaper.blur_enabled", 0);
                int blurType = SystemProperties.getInt("persist.sys.wallpaper.blur_type", 0);
                int effectType = SystemProperties.getInt("persist.sys.wallpaper.effect_type", 0);
                int effectTarget = SystemProperties.getInt("persist.sys.wallpaper.effect_target", 1);
                int satLevel = SystemProperties.getInt("persist.sys.wallpaper.saturation_level", 100);
                int pixelSize = SystemProperties.getInt("persist.sys.wallpaper.pixelation_size", 8);
                int vignetteLevel = SystemProperties.getInt("persist.sys.wallpaper.vignette_intensity", 50);
                int posterizeLevels = SystemProperties.getInt("persist.sys.wallpaper.posterize_levels", 8);
                int dimTarget = SystemProperties.getInt("persist.sys.wallpaper.dim_enabled", 0);
                int dimLevel = SystemProperties.getInt("persist.sys.wallpaper.dim_level", 10);

                boolean anyActive = (blurTarget > 0) || (effectType > 0) || (dimTarget > 0);
                if (!anyActive) {
                    if (backupFile.exists()) {
                        Bitmap orig = BitmapFactory.decodeFile(backupFile.getAbsolutePath());
                        if (orig != null) {
                            wm.setBitmap(orig, null, true,
                                    WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
                        }
                    }
                    return;
                }

                Bitmap base = null;
                if (backupFile.exists()) {
                    base = BitmapFactory.decodeFile(backupFile.getAbsolutePath());
                }
                if (base == null) {
                    Drawable d = wm.getDrawable();
                    if (d instanceof BitmapDrawable) {
                        base = ((BitmapDrawable) d).getBitmap();
                    }
                    if (base != null) {
                        try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                            base.compress(Bitmap.CompressFormat.PNG, 95, fos);
                        } catch (Throwable ignored) {}
                    }
                }
                if (base == null) return;

                Bitmap working = base.copy(Bitmap.Config.ARGB_8888, true);
                if ((effectType == 4 && pixelSize >= 2) || blurTarget > 0) {
                    int downscale = (effectType == 4)
                            ? Math.max(2, pixelSize)
                            : (blurType == 3 ? 12 : (blurType == 2 ? 9 : 6));
                    int sw = Math.max(16, working.getWidth() / downscale);
                    int sh = Math.max(16, working.getHeight() / downscale);
                    Bitmap small = Bitmap.createScaledBitmap(working, sw, sh, blurTarget > 0);
                    working = Bitmap.createScaledBitmap(
                            small, working.getWidth(), working.getHeight(), effectType != 4);
                }

                Bitmap out = Bitmap.createBitmap(
                        working.getWidth(), working.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(out);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

                if (effectType > 0) {
                    ColorMatrix cm = new ColorMatrix();
                    switch (effectType) {
                        case 1: cm.setSaturation(0f); break;
                        case 2:
                            cm.setSaturation(0f);
                            ColorMatrix sepia = new ColorMatrix();
                            sepia.setScale(1f, 0.92f, 0.76f, 1f);
                            cm.postConcat(sepia);
                            break;
                        case 5: cm.setSaturation(Math.max(0f, satLevel / 100f)); break;
                        case 6:
                            cm.set(new float[]{
                                    -1f, 0f, 0f, 0f, 255f,
                                    0f, -1f, 0f, 0f, 255f,
                                    0f, 0f, -1f, 0f, 255f,
                                    0f, 0f, 0f, 1f, 0f
                            });
                            break;
                        case 7: cm.setScale(1.12f, 1.02f, 0.88f, 1f); break;
                        case 8: cm.setScale(0.88f, 1.0f, 1.15f, 1f); break;
                        case 9:
                            cm.setSaturation(1.45f);
                            ColorMatrix cyber = new ColorMatrix(new float[]{
                                    1.15f, 0f, 0.15f, 0f, 10f,
                                    0f, 0.90f, 0.10f, 0f, 0f,
                                    0.15f, 0f, 1.25f, 0f, 20f,
                                    0f, 0f, 0f, 1f, 0f
                            });
                            cm.postConcat(cyber);
                            break;
                        case 10:
                            cm.setSaturation(0.65f);
                            ColorMatrix vintage = new ColorMatrix(new float[]{
                                    1.08f, 0f, 0f, 0f, 12f,
                                    0f, 1.02f, 0f, 0f, 6f,
                                    0f, 0f, 0.85f, 0f, -8f,
                                    0f, 0f, 0f, 1f, 0f
                            });
                            cm.postConcat(vintage);
                            break;
                        case 11:
                            float contrast = 1.2f + ((16 - Math.min(16, posterizeLevels)) / 14f) * 0.65f;
                            float translate = (-0.5f * contrast + 0.5f) * 255f;
                            cm.set(new float[]{
                                    contrast, 0, 0, 0, translate,
                                    0, contrast, 0, 0, translate,
                                    0, 0, contrast, 0, translate,
                                    0, 0, 0, 1, 0
                            });
                            break;
                    }
                    paint.setColorFilter(new ColorMatrixColorFilter(cm));
                }
                canvas.drawBitmap(working, 0f, 0f, paint);

                if (blurTarget > 0 && blurType == 1) {
                    Paint frost = new Paint();
                    frost.setColor(Color.argb(46, 255, 255, 255));
                    canvas.drawRect(0, 0, out.getWidth(), out.getHeight(), frost);
                }

                if (effectType == 3 && vignetteLevel > 0) {
                    Paint vp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    float radius = Math.max(out.getWidth(), out.getHeight()) * 0.65f;
                    int edgeAlpha = Math.min(235, (int) (vignetteLevel * 2.35f));
                    vp.setShader(new RadialGradient(
                            out.getWidth() * 0.5f, out.getHeight() * 0.5f, radius,
                            new int[]{Color.TRANSPARENT, Color.argb(edgeAlpha, 0, 0, 0)},
                            new float[]{0.32f, 1.0f}, Shader.TileMode.CLAMP));
                    canvas.drawRect(0, 0, out.getWidth(), out.getHeight(), vp);
                }

                if (dimTarget > 0 && dimLevel > 0) {
                    Paint dp = new Paint();
                    dp.setColor(Color.argb(Math.min(230, Math.round(dimLevel * 2.3f)), 0, 0, 0));
                    canvas.drawRect(0, 0, out.getWidth(), out.getHeight(), dp);
                }

                int targetFlags = 0;
                int primaryTarget = effectType > 0 ? effectTarget : (blurTarget > 0 ? blurTarget : dimTarget);
                if (primaryTarget == 1) {
                    targetFlags = WallpaperManager.FLAG_LOCK;
                } else if (primaryTarget == 2) {
                    targetFlags = WallpaperManager.FLAG_SYSTEM;
                } else {
                    targetFlags = WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;
                }
                wm.setBitmap(out, null, true, targetFlags);
            } catch (Throwable ignored) {}
        });
    }

    private void updateEffectDependencies() {
        int effectType = SystemProperties.getInt("persist.sys.wallpaper.effect_type", 0);
        if (mEffectTargetPref != null) mEffectTargetPref.setVisible(effectType > 0);
        if (mVignettePref != null) mVignettePref.setVisible(effectType == 3);
        if (mPixelationPref != null) mPixelationPref.setVisible(effectType == 4);
        if (mSaturationPref != null) mSaturationPref.setVisible(effectType == 5);
        if (mPosterizePref != null) mPosterizePref.setVisible(effectType == 11);
    }

    private void updateBlurDependencies() {
        int blurEnabled = SystemProperties.getInt("persist.sys.wallpaper.blur_enabled", 0);
        if (mBlurWpStylePref != null) mBlurWpStylePref.setVisible(blurEnabled > 0);
    }

    private void updateDimDependencies() {
        int dimEnabled = SystemProperties.getInt("persist.sys.wallpaper.dim_enabled", 0);
        if (mDimLvlPref != null) mDimLvlPref.setVisible(dimEnabled > 0);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.wallpaper_settings);
}
