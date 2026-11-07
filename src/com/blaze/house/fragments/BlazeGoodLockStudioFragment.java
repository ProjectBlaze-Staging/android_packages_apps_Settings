/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Home Up & Modüler Sistem Araçları Stüdyosu.
 * Every control in this fragment is wired directly to Launcher3 (BlazeHomeUpController,
 * RectFSpringAnim, QuickstepTransitionManager, ScalingWorkspaceRevealAnim, Folder,
 * InvariantDeviceProfile, RecentsView) or Android 17 framework services.
 */
public class BlazeGoodLockStudioFragment extends SettingsPreferenceFragment {

    private static final int TAB_HOMEUP_GESTURE = 0;
    private static final int TAB_HOMEUP_LAYOUT = 1;
    private static final int TAB_SYSTEM = 2;

    // Shared Settings.Secure keys with Launcher3 BlazeHomeUpController
    private static final String KEY_GESTURE_MODE = "blaze_homeup_gesture_mode";
    private static final String KEY_SIMPLE_PROGRESS = "blaze_homeup_simple_progress";
    private static final String KEY_DAMPING_X = "blaze_homeup_damping_x";
    private static final String KEY_DAMPING_Y = "blaze_homeup_damping_y";
    private static final String KEY_STIFFNESS_X = "blaze_homeup_stiffness_x";
    private static final String KEY_STIFFNESS_Y = "blaze_homeup_stiffness_y";
    private static final String KEY_SCALE_DAMPING = "blaze_homeup_scale_damping";
    private static final String KEY_SCALE_STIFFNESS = "blaze_homeup_scale_stiffness";
    private static final String KEY_TRACKING_MODE = "blaze_homeup_tracking_mode";
    private static final String KEY_APP_OPEN_DURATION = "blaze_homeup_app_open_duration";
    private static final String KEY_HOME_DURATION = "blaze_homeup_home_duration";
    private static final String KEY_HOME_SCALE = "blaze_homeup_home_scale";
    private static final String KEY_WALLPAPER_DURATION = "blaze_homeup_wallpaper_duration";
    private static final String KEY_WALLPAPER_BLUR = "blaze_homeup_wallpaper_blur";

    private static final String KEY_POPUP_FOLDER = "blaze_homeup_popup_folder";
    private static final String KEY_FOLDER_GRID = "blaze_homeup_folder_grid";
    private static final String KEY_ICON_SCALE_PCT = "blaze_homeup_icon_scale_pct";
    private static final String KEY_GRID_NAME = "blaze_homeup_grid_name";
    private static final String KEY_HIDE_LABELS = "blaze_homeup_hide_labels";
    private static final String KEY_TASK_CHANGER_STYLE = "blaze_homeup_task_changer_style";
    private static final String KEY_RECENTS_FOCUS_PREV = "blaze_homeup_recents_focus_prev";

    private Context mContext;
    private ContentResolver mResolver;
    private int mCurrentTab = TAB_HOMEUP_GESTURE;

    private boolean mIsNight;
    private int mColorBg;
    private int mColorSurface;
    private int mColorSurfaceVariant;
    private int mColorBorder;
    private int mColorAccent;
    private int mColorTextPrimary;
    private int mColorTextSecondary;

    private LinearLayout mGestureContainer;
    private LinearLayout mLayoutContainer;
    private LinearLayout mSystemContainer;
    private TextView mTabGestureBtn;
    private TextView mTabLayoutBtn;
    private TextView mTabSystemBtn;
    private TextView mActiveModulesBadge;

    private HomeUpSpringPreviewView mSpringPreviewView;
    private TextView mPhysicsTelemetryText;

    private volatile boolean mAmbientPlaying = false;
    private Thread mAmbientThread;
    private TextView mAmbientStatusText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = requireContext();
        mResolver = mContext.getContentResolver();
        if (savedInstanceState != null) {
            mCurrentTab = savedInstanceState.getInt("modular_tab", TAB_HOMEUP_GESTURE);
        }
        if (getActivity() != null) {
            getActivity().setTitle("Home Up ve Sistem Araçları");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("modular_tab", mCurrentTab);
    }

    private void resolveThemePalette() {
        int uiMode = mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        mIsNight = (uiMode == Configuration.UI_MODE_NIGHT_YES);
        mColorBg = resolveAttrColor(android.R.attr.colorBackground, mIsNight ? 0xFF111318 : 0xFFF8F9FC);
        mColorAccent = resolveAttrColor(android.R.attr.colorAccent, mIsNight ? 0xFFA8C7FA : 0xFF1B6EF3);
        mColorTextPrimary = resolveAttrColor(android.R.attr.textColorPrimary, mIsNight ? 0xFFE2E2E9 : 0xFF1A1C20);
        mColorTextSecondary = resolveAttrColor(android.R.attr.textColorSecondary, mIsNight ? 0xFFC4C6D0 : 0xFF44474F);
        mColorSurface = mIsNight ? 0xFF1C1F26 : 0xFFFFFFFF;
        mColorSurfaceVariant = mIsNight ? 0xFF252932 : 0xFFEEF1F7;
        mColorBorder = mIsNight ? 0x22FFFFFF : 0x16000000;
    }

    private int resolveAttrColor(int attr, int fallback) {
        TypedValue tv = new TypedValue();
        if (mContext.getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) {
                try {
                    return mContext.getColor(tv.resourceId);
                } catch (Exception ignored) {}
            }
            return tv.data;
        }
        return fallback;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        resolveThemePalette();

        ScrollView scrollView = new ScrollView(mContext);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(mColorBg);

        LinearLayout root = new LinearLayout(mContext);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(16);
        root.setPadding(padH, dp(12), padH, dp(40));
        scrollView.addView(root);

        root.addView(buildHeroBanner());
        root.addView(buildSuiteTabSwitcher());

        mGestureContainer = new LinearLayout(mContext);
        mGestureContainer.setOrientation(LinearLayout.VERTICAL);
        populateHomeUpGestureTab(mGestureContainer);
        root.addView(mGestureContainer);

        mLayoutContainer = new LinearLayout(mContext);
        mLayoutContainer.setOrientation(LinearLayout.VERTICAL);
        populateHomeUpLayoutTab(mLayoutContainer);
        root.addView(mLayoutContainer);

        mSystemContainer = new LinearLayout(mContext);
        mSystemContainer.setOrientation(LinearLayout.VERTICAL);
        populateSystemModules(mSystemContainer);
        root.addView(mSystemContainer);

        switchSuiteTab(mCurrentTab);
        refreshActiveModulesCount();
        return scrollView;
    }

    @Override
    public void onDestroyView() {
        stopAmbientSoundscape();
        if (mSpringPreviewView != null) {
            mSpringPreviewView.stopAnimation();
        }
        super.onDestroyView();
    }

    private View buildHeroBanner() {
        LinearLayout card = new LinearLayout(mContext);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(mColorSurface);
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), mColorBorder);
        card.setBackground(bg);
        int pad = dp(20);
        card.setPadding(pad, dp(18), pad, dp(18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        card.setLayoutParams(lp);

        LinearLayout topRow = new LinearLayout(mContext);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout iconWrap = createIconBadge(R.drawable.ic_settings_system_dashboard_filled);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        iconLp.rightMargin = dp(12);
        topRow.addView(iconWrap, iconLp);

        LinearLayout titleCol = new LinearLayout(mContext);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleColLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleCol.setLayoutParams(titleColLp);

        TextView pill = new TextView(mContext);
        pill.setText("HOME UP & GOOD LOCK MİMARİSİ");
        pill.setTextColor(mColorAccent);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        pill.setLetterSpacing(0.05f);
        titleCol.addView(pill);

        TextView title = new TextView(mContext);
        title.setText("Home Up ve Sistem Stüdyosu");
        title.setTextColor(mColorTextPrimary);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.5f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleCol.addView(title);

        topRow.addView(titleCol);

        mActiveModulesBadge = new TextView(mContext);
        mActiveModulesBadge.setTextColor(mColorAccent);
        mActiveModulesBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        mActiveModulesBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(withAlpha(mColorAccent, 28));
        badgeBg.setCornerRadius(dp(99));
        mActiveModulesBadge.setBackground(badgeBg);
        mActiveModulesBadge.setPadding(dp(10), dp(5), dp(10), dp(5));
        topRow.addView(mActiveModulesBadge);

        card.addView(topRow);

        TextView subtitle = new TextView(mContext);
        subtitle.setText("Launcher3 yaylanma fiziği (Stiffness/Damping), uygulama açılış eğrileri, Pop-up klasör düzeni, ızgara/ikon ölçeği, Task Changer kart görünümü ve sistem araçlarını gerçek zamanlı yönetin.");
        subtitle.setTextColor(mColorTextSecondary);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        subtitle.setLineSpacing(dp(2), 1.0f);
        subtitle.setPadding(0, dp(10), 0, 0);
        card.addView(subtitle);

        return card;
    }

    private FrameLayout createIconBadge(int drawableResId) {
        FrameLayout frame = new FrameLayout(mContext);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(withAlpha(mColorAccent, 30));
        bg.setCornerRadius(dp(12));
        frame.setBackground(bg);

        ImageView iv = new ImageView(mContext);
        iv.setImageResource(drawableResId);
        iv.setColorFilter(mColorAccent);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER);
        frame.addView(iv, ivLp);
        return frame;
    }

    private View buildSuiteTabSwitcher() {
        LinearLayout bar = new LinearLayout(mContext);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(mColorSurfaceVariant);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), mColorBorder);
        bar.setBackground(bg);
        bar.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(16);
        bar.setLayoutParams(lp);

        mTabGestureBtn = createSuiteTabButton("Home Up • Fizik", TAB_HOMEUP_GESTURE);
        mTabLayoutBtn = createSuiteTabButton("Home Up • Düzen", TAB_HOMEUP_LAYOUT);
        mTabSystemBtn = createSuiteTabButton("Sistem Araçları", TAB_SYSTEM);

        bar.addView(mTabGestureBtn, new LinearLayout.LayoutParams(0, dp(42), 1f));
        bar.addView(mTabLayoutBtn, new LinearLayout.LayoutParams(0, dp(42), 1f));
        bar.addView(mTabSystemBtn, new LinearLayout.LayoutParams(0, dp(42), 1f));
        return bar;
    }

    private TextView createSuiteTabButton(String label, final int tabIndex) {
        TextView tv = new TextView(mContext);
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setOnClickListener(v -> {
            vibrateTick();
            switchSuiteTab(tabIndex);
        });
        return tv;
    }

    private void switchSuiteTab(int tabIndex) {
        mCurrentTab = tabIndex;
        if (mGestureContainer != null) {
            mGestureContainer.setVisibility(tabIndex == TAB_HOMEUP_GESTURE ? View.VISIBLE : View.GONE);
        }
        if (mLayoutContainer != null) {
            mLayoutContainer.setVisibility(tabIndex == TAB_HOMEUP_LAYOUT ? View.VISIBLE : View.GONE);
        }
        if (mSystemContainer != null) {
            mSystemContainer.setVisibility(tabIndex == TAB_SYSTEM ? View.VISIBLE : View.GONE);
        }
        styleSuiteTab(mTabGestureBtn, tabIndex == TAB_HOMEUP_GESTURE);
        styleSuiteTab(mTabLayoutBtn, tabIndex == TAB_HOMEUP_LAYOUT);
        styleSuiteTab(mTabSystemBtn, tabIndex == TAB_SYSTEM);
        if (tabIndex == TAB_HOMEUP_GESTURE && mSpringPreviewView != null) {
            mSpringPreviewView.triggerPreviewAnimation();
        }
    }

    private void styleSuiteTab(TextView tv, boolean active) {
        if (tv == null) return;
        if (active) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(mColorSurface);
            bg.setCornerRadius(dp(14));
            bg.setStroke(dp(1), withAlpha(mColorAccent, 90));
            tv.setBackground(bg);
            tv.setTextColor(mColorAccent);
        } else {
            tv.setBackground(null);
            tv.setTextColor(mColorTextSecondary);
        }
    }

    private void refreshActiveModulesCount() {
        if (mActiveModulesBadge == null) return;
        int mode = Settings.Secure.getInt(mResolver, KEY_GESTURE_MODE, 0);
        String[] modeNames = {"Dinamik", "Klasik", "Canlı", "Yumuşak", "Basit", "Gelişmiş"};
        String modeLabel = (mode >= 0 && mode < modeNames.length) ? modeNames[mode] : "Dinamik";
        mActiveModulesBadge.setText("Profil: " + modeLabel);
    }

    // =========================================================================
    // TAB 1: HOME UP • HAREKET VE YAYLANMA FİZİĞİ (GESTURE TUNING)
    // =========================================================================

    private void populateHomeUpGestureTab(LinearLayout parent) {
        parent.removeAllViews();

        // CARD 1: Canlı Yaylanma ve Yörünge Simülatörü (IconMovePreview + IconScalePreview)
        LinearLayout previewCard = createModuleCard(
                R.drawable.ic_settings_gestures,
                "Canlı Yaylanma ve Yörünge Önizlemesi",
                "GERÇEK ZAMANLI SPRINGANIMATION SİMÜLATÖRÜ");

        mPhysicsTelemetryText = new TextView(mContext);
        mPhysicsTelemetryText.setTextColor(mColorTextSecondary);
        mPhysicsTelemetryText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        mPhysicsTelemetryText.setPadding(0, 0, 0, dp(8));
        previewCard.addView(mPhysicsTelemetryText);

        mSpringPreviewView = new HomeUpSpringPreviewView(mContext);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(164));
        prevLp.bottomMargin = dp(8);
        previewCard.addView(mSpringPreviewView, prevLp);

        TextView tapHint = new TextView(mContext);
        tapHint.setText("Simülasyonu tekrar oynatmak için önizleme alanına dokunun veya aşağıdaki fizik parametrelerini değiştirin.");
        tapHint.setTextColor(mColorTextSecondary);
        tapHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        previewCard.addView(tapHint);

        parent.addView(previewCard);

        // CARD 2: Home Up Hareket Modu ve Hazır Profiller
        LinearLayout modeCard = createModuleCard(
                R.drawable.ic_settings_display_filled,
                "Hareket Fiziği ve Yaylanma Profili",
                "HOME UP 17 ORİJİNAL PRESET VE ENTERPOLASYON MOTORU");

        int currentMode = Settings.Secure.getInt(mResolver, KEY_GESTURE_MODE, 0);
        final String[] modeTitles = {
                "Dinamik (Hızlı ve Dengeli — Varsayılan)",
                "Klasik (Kritik Sönümlü • Sıfır Salınım)",
                "Canlı (Esnek • Yüksek Yaylanma)",
                "Yumuşak (Geniş ve Sakin Geçiş)",
                "Basit Kaydırıcı (0–100 Tek Çubuk Senkronizasyonu)",
                "Gelişmiş Fizik Ayarları (Tam Manuel Kontrol)"
        };

        HorizontalScrollView chipScroll = new HorizontalScrollView(mContext);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(mContext);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(0, dp(4), 0, dp(10));

        final String[] shortLabels = {
                "Dinamik", "Klasik", "Canlı", "Yumuşak", "Basit Çubuk", "Gelişmiş"
        };
        for (int i = 0; i < shortLabels.length; i++) {
            final int modeIdx = i;
            TextView chip = new TextView(mContext);
            chip.setText(shortLabels[i]);
            boolean selected = (currentMode == modeIdx);
            chip.setTextColor(selected ? mColorAccent : mColorTextSecondary);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(selected ? withAlpha(mColorAccent, 28) : mColorSurfaceVariant);
            chipBg.setCornerRadius(dp(99));
            chipBg.setStroke(dp(1), selected ? mColorAccent : mColorBorder);
            chip.setBackground(chipBg);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = dp(8);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                vibrateTick();
                Settings.Secure.putInt(mResolver, KEY_GESTURE_MODE, modeIdx);
                refreshActiveModulesCount();
                populateHomeUpGestureTab(parent);
                if (mSpringPreviewView != null) {
                    mSpringPreviewView.triggerPreviewAnimation();
                }
            });
            chipRow.addView(chip);
        }
        chipScroll.addView(chipRow);
        modeCard.addView(chipScroll);

        TextView activeProfileDesc = new TextView(mContext);
        int safeMode = Math.max(0, Math.min(modeTitles.length - 1, currentMode));
        activeProfileDesc.setText("Seçili Mod: " + modeTitles[safeMode]);
        activeProfileDesc.setTextColor(mColorAccent);
        activeProfileDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        activeProfileDesc.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        activeProfileDesc.setPadding(0, dp(2), 0, dp(8));
        modeCard.addView(activeProfileDesc);

        // If Mode == 4 (Simple Tuning Slider 0..100), show the unified slider
        if (currentMode == 4) {
            int curProgress = Settings.Secure.getInt(mResolver, KEY_SIMPLE_PROGRESS, 50);
            addSliderControl(modeCard,
                    "Hız ve Yaylanma Dengesi (SimpleTuningLogic)",
                    0, 100, curProgress,
                    val -> "%" + val + (val < 35 ? " (Yumuşak)" : (val > 70 ? " (Çok Hızlı & Esnek)" : " (Dengeli)")),
                    val -> {
                        Settings.Secure.putInt(mResolver, KEY_SIMPLE_PROGRESS, val);
                        updateTelemetryAndPreview();
                    });
        }

        boolean blurEnabled = Settings.Secure.getInt(mResolver, KEY_WALLPAPER_BLUR, 1) == 1;
        addSwitchRow(modeCard,
                "Uygulama Geçişlerinde Canlı Arka Plan Bulanıklığı",
                "Uygulama açılış ve ana ekrana dönüşte duvar kağıdı derinlik bulanıklığını çalıştırır",
                blurEnabled,
                checked -> {
                    Settings.Secure.putInt(mResolver, KEY_WALLPAPER_BLUR, checked ? 1 : 0);
                    updateTelemetryAndPreview();
                });

        parent.addView(modeCard);

        // CARD 3: Gelişmiş Fizik Ayarları (Shown expanded when Mode == 5, or quick button to switch to Mode 5)
        LinearLayout advCard = createModuleCard(
                R.drawable.ic_settings_system_dashboard_expressive,
                "Gelişmiş Yay ve Süre Parametreleri",
                "STIFFNESS, DAMPING, ÖLÇEK VE SÜRE KATSAYILARI");

        if (currentMode != 5) {
            TextView note = new TextView(mContext);
            note.setText("Şu anda hazır profil veya basit kaydırıcı aktif. Tüm X/Y yay sertliği, sönümleme oranı, ikon takip noktası ve süreleri tek tek ayarlamak için Gelişmiş Fizik modunu etkinleştirin.");
            note.setTextColor(mColorTextSecondary);
            note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            note.setPadding(0, dp(2), 0, dp(8));
            advCard.addView(note);

            addActionRow(advCard,
                    "Gelişmiş Manuel Fizik Kontrollerini Aç",
                    "Mevcut profilin katsayılarını temel alarak 11 parametreyi bağımsız düzenle",
                    null,
                    v -> {
                        vibrateTick();
                        seedAdvancedFromCurrentPhysics();
                        Settings.Secure.putInt(mResolver, KEY_GESTURE_MODE, 5);
                        refreshActiveModulesCount();
                        populateHomeUpGestureTab(parent);
                    });
        } else {
            int dampX = Math.round(Settings.Secure.getFloat(mResolver, KEY_DAMPING_X, 0.82f) * 100f);
            addSliderControl(advCard,
                    "Yatay Sönümleme Oranı (Damping X)",
                    50, 100, clamp(dampX, 50, 100),
                    val -> String.format(Locale.US, "%.2f", val / 100f),
                    val -> {
                        Settings.Secure.putFloat(mResolver, KEY_DAMPING_X, val / 100f);
                        updateTelemetryAndPreview();
                    });

            int dampY = Math.round(Settings.Secure.getFloat(mResolver, KEY_DAMPING_Y, 0.82f) * 100f);
            addSliderControl(advCard,
                    "Dikey Sönümleme Oranı (Damping Y)",
                    50, 100, clamp(dampY, 50, 100),
                    val -> String.format(Locale.US, "%.2f", val / 100f),
                    val -> {
                        Settings.Secure.putFloat(mResolver, KEY_DAMPING_Y, val / 100f);
                        updateTelemetryAndPreview();
                    });

            int stiffX = Math.round(Settings.Secure.getFloat(mResolver, KEY_STIFFNESS_X, 240f));
            addSliderControl(advCard,
                    "Yatay Yay Sertliği (Stiffness X)",
                    50, 900, clamp(stiffX, 50, 900),
                    val -> val + " N/m",
                    val -> {
                        Settings.Secure.putFloat(mResolver, KEY_STIFFNESS_X, (float) val);
                        updateTelemetryAndPreview();
                    });

            int stiffY = Math.round(Settings.Secure.getFloat(mResolver, KEY_STIFFNESS_Y, 240f));
            addSliderControl(advCard,
                    "Dikey Yay Sertliği (Stiffness Y)",
                    50, 900, clamp(stiffY, 50, 900),
                    val -> val + " N/m",
                    val -> {
                        Settings.Secure.putFloat(mResolver, KEY_STIFFNESS_Y, (float) val);
                        updateTelemetryAndPreview();
                    });

            int scaleDamp = Math.round(Settings.Secure.getFloat(mResolver, KEY_SCALE_DAMPING, 0.86f) * 100f);
            addSliderControl(advCard,
                    "İkon Küçülme Sönümlemesi (Scale Damping)",
                    50, 100, clamp(scaleDamp, 50, 100),
                    val -> String.format(Locale.US, "%.2f", val / 100f),
                    val -> {
                        Settings.Secure.putFloat(mResolver, KEY_SCALE_DAMPING, val / 100f);
                        updateTelemetryAndPreview();
                    });

            int scaleStiff = Math.round(Settings.Secure.getFloat(mResolver, KEY_SCALE_STIFFNESS, 300f));
            addSliderControl(advCard,
                    "İkon Küçülme Yay Sertliği (Scale Stiffness)",
                    50, 900, clamp(scaleStiff, 50, 900),
                    val -> val + " N/m",
                    val -> {
                        Settings.Secure.putFloat(mResolver, KEY_SCALE_STIFFNESS, (float) val);
                        updateTelemetryAndPreview();
                    });

            int tracking = Settings.Secure.getInt(mResolver, KEY_TRACKING_MODE, 1);
            final String[] trackLabels = {"Üst Kenar (0.0)", "Merkez (0.5)", "Alt Kenar (1.0)"};
            final TextView trackSummary = new TextView(mContext);
            addActionRow(advCard,
                    "İkon Yörünge Takip Noktası (Tracking)",
                    "Aktif hizalama: " + trackLabels[clamp(tracking, 0, 2)],
                    trackSummary,
                    v -> {
                        int cur = clamp(Settings.Secure.getInt(mResolver, KEY_TRACKING_MODE, 1), 0, 2);
                        new AlertDialog.Builder(mContext)
                                .setTitle("İkon Yörünge Takip Noktası")
                                .setSingleChoiceItems(trackLabels, cur, (d, which) -> {
                                    Settings.Secure.putInt(mResolver, KEY_TRACKING_MODE, which);
                                    trackSummary.setText("Aktif hizalama: " + trackLabels[which]);
                                    vibrateTick();
                                    updateTelemetryAndPreview();
                                    d.dismiss();
                                })
                                .show();
                    });

            int appOpenMs = Settings.Secure.getInt(mResolver, KEY_APP_OPEN_DURATION, 450);
            addSliderControl(advCard,
                    "Uygulama Açılış Süresi (App Open Duration)",
                    180, 1000, clamp(appOpenMs, 180, 1000),
                    val -> val + " ms",
                    val -> {
                        Settings.Secure.putInt(mResolver, KEY_APP_OPEN_DURATION, val);
                        updateTelemetryAndPreview();
                    });

            int homeMs = Settings.Secure.getInt(mResolver, KEY_HOME_DURATION, 480);
            addSliderControl(advCard,
                    "Ana Ekran Dönüş Süresi (Home Reveal Duration)",
                    180, 1500, clamp(homeMs, 180, 1500),
                    val -> val + " ms",
                    val -> {
                        Settings.Secure.putInt(mResolver, KEY_HOME_DURATION, val);
                        updateTelemetryAndPreview();
                    });

            int homeScalePct = Math.round(Settings.Secure.getFloat(mResolver, KEY_HOME_SCALE, 0.88f) * 100f);
            addSliderControl(advCard,
                    "Ana Ekran Derinlik Ölçeği (Home Scale)",
                    70, 98, clamp(homeScalePct, 70, 98),
                    val -> "%" + val,
                    val -> {
                        Settings.Secure.putFloat(mResolver, KEY_HOME_SCALE, val / 100f);
                        updateTelemetryAndPreview();
                    });

            int wpMs = Settings.Secure.getInt(mResolver, KEY_WALLPAPER_DURATION, 850);
            addSliderControl(advCard,
                    "Duvar Kağıdı Zoom Süresi (Wallpaper Duration)",
                    200, 1800, clamp(wpMs, 200, 1800),
                    val -> val + " ms",
                    val -> {
                        Settings.Secure.putInt(mResolver, KEY_WALLPAPER_DURATION, val);
                        updateTelemetryAndPreview();
                    });
        }

        addActionRow(advCard,
                "Hareket Fiziğini Fabrika Değerlerine Sıfırla",
                "Home Up Dinamik (Preset 0) standart değerlerine geri döner",
                null,
                v -> {
                    vibrateTick();
                    Settings.Secure.putInt(mResolver, KEY_GESTURE_MODE, 0);
                    Settings.Secure.putInt(mResolver, KEY_SIMPLE_PROGRESS, 50);
                    Settings.Secure.putInt(mResolver, KEY_WALLPAPER_BLUR, 1);
                    refreshActiveModulesCount();
                    populateHomeUpGestureTab(parent);
                    Toast.makeText(mContext, "Home Up hareket fiziği sıfırlandı", Toast.LENGTH_SHORT).show();
                });

        parent.addView(advCard);
        updateTelemetryAndPreview();
    }

    private void seedAdvancedFromCurrentPhysics() {
        float[] vals = resolveCurrentPhysicsArray();
        Settings.Secure.putFloat(mResolver, KEY_DAMPING_X, vals[0]);
        Settings.Secure.putFloat(mResolver, KEY_DAMPING_Y, vals[1]);
        Settings.Secure.putFloat(mResolver, KEY_STIFFNESS_X, vals[2]);
        Settings.Secure.putFloat(mResolver, KEY_STIFFNESS_Y, vals[3]);
        Settings.Secure.putFloat(mResolver, KEY_SCALE_DAMPING, vals[4]);
        Settings.Secure.putFloat(mResolver, KEY_SCALE_STIFFNESS, vals[5]);
        Settings.Secure.putInt(mResolver, KEY_APP_OPEN_DURATION, Math.round(vals[6]));
        Settings.Secure.putInt(mResolver, KEY_HOME_DURATION, Math.round(vals[7]));
        Settings.Secure.putFloat(mResolver, KEY_HOME_SCALE, vals[8]);
        Settings.Secure.putInt(mResolver, KEY_WALLPAPER_DURATION, Math.round(vals[9]));
    }

    /**
     * Returns [dampingX, dampingY, stiffnessX, stiffnessY, scaleDamping, scaleStiffness,
     *          appOpenMs, homeMs, homeScale, wallpaperMs]
     */
    private float[] resolveCurrentPhysicsArray() {
        int mode = Settings.Secure.getInt(mResolver, KEY_GESTURE_MODE, 0);
        switch (mode) {
            case 1: // Classic
                return new float[]{1.00f, 1.00f, 150f, 150f, 1.00f, 150f, 500f, 700f, 0.82f, 1100f};
            case 2: // Playful
                return new float[]{0.66f, 0.66f, 330f, 330f, 0.68f, 650f, 420f, 320f, 0.76f, 620f};
            case 3: // Sweet
                return new float[]{0.96f, 0.96f, 65f, 65f, 0.98f, 110f, 680f, 1050f, 0.85f, 1500f};
            case 4: { // Simple Slider
                int p = clamp(Settings.Secure.getInt(mResolver, KEY_SIMPLE_PROGRESS, 50), 0, 100);
                int inv = 100 - p;
                return new float[]{
                        (inv * 0.0021f) + 0.78f,
                        (inv * 0.0021f) + 0.78f,
                        (p * 5.5f) + 50f,
                        (p * 5.5f) + 50f,
                        (inv * 0.0019f) + 0.80f,
                        (p * 6.0f) + 100f,
                        650f - (p * 3f),
                        700f - (p * 4f),
                        0.84f + (p * 0.001f),
                        1300f - (p * 10f)
                };
            }
            case 5: // Advanced
                return new float[]{
                        clampF(Settings.Secure.getFloat(mResolver, KEY_DAMPING_X, 0.82f), 0.50f, 1.00f),
                        clampF(Settings.Secure.getFloat(mResolver, KEY_DAMPING_Y, 0.82f), 0.50f, 1.00f),
                        clampF(Settings.Secure.getFloat(mResolver, KEY_STIFFNESS_X, 240f), 50f, 900f),
                        clampF(Settings.Secure.getFloat(mResolver, KEY_STIFFNESS_Y, 240f), 50f, 900f),
                        clampF(Settings.Secure.getFloat(mResolver, KEY_SCALE_DAMPING, 0.86f), 0.50f, 1.00f),
                        clampF(Settings.Secure.getFloat(mResolver, KEY_SCALE_STIFFNESS, 300f), 50f, 900f),
                        clamp(Settings.Secure.getInt(mResolver, KEY_APP_OPEN_DURATION, 450), 180, 1200),
                        clamp(Settings.Secure.getInt(mResolver, KEY_HOME_DURATION, 480), 180, 1800),
                        clampF(Settings.Secure.getFloat(mResolver, KEY_HOME_SCALE, 0.88f), 0.65f, 0.98f),
                        clamp(Settings.Secure.getInt(mResolver, KEY_WALLPAPER_DURATION, 850), 200, 2000)
                };
            case 0: // Dynamic
            default:
                return new float[]{0.80f, 0.80f, 300f, 300f, 0.75f, 350f, 380f, 350f, 0.90f, 450f};
        }
    }

    private void updateTelemetryAndPreview() {
        float[] v = resolveCurrentPhysicsArray();
        if (mPhysicsTelemetryText != null) {
            mPhysicsTelemetryText.setText(String.format(Locale.US,
                    "Stiffness X/Y: %.0f / %.0f  •  Damping X/Y: %.2f / %.2f  •  Scale: %.0f (%.2f)  •  Açılış: %.0f ms",
                    v[2], v[3], v[0], v[1], v[5], v[4], v[6]));
        }
        if (mSpringPreviewView != null) {
            mSpringPreviewView.setPhysics(v[0], v[1], v[2], v[3], v[4], v[5]);
            mSpringPreviewView.triggerPreviewAnimation();
        }
    }

    // =========================================================================
    // TAB 2: HOME UP • ANA EKRAN, KLASÖR VE GÖREV DEĞİŞTİRİCİ (TASK CHANGER)
    // =========================================================================

    private void populateHomeUpLayoutTab(LinearLayout parent) {
        parent.removeAllViews();

        // CARD 1: Ana Ekran Izgarası ve İkon Ölçekleme
        LinearLayout homeCard = createModuleCard(
                R.drawable.ic_settings_display_filled,
                "Ana Ekran Izgarası ve İkon Boyutu",
                "INVARIANTDEVICEPROFILE CANLI IZGARA VE ÖLÇEK MOTORU");

        String curGrid = Settings.Secure.getString(mResolver, KEY_GRID_NAME);
        if (curGrid == null || curGrid.isEmpty()) curGrid = "4_by_4";
        final TextView gridSummary = new TextView(mContext);
        addActionRow(homeCard,
                "Ana Ekran Sütun ve Satır Izgarası",
                "Aktif ızgara düzeni: " + formatGridLabel(curGrid),
                gridSummary,
                v -> showHomeGridDialog(gridSummary));

        int iconScalePct = clamp(Settings.Secure.getInt(mResolver, KEY_ICON_SCALE_PCT, 100), 80, 115);
        addSliderControl(homeCard,
                "Uygulama İkon Boyutu Ölçeği",
                80, 115, iconScalePct,
                val -> "%" + val + (val == 100 ? " (Standart)" : ""),
                val -> Settings.Secure.putInt(mResolver, KEY_ICON_SCALE_PCT, val));

        boolean hideLabels = Settings.Secure.getInt(mResolver, KEY_HIDE_LABELS, 0) == 1;
        addSwitchRow(homeCard,
                "Ana Ekran İkon Etiketlerini Gizle",
                "Ana ekrandaki uygulama ve klasör isimlerini gizleyerek minimal görünüm sağlar",
                hideLabels,
                checked -> Settings.Secure.putInt(mResolver, KEY_HIDE_LABELS, checked ? 1 : 0));

        parent.addView(homeCard);

        // CARD 2: Home Up Pop-up Klasör Düzeni
        LinearLayout folderCard = createModuleCard(
                R.drawable.ic_settings_system_dashboard_filled,
                "Pop-up Klasör ve Klasör İçi Izgara",
                "HOME UP POPUP FOLDER MİMARİSİ");

        boolean popupFolder = Settings.Secure.getInt(mResolver, KEY_POPUP_FOLDER, 0) == 1;
        addSwitchRow(folderCard,
                "Pop-up Klasör Görünümü",
                "Klasörleri tam ekran ortalamak yerine dokunulan klasör ikonunun hemen yanında açılır pencere olarak açar",
                popupFolder,
                checked -> Settings.Secure.putInt(mResolver, KEY_POPUP_FOLDER, checked ? 1 : 0));

        int folderGrid = clamp(Settings.Secure.getInt(mResolver, KEY_FOLDER_GRID, 0), 0, 3);
        final String[] folderGridLabels = {
                "Otomatik (Ana Ekran Izgarasına Uyumlu)",
                "3x3 Kompakt Klasör Düzeni",
                "4x4 Geniş Klasör Düzeni",
                "4x5 Maksimum Yoğunluklu Klasör"
        };
        final TextView folderGridSummary = new TextView(mContext);
        addActionRow(folderCard,
                "Klasör İçi Sütun ve Satır Yoğunluğu",
                folderGridLabels[folderGrid],
                folderGridSummary,
                v -> {
                    int cur = clamp(Settings.Secure.getInt(mResolver, KEY_FOLDER_GRID, 0), 0, 3);
                    new AlertDialog.Builder(mContext)
                            .setTitle("Klasör İçi Izgara Yoğunluğu")
                            .setSingleChoiceItems(folderGridLabels, cur, (d, which) -> {
                                Settings.Secure.putInt(mResolver, KEY_FOLDER_GRID, which);
                                folderGridSummary.setText(folderGridLabels[which]);
                                vibrateTick();
                                d.dismiss();
                            })
                            .show();
                });

        parent.addView(folderCard);

        // CARD 3: Task Changer (Son Kullanılanlar / Recents Görünümü)
        LinearLayout recentsCard = createModuleCard(
                R.drawable.ic_settings_system_dashboard_expressive,
                "Görev Değiştirici (Task Changer)",
                "RECENTSVIEW VE TASKVIEW DÖNÜŞÜM MOTORU");

        int taskStyle = clamp(Settings.Secure.getInt(mResolver, KEY_TASK_CHANGER_STYLE, 0), 0, 2);
        final String[] taskStyleLabels = {
                "Standart Kart (Varsayılan AOSP/Lineage Görünümü)",
                "Yığın / Stack (3B Derinlikli Üst Üste binen Kartlar)",
                "Kompakt Şerit / Mini Mod (Çoklu Görev Görünümü)"
        };
        final TextView taskStyleSummary = new TextView(mContext);
        addActionRow(recentsCard,
                "Son Kullanılanlar Kart Düzeni",
                taskStyleLabels[taskStyle],
                taskStyleSummary,
                v -> {
                    int cur = clamp(Settings.Secure.getInt(mResolver, KEY_TASK_CHANGER_STYLE, 0), 0, 2);
                    new AlertDialog.Builder(mContext)
                            .setTitle("Son Kullanılanlar Kart Düzeni")
                            .setSingleChoiceItems(taskStyleLabels, cur, (d, which) -> {
                                Settings.Secure.putInt(mResolver, KEY_TASK_CHANGER_STYLE, which);
                                taskStyleSummary.setText(taskStyleLabels[which]);
                                vibrateTick();
                                d.dismiss();
                            })
                            .show();
                });

        boolean focusPrev = Settings.Secure.getInt(mResolver, KEY_RECENTS_FOCUS_PREV, 0) == 1;
        addSwitchRow(recentsCard,
                "Açılışta Önceki Uygulamayı Ortala",
                "Son kullanılanlar ekranı açıldığında aktif uygulama yerine bir önceki uygulamaya odaklanır",
                focusPrev,
                checked -> Settings.Secure.putInt(mResolver, KEY_RECENTS_FOCUS_PREV, checked ? 1 : 0));

        parent.addView(recentsCard);
    }

    private String formatGridLabel(String gridName) {
        if ("3_by_3".equals(gridName)) return "3x3 (Büyük İkonlar)";
        if ("5_by_5".equals(gridName)) return "5x5 (Geniş Düzen)";
        if ("6_by_5".equals(gridName)) return "6x5 (Yüksek Yoğunluk)";
        return "4x4 (Standart Dengeli)";
    }

    private void showHomeGridDialog(final TextView summaryView) {
        final String[] labels = {
                "3x3 (Büyük İkonlar)",
                "4x4 (Standart Dengeli)",
                "5x5 (Geniş Düzen)",
                "6x5 (Yüksek Yoğunluk)"
        };
        final String[] values = {"3_by_3", "4_by_4", "5_by_5", "6_by_5"};
        String cur = Settings.Secure.getString(mResolver, KEY_GRID_NAME);
        int checked = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(cur)) checked = i;
        }
        new AlertDialog.Builder(mContext)
                .setTitle("Ana Ekran Sütun ve Satır Izgarası")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    String chosen = values[which];
                    Settings.Secure.putString(mResolver, KEY_GRID_NAME, chosen);
                    try {
                        ContentValues cv = new ContentValues();
                        cv.put("name", chosen);
                        mResolver.update(
                                Uri.parse("content://com.android.launcher3.grid_control/default_grid"),
                                cv, null, null);
                    } catch (Throwable ignored) {}
                    if (summaryView != null) {
                        summaryView.setText("Aktif ızgara düzeni: " + labels[which]);
                    }
                    vibrateTick();
                    d.dismiss();
                })
                .show();
    }

    // =========================================================================
    // TAB 3: SİSTEM ARAÇLARI VE MODÜLLER
    // =========================================================================

    private void populateSystemModules(LinearLayout parent) {
        parent.removeAllViews();

        // MODULE 1: Kilit Ekranı ve Bildirim Düzeni
        LinearLayout lockCard = createModuleCard(
                R.drawable.ic_settings_security_filled,
                "Kilit Ekranı ve Bildirim Düzeni",
                "DERİNLİK KATMANI, EKRAN ZAMAN AŞIMI VE KOMPAKT BİLDİRİMLER");

        int timeoutMs = Settings.System.getIntForUser(
                mResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000, UserHandle.USER_CURRENT);
        int timeoutSec = Math.max(15, timeoutMs / 1000);
        final TextView timeoutSummary = new TextView(mContext);
        addActionRow(lockCard,
                "Ekran Zaman Aşımı Süresi",
                "Hareketsizlik sonrası ekranın kapanma süresi (" + timeoutSec + " sn)",
                timeoutSummary,
                v -> showScreenTimeoutDialog(timeoutSummary));

        boolean briefPopup = Settings.Secure.getIntForUser(
                mResolver, "ax_dynamic_bar_compact_notifications", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(lockCard,
                "Kompakt Bildirim Hapı",
                "Üst bildirimleri ekranı kaplamayan kompakt hap biçiminde gösterir",
                briefPopup,
                checked -> Settings.Secure.putIntForUser(
                        mResolver, "ax_dynamic_bar_compact_notifications", checked ? 1 : 0, UserHandle.USER_CURRENT));

        addActionRow(lockCard,
                "Derinlik ve Saat Konum Stüdyosunu Aç",
                "Üst üste saat örtülme koruması, özne/saat hizalama ve canlı önizleme",
                null,
                v -> new SubSettingLauncher(mContext)
                        .setDestination("org.evolution.settings.fragments.lockscreen.WallpaperDepth")
                        .setTitleText("Derinlik ve Saat Konum Stüdyosu")
                        .setSourceMetricsCategory(getMetricsCategory())
                        .launch());
        parent.addView(lockCard);

        // MODULE 2: Sistem Geçiş Hızı ve Hızlı Panel
        LinearLayout qsCard = createModuleCard(
                R.drawable.ic_settings_display_filled,
                "Sistem Geçiş Hızı ve Hızlı Panel",
                "PENCERE ANİMASYON ÖLÇEĞİ, IZGARA YOĞUNLUĞU VE MEDYA ÇUBUĞU");

        float curScale = Settings.Global.getFloat(mResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1.0f);
        final TextView animSummary = new TextView(mContext);
        addActionRow(qsCard,
                "Sistem Pencere ve Geçiş Animasyon Hızı",
                "Aktif ölçek: " + formatAnimScaleLabel(curScale),
                animSummary,
                v -> showAnimationScaleDialog(animSummary));

        int qsCols = Settings.System.getIntForUser(
                mResolver, "qs_columns_portrait", 4, UserHandle.USER_CURRENT);
        if (qsCols < 3) qsCols = 4;
        final TextView qsColSummary = new TextView(mContext);
        addActionRow(qsCard,
                "Hızlı Ayarlar Sütun Yoğunluğu",
                "Portre modunda satır başına " + qsCols + " kare gösteriliyor",
                qsColSummary,
                v -> showQsColumnsDialog(qsColSummary));

        boolean waveform = Settings.System.getIntForUser(
                mResolver, "media_waveform_seekbar", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(qsCard,
                "Medya Dalga Formu Çubuğu",
                "Medya oynatıcı ilerleme çubuğunu dinamik ses dalgası olarak çizer",
                waveform,
                checked -> Settings.System.putIntForUser(
                        mResolver, "media_waveform_seekbar", checked ? 1 : 0, UserHandle.USER_CURRENT));
        parent.addView(qsCard);

        // MODULE 3: Ses Kanal Mikseri ve Ortam Sentezleyici
        LinearLayout soundCard = createModuleCard(
                R.drawable.ic_settings_voice_calls,
                "Ses Mikseri ve Ortam Sentezleyici",
                "KANAL BAZLI SES DENGESİ VE CANLI DSP ODAK SESLERİ");

        addActionRow(soundCard,
                "Bağımsız Kanal ve Oturum Ses Mikseri",
                "Medya, Zil, Bildirim, Alarm düzeylerini ve aktif ses oturumlarını dengele",
                null,
                v -> showPerStreamAndAppVolumeMixerDialog());

        soundCard.addView(buildAmbientSoundscapeWidget());
        parent.addView(soundCard);

        // MODULE 4: Donanım Kısayolu ve Çoklu Pencere Motoru
        LinearLayout multiWindowCard = createModuleCard(
                R.drawable.ic_settings_system_dashboard_expressive,
                "Çoklu Pencere ve Donanım Kısayolları",
                "ZORUNLU BÖLÜNMÜŞ EKRAN VE GÜÇ TUŞU FENERİ");

        boolean forceResizable = Settings.Global.getInt(
                mResolver, "enable_non_resizable_multi_window", 0) == 1;
        addSwitchRow(multiWindowCard,
                "Tüm Uygulamalarda Zorunlu Çoklu Pencere",
                "Bölünmüş ekranı veya serbest pencereyi desteklemeyen uygulamaları çoklu pencere uyumlu yapar",
                forceResizable,
                checked -> {
                    Settings.Global.putInt(mResolver, "enable_non_resizable_multi_window", checked ? 1 : 0);
                    Settings.Global.putInt(mResolver, "force_resizable_activities", checked ? 1 : 0);
                    Settings.Global.putInt(mResolver, "enable_freeform_support", checked ? 1 : 0);
                });

        boolean torchPower = Settings.System.getIntForUser(
                mResolver, "torch_long_press_power_gesture", 0, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(multiWindowCard,
                "Ekran Kapalıyken Güç Tuşuyla El Feneri",
                "Kilit ekranını açmadan güç tuşuna basılı tutarak el fenerini yakar",
                torchPower,
                checked -> Settings.System.putIntForUser(
                        mResolver, "torch_long_press_power_gesture", checked ? 1 : 0, UserHandle.USER_CURRENT));
        parent.addView(multiWindowCard);

        // MODULE 5: Arka Plan Etkinlik ve Uyandırma Günlüğü
        LinearLayout logCard = createModuleCard(
                R.drawable.ic_settings_privacy_filled,
                "Arka Plan Etkinlik ve Uyandırma Günlüğü",
                "TİTREŞİM, EKRAN UYANDIRMA VE İZİN DENETİMİ");

        TextView detectiveDesc = new TextView(mContext);
        detectiveDesc.setText("Son saatlerde hangi uygulamaların etkinleştiğini ve titreşim izni kullandığını gerçek zamanlı UsageStats ve AppOps verileriyle listeler.");
        detectiveDesc.setTextColor(mColorTextSecondary);
        detectiveDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        detectiveDesc.setPadding(0, dp(2), 0, dp(10));
        logCard.addView(detectiveDesc);

        LinearLayout liveLogBox = new LinearLayout(mContext);
        liveLogBox.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable logBg = new GradientDrawable();
        logBg.setColor(mColorSurfaceVariant);
        logBg.setCornerRadius(dp(16));
        logBg.setStroke(dp(1), mColorBorder);
        liveLogBox.setBackground(logBg);
        liveLogBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        populateLiveActivityLogs(liveLogBox);
        logCard.addView(liveLogBox);

        addActionRow(logCard,
                "Etkinlik Günlüğünü Yenile",
                "Uygulama etkinlik ve izin geçmişini yeniden tara",
                null,
                v -> {
                    vibrateTick();
                    populateLiveActivityLogs(liveLogBox);
                    Toast.makeText(mContext, "Etkinlik günlüğü güncellendi", Toast.LENGTH_SHORT).show();
                });

        parent.addView(logCard);
    }

    // =========================================================================
    // LIVE SPRING PHYSICS PREVIEW VIEW (Modeled on Home Up IconMovePreview)
    // =========================================================================

    private final class HomeUpSpringPreviewView extends View {
        private final Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mTargetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mIconBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mTrajectoryPath = new Path();
        private final RectF mRect = new RectF();

        private float mDampX = 0.80f;
        private float mDampY = 0.80f;
        private float mStiffX = 300f;
        private float mStiffY = 300f;
        private float mScaleDamp = 0.75f;
        private float mScaleStiff = 350f;

        private float mProgressTimeSec = 1.0f;
        private ValueAnimator mAnimator;

        HomeUpSpringPreviewView(Context context) {
            super(context);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(mColorSurfaceVariant);
            bg.setCornerRadius(dp(18));
            bg.setStroke(dp(1), mColorBorder);
            setBackground(bg);

            mGridPaint.setStyle(Paint.Style.STROKE);
            mGridPaint.setStrokeWidth(dp(1));
            mGridPaint.setColor(mColorBorder);

            mPathPaint.setStyle(Paint.Style.STROKE);
            mPathPaint.setStrokeWidth(dp(2));
            mPathPaint.setColor(withAlpha(mColorAccent, 110));
            mPathPaint.setPathEffect(new DashPathEffect(new float[]{dp(6), dp(4)}, 0));

            mTargetPaint.setStyle(Paint.Style.STROKE);
            mTargetPaint.setStrokeWidth(dp(2));
            mTargetPaint.setColor(withAlpha(mColorTextSecondary, 110));

            mIconPaint.setStyle(Paint.Style.FILL);
            mIconPaint.setColor(withAlpha(mColorAccent, 55));

            mIconBorderPaint.setStyle(Paint.Style.STROKE);
            mIconBorderPaint.setStrokeWidth(dp(2));
            mIconBorderPaint.setColor(mColorAccent);

            float[] init = resolveCurrentPhysicsArray();
            setPhysics(init[0], init[1], init[2], init[3], init[4], init[5]);

            setOnClickListener(v -> {
                vibrateTick();
                triggerPreviewAnimation();
            });
        }

        void setPhysics(float dx, float dy, float sx, float sy, float sd, float ss) {
            mDampX = dx;
            mDampY = dy;
            mStiffX = sx;
            mStiffY = sy;
            mScaleDamp = sd;
            mScaleStiff = ss;
            invalidate();
        }

        void triggerPreviewAnimation() {
            stopAnimation();
            mAnimator = ValueAnimator.ofFloat(0f, 1.15f);
            mAnimator.setDuration(1150L);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.addUpdateListener(anim -> {
                mProgressTimeSec = (float) anim.getAnimatedValue();
                invalidate();
            });
            mAnimator.start();
        }

        void stopAnimation() {
            if (mAnimator != null) {
                mAnimator.cancel();
                mAnimator = null;
            }
        }

        private float evalSpring(float stiffness, float damping, float tSec) {
            double w = Math.sqrt(Math.max(10f, stiffness));
            double z = Math.max(0.35, Math.min(1.5, damping));
            if (z < 0.999) {
                double wd = w * Math.sqrt(1.0 - z * z);
                return (float) (1.0 - Math.exp(-z * w * tSec)
                        * (Math.cos(wd * tSec) + (z * w / wd) * Math.sin(wd * tSec)));
            } else {
                return (float) (1.0 - (1.0 + w * tSec) * Math.exp(-w * tSec));
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            // 1. Subtle 8x4 coordinate grid
            int cols = 8;
            int rows = 4;
            for (int i = 1; i < cols; i++) {
                float x = (w * i) / (float) cols;
                canvas.drawLine(x, 0, x, h, mGridPaint);
            }
            for (int j = 1; j < rows; j++) {
                float y = (h * j) / (float) rows;
                canvas.drawLine(0, y, w, y, mGridPaint);
            }

            float startX = w * 0.78f;
            float startY = h * 0.74f;
            float targetX = w * 0.22f;
            float targetY = h * 0.30f;

            float startSize = dp(68);
            float targetSize = dp(38);

            // 2. Target slot outline
            float halfTarget = targetSize / 2f;
            mRect.set(targetX - halfTarget, targetY - halfTarget,
                    targetX + halfTarget, targetY + halfTarget);
            canvas.drawRoundRect(mRect, dp(10), dp(10), mTargetPaint);

            // 3. Analytic trajectory path
            mTrajectoryPath.reset();
            int steps = 48;
            for (int i = 0; i <= steps; i++) {
                float t = (i / (float) steps) * 1.0f;
                float px = startX + (targetX - startX) * evalSpring(mStiffX, mDampX, t);
                float py = startY + (targetY - startY) * evalSpring(mStiffY, mDampY, t);
                if (i == 0) mTrajectoryPath.moveTo(px, py);
                else mTrajectoryPath.lineTo(px, py);
            }
            canvas.drawPath(mTrajectoryPath, mPathPaint);

            // 4. Current animated window/icon position & scale
            float progX = evalSpring(mStiffX, mDampX, mProgressTimeSec);
            float progY = evalSpring(mStiffY, mDampY, mProgressTimeSec);
            float progS = evalSpring(mScaleStiff, mScaleDamp, mProgressTimeSec);

            float cx = startX + (targetX - startX) * progX;
            float cy = startY + (targetY - startY) * progY;
            float curSize = Math.max(dp(18), startSize + (targetSize - startSize) * progS);
            float half = curSize / 2f;
            float corner = dp(10) + (1f - Math.min(1f, progS)) * dp(8);

            mRect.set(cx - half, cy - half, cx + half, cy + half);
            canvas.drawRoundRect(mRect, corner, corner, mIconPaint);
            canvas.drawRoundRect(mRect, corner, corner, mIconBorderPaint);
        }
    }

    // =========================================================================
    // DIALOGS & INTERACTIVE SYSTEM TOOLS
    // =========================================================================

    private void showScreenTimeoutDialog(final TextView summaryView) {
        final String[] labels = {
                "15 Saniye",
                "30 Saniye (Standart)",
                "60 Saniye (1 Dakika)",
                "120 Saniye (2 Dakika)",
                "300 Saniye (5 Dakika)"
        };
        final int[] valuesMs = {15000, 30000, 60000, 120000, 300000};
        int curMs = Settings.System.getIntForUser(
                mResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000, UserHandle.USER_CURRENT);
        int checked = 1;
        for (int i = 0; i < valuesMs.length; i++) {
            if (valuesMs[i] == curMs) checked = i;
        }
        new AlertDialog.Builder(mContext)
                .setTitle("Ekran Zaman Aşımı Süresi")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    Settings.System.putIntForUser(
                            mResolver, Settings.System.SCREEN_OFF_TIMEOUT, valuesMs[which], UserHandle.USER_CURRENT);
                    if (summaryView != null) {
                        summaryView.setText("Hareketsizlik sonrası ekranın kapanma süresi (" + (valuesMs[which] / 1000) + " sn)");
                    }
                    vibrateTick();
                    d.dismiss();
                })
                .show();
    }

    private String formatAnimScaleLabel(float scale) {
        if (scale <= 0.6f) return "0.5x (Çok Hızlı)";
        if (scale <= 0.92f) return "0.85x (Akıcı ve Hızlı)";
        if (scale <= 1.05f) return "1.0x (Standart Dengeli)";
        return "1.25x (Yumuşak Geçiş)";
    }

    private void showAnimationScaleDialog(final TextView summaryView) {
        final String[] names = {
                "0.5x (Çok Hızlı)",
                "0.85x (Akıcı ve Hızlı)",
                "1.0x (Standart Dengeli)",
                "1.25x (Yumuşak Geçiş)"
        };
        final float[] scales = {0.5f, 0.85f, 1.0f, 1.25f};
        float cur = Settings.Global.getFloat(mResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1.0f);
        int checked = 2;
        if (cur <= 0.6f) checked = 0;
        else if (cur <= 0.92f) checked = 1;
        else if (cur > 1.05f) checked = 3;

        new AlertDialog.Builder(mContext)
                .setTitle("Sistem Pencere ve Geçiş Animasyon Hızı")
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    Settings.Global.putFloat(mResolver, Settings.Global.WINDOW_ANIMATION_SCALE, scales[which]);
                    Settings.Global.putFloat(mResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, scales[which]);
                    Settings.Global.putFloat(mResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scales[which]);
                    if (summaryView != null) {
                        summaryView.setText("Aktif ölçek: " + names[which]);
                    }
                    vibrateTick();
                    d.dismiss();
                })
                .show();
    }

    private void showQsColumnsDialog(final TextView summaryView) {
        final String[] labels = {
                "3 Sütun (Geniş Kart Düzeni)",
                "4 Sütun (Standart Dengeli)",
                "5 Sütun (Kompakt Yoğun Düzen)"
        };
        final int[] vals = {3, 4, 5};
        int cur = Settings.System.getIntForUser(mResolver, "qs_columns_portrait", 4, UserHandle.USER_CURRENT);
        int idx = cur == 3 ? 0 : (cur == 5 ? 2 : 1);
        new AlertDialog.Builder(mContext)
                .setTitle("Hızlı Ayarlar Sütun Yoğunluğu")
                .setSingleChoiceItems(labels, idx, (d, which) -> {
                    Settings.System.putIntForUser(
                            mResolver, "qs_columns_portrait", vals[which], UserHandle.USER_CURRENT);
                    if (summaryView != null) {
                        summaryView.setText("Portre modunda satır başına " + vals[which] + " kare gösteriliyor");
                    }
                    vibrateTick();
                    d.dismiss();
                })
                .show();
    }

    private void showPerStreamAndAppVolumeMixerDialog() {
        final AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;

        LinearLayout box = new LinearLayout(mContext);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(14), pad, dp(10));

        final int[] streams = {
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.STREAM_ALARM
        };
        final String[] streamTitles = {
                "Medya ve Müzik Sesi",
                "Zil Sesi",
                "Bildirim Sesi",
                "Alarm Sesi"
        };

        for (int i = 0; i < streams.length; i++) {
            final int streamType = streams[i];
            int max = Math.max(1, am.getStreamMaxVolume(streamType));
            int cur = am.getStreamVolume(streamType);

            TextView label = new TextView(mContext);
            int pct = (int) Math.round((cur * 100.0) / max);
            label.setText(streamTitles[i] + "  •  %" + pct);
            label.setTextColor(mColorTextPrimary);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            label.setPadding(0, dp(8), 0, dp(2));
            box.addView(label);

            SeekBar sb = new SeekBar(mContext);
            sb.setMax(max);
            sb.setProgress(cur);
            final String baseTitle = streamTitles[i];
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        am.setStreamVolume(streamType, progress, 0);
                        int p = (int) Math.round((progress * 100.0) / max);
                        label.setText(baseTitle + "  •  %" + p);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            box.addView(sb);
        }

        List<AudioPlaybackConfiguration> configs = am.getActivePlaybackConfigurations();
        TextView activeHeader = new TextView(mContext);
        activeHeader.setText("Aktif Ses Oynatan Uygulamalar (" + configs.size() + " oturum)");
        activeHeader.setTextColor(mColorAccent);
        activeHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        activeHeader.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        activeHeader.setPadding(0, dp(14), 0, dp(4));
        box.addView(activeHeader);

        if (configs.isEmpty()) {
            TextView empty = new TextView(mContext);
            empty.setText("Şu anda arka planda aktif medya oynatan bir uygulama yok.");
            empty.setTextColor(mColorTextSecondary);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            box.addView(empty);
        } else {
            for (int i = 0; i < Math.min(4, configs.size()); i++) {
                AudioPlaybackConfiguration cfg = configs.get(i);
                int usage = cfg.getAudioAttributes() != null ? cfg.getAudioAttributes().getUsage() : 0;
                TextView row = new TextView(mContext);
                row.setText("• Aktif Ses Oturumu #" + (i + 1) + " (Kanal: " + usage + ") — Çalıyor");
                row.setTextColor(mColorTextPrimary);
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                row.setPadding(0, dp(3), 0, dp(3));
                box.addView(row);
            }
        }

        new AlertDialog.Builder(mContext)
                .setTitle("Bağımsız Ses Mikseri")
                .setView(box)
                .setPositiveButton("Tamam", null)
                .show();
    }

    private View buildAmbientSoundscapeWidget() {
        LinearLayout container = new LinearLayout(mContext);
        container.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(mColorSurfaceVariant);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), mColorBorder);
        container.setBackground(bg);
        int pad = dp(14);
        container.setPadding(pad, dp(12), pad, dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        container.setLayoutParams(lp);

        TextView header = new TextView(mContext);
        header.setText("Doğal Odak ve Ortam Ses Sentezleyicisi");
        header.setTextColor(mColorTextPrimary);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        container.addView(header);

        mAmbientStatusText = new TextView(mContext);
        mAmbientStatusText.setText("Durum: Hazır (Gerçek zamanlı DSP ses sentezleyici)");
        mAmbientStatusText.setTextColor(mColorTextSecondary);
        mAmbientStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        mAmbientStatusText.setPadding(0, dp(2), 0, dp(8));
        container.addView(mAmbientStatusText);

        HorizontalScrollView hsv = new HorizontalScrollView(mContext);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(mContext);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);

        final String[] labels = {"Durdur", "Gece Yağmuru", "Okyanus Dalgası", "Kamp Ateşi", "Derin Uzay"};
        for (int i = 0; i < labels.length; i++) {
            final int mode = i;
            TextView chip = new TextView(mContext);
            chip.setText(labels[i]);
            chip.setTextColor(i == 0 ? mColorTextSecondary : mColorAccent);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(mColorSurface);
            chipBg.setCornerRadius(dp(99));
            chipBg.setStroke(dp(1), i == 0 ? mColorBorder : withAlpha(mColorAccent, 90));
            chip.setBackground(chipBg);
            chip.setPadding(dp(14), dp(7), dp(14), dp(7));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = dp(8);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                vibrateTick();
                if (mode == 0) {
                    stopAmbientSoundscape();
                } else {
                    startAmbientSoundscape(mode, labels[mode]);
                }
            });
            chipRow.addView(chip);
        }
        hsv.addView(chipRow);
        container.addView(hsv);
        return container;
    }

    private void startAmbientSoundscape(final int mode, final String modeLabel) {
        stopAmbientSoundscape();
        mAmbientPlaying = true;
        if (mAmbientStatusText != null) {
            mAmbientStatusText.setText("Çalıyor: " + modeLabel + " (DSP Sentezi Aktif)");
            mAmbientStatusText.setTextColor(mColorAccent);
        }

        mAmbientThread = new Thread(() -> {
            final int sampleRate = 24000;
            int minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(Math.max(minBuf, 4096))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();

            short[] buffer = new short[1024];
            Random rng = new Random();
            double brown = 0.0;
            double lp1 = 0.0;
            double phase = 0.0;

            try {
                track.play();
                while (mAmbientPlaying && !Thread.currentThread().isInterrupted()) {
                    for (int i = 0; i < buffer.length; i++) {
                        double white = rng.nextDouble() * 2.0 - 1.0;
                        brown = (brown + (0.02 * white)) / 1.02;
                        lp1 = lp1 + 0.08 * (white - lp1);
                        phase += 1.0 / sampleRate;

                        double sample;
                        if (mode == 1) {
                            sample = (lp1 * 0.55 + brown * 0.45) * 0.35;
                        } else if (mode == 2) {
                            double waveEnv = 0.25 + 0.75 * (0.5 + 0.5 * Math.sin(phase * 2.0 * Math.PI * 0.14));
                            sample = (brown * 0.75 + lp1 * 0.25) * waveEnv * 0.55;
                        } else if (mode == 3) {
                            double crackle = (rng.nextDouble() > 0.9975) ? (white * 0.65) : 0.0;
                            sample = (brown * 0.65 + crackle) * 0.40;
                        } else {
                            sample = brown * 0.55;
                        }
                        int pcm = (int) (Math.max(-1.0, Math.min(1.0, sample)) * 14000);
                        buffer[i] = (short) pcm;
                    }
                    track.write(buffer, 0, buffer.length);
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    track.stop();
                    track.release();
                } catch (Exception ignored) {
                }
            }
        }, "BlazeAmbientSynth");
        mAmbientThread.start();
    }

    private void stopAmbientSoundscape() {
        mAmbientPlaying = false;
        if (mAmbientThread != null) {
            mAmbientThread.interrupt();
            mAmbientThread = null;
        }
        if (mAmbientStatusText != null) {
            mAmbientStatusText.setText("Durum: Durduruldu");
            mAmbientStatusText.setTextColor(mColorTextSecondary);
        }
    }

    private void populateLiveActivityLogs(LinearLayout container) {
        container.removeAllViews();
        PackageManager pm = mContext.getPackageManager();
        UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        List<UsageStats> stats = usm != null
                ? usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 3600_000L * 12, now)
                : Collections.emptyList();

        List<UsageStats> sorted = new ArrayList<>();
        if (stats != null) {
            for (UsageStats s : stats) {
                if (s.getLastTimeUsed() > 0) {
                    sorted.add(s);
                }
            }
            sorted.sort((a, b) -> Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()));
        }

        AppOpsManager appOps = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
        int count = 0;
        for (UsageStats us : sorted) {
            if (count >= 6) break;
            String pkg = us.getPackageName();
            if ("android".equals(pkg)) continue;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                String appName = pm.getApplicationLabel(ai).toString();
                String timeStr = sdf.format(new Date(us.getLastTimeUsed()));

                StringBuilder tags = new StringBuilder();
                if (appOps != null) {
                    int vibMode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_VIBRATE, ai.uid, pkg);
                    if (vibMode == AppOpsManager.MODE_ALLOWED) tags.append("Titreşim • ");
                }
                tags.append("Etkinlik / Uyandırma");

                TextView row = new TextView(mContext);
                row.setText(timeStr + "  •  " + appName + "  —  " + tags);
                row.setTextColor(mColorTextPrimary);
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                row.setPadding(0, dp(4), 0, dp(4));
                container.addView(row);
                count++;
            } catch (Exception ignored) {
            }
        }

        if (count == 0) {
            TextView empty = new TextView(mContext);
            empty.setText("Son saatlerde olağandışı arka plan titreşimi veya ekran uyandırma tetikleyicisi saptanmadı.");
            empty.setTextColor(mColorTextSecondary);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            container.addView(empty);
        }
    }

    // =========================================================================
    // UI CARD & ROW BUILDERS
    // =========================================================================

    private LinearLayout createModuleCard(int iconResId, String title, String badgeText) {
        LinearLayout card = new LinearLayout(mContext);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(mColorSurface);
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), mColorBorder);
        card.setBackground(bg);
        int pad = dp(18);
        card.setPadding(pad, dp(16), pad, dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        card.setLayoutParams(lp);

        LinearLayout headerRow = new LinearLayout(mContext);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(8));

        FrameLayout iconBadge = createIconBadge(iconResId);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        iconLp.rightMargin = dp(12);
        headerRow.addView(iconBadge, iconLp);

        LinearLayout titleCol = new LinearLayout(mContext);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = new TextView(mContext);
        badge.setText(badgeText);
        badge.setTextColor(mColorAccent);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        badge.setLetterSpacing(0.04f);
        titleCol.addView(badge);

        TextView header = new TextView(mContext);
        header.setText(title);
        header.setTextColor(mColorTextPrimary);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        header.setPadding(0, dp(1), 0, 0);
        titleCol.addView(header);

        headerRow.addView(titleCol);
        card.addView(headerRow);

        return card;
    }

    private interface OnToggleChanged {
        void onChanged(boolean checked);
    }

    private interface SliderValueFormatter {
        String format(int value);
    }

    private interface OnSliderChanged {
        void onChanged(int value);
    }

    private void addSliderControl(
            LinearLayout parent,
            String title,
            final int min,
            final int max,
            int initialValue,
            final SliderValueFormatter formatter,
            final OnSliderChanged listener) {
        LinearLayout wrap = new LinearLayout(mContext);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, dp(8), 0, dp(6));

        LinearLayout topRow = new LinearLayout(mContext);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(mContext);
        titleView.setText(title);
        titleView.setTextColor(mColorTextPrimary);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        topRow.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView valView = new TextView(mContext);
        valView.setText(formatter.format(initialValue));
        valView.setTextColor(mColorAccent);
        valView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        valView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        topRow.addView(valView);

        wrap.addView(topRow);

        SeekBar sb = new SeekBar(mContext);
        sb.setMax(Math.max(1, max - min));
        sb.setProgress(Math.max(0, Math.min(max - min, initialValue - min)));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actual = min + progress;
                valView.setText(formatter.format(actual));
                if (fromUser && listener != null) {
                    listener.onChanged(actual);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                vibrateTick();
            }
        });
        wrap.addView(sb);
        parent.addView(wrap);
    }

    private void addSwitchRow(
            LinearLayout parent,
            String title,
            String summary,
            boolean initialChecked,
            final OnToggleChanged listener) {
        LinearLayout row = new LinearLayout(mContext);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout textCol = new LinearLayout(mContext);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.rightMargin = dp(12);
        textCol.setLayoutParams(colLp);

        TextView t = new TextView(mContext);
        t.setText(title);
        t.setTextColor(mColorTextPrimary);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textCol.addView(t);

        TextView s = new TextView(mContext);
        s.setText(summary);
        s.setTextColor(mColorTextSecondary);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        s.setPadding(0, dp(2), 0, 0);
        textCol.addView(s);

        final Switch sw = new Switch(mContext);
        sw.setChecked(initialChecked);
        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            vibrateTick();
            if (listener != null) listener.onChanged(isChecked);
        });

        row.setOnClickListener(v -> sw.setChecked(!sw.isChecked()));
        row.addView(textCol);
        row.addView(sw);
        parent.addView(row);
    }

    private void addActionRow(
            LinearLayout parent,
            String title,
            String summary,
            TextView customSummaryRef,
            View.OnClickListener clickListener) {
        LinearLayout row = new LinearLayout(mContext);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout textCol = new LinearLayout(mContext);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.rightMargin = dp(12);
        textCol.setLayoutParams(colLp);

        TextView t = new TextView(mContext);
        t.setText(title);
        t.setTextColor(mColorTextPrimary);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textCol.addView(t);

        TextView s = customSummaryRef != null ? customSummaryRef : new TextView(mContext);
        s.setText(summary);
        s.setTextColor(mColorAccent);
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        s.setPadding(0, dp(2), 0, 0);
        textCol.addView(s);

        TextView chev = new TextView(mContext);
        chev.setText("›");
        chev.setTextColor(mColorTextSecondary);
        chev.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);

        row.addView(textCol);
        row.addView(chev);
        row.setOnClickListener(clickListener);
        parent.addView(row);
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private float clampF(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private void vibrateTick() {
        try {
            Vibrator vib = mContext.getSystemService(Vibrator.class);
            if (vib != null && vib.hasVibrator()) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
            }
        } catch (Exception ignored) {
        }
    }

    private int dp(int val) {
        return Math.round(val * mContext.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.VIEW_UNKNOWN;
    }
}
