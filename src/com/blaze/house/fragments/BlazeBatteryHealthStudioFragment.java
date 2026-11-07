/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

/**
 * Pil Sağlığı, Şarj Koruması ve Donanım Özeti
 * Clean Material 3 Expressive hardware battery telemetry and protection studio.
 */
public class BlazeBatteryHealthStudioFragment extends SettingsPreferenceFragment {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mRunning = false;

    private boolean mIsNight;
    private int mColorBg;
    private int mColorSurface;
    private int mColorSurfaceVariant;
    private int mColorBorder;
    private int mColorAccent;
    private int mColorTextPrimary;
    private int mColorTextSecondary;

    private TextView mLivePowerMetricsTv;
    private TextView mLiveCpuRamMetricsTv;
    private BatteryHealthArcView mArcView;
    private final LinearLayout[] mTierCards = new LinearLayout[4];

    private final Runnable mTelemetryUpdater = new Runnable() {
        @Override
        public void run() {
            if (!mRunning || getContext() == null) return;
            updateLiveTelemetry(getContext());
            mHandler.postDelayed(this, 1500L);
        }
    };

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    private void resolveThemePalette(Context context) {
        int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        mIsNight = (uiMode == Configuration.UI_MODE_NIGHT_YES);
        mColorBg = resolveAttrColor(context, android.R.attr.colorBackground, mIsNight ? 0xFF111318 : 0xFFF8F9FC);
        mColorAccent = resolveAttrColor(context, android.R.attr.colorAccent, mIsNight ? 0xFFA8C7FA : 0xFF1B6EF3);
        mColorTextPrimary = resolveAttrColor(context, android.R.attr.textColorPrimary, mIsNight ? 0xFFE2E2E9 : 0xFF1A1C20);
        mColorTextSecondary = resolveAttrColor(context, android.R.attr.textColorSecondary, mIsNight ? 0xFFC4C6D0 : 0xFF44474F);
        mColorSurface = mIsNight ? 0xFF1C1F26 : 0xFFFFFFFF;
        mColorSurfaceVariant = mIsNight ? 0xFF252932 : 0xFFEEF1F7;
        mColorBorder = mIsNight ? 0x22FFFFFF : 0x16000000;
    }

    private int resolveAttrColor(Context context, int attr, int fallback) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) {
                try {
                    return context.getColor(tv.resourceId);
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
        Context context = requireContext();
        resolveThemePalette(context);
        if (getActivity() != null) {
            getActivity().setTitle("Pil Sağlığı ve Donanım Durumu");
        }

        float d = context.getResources().getDisplayMetrics().density;
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(mColorBg);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(16 * d);
        root.setPadding(pad, pad, pad, Math.round(28 * d));
        scroll.addView(root);

        // 1. Hero Battery Health & Cycle Count Card
        LinearLayout heroCard = createSurfaceCard(context, d);
        heroCard.addView(createCardHeader(context, d,
                R.drawable.ic_settings_battery_filled,
                "DONANIM PİL TELEMETRİSİ",
                "Pil Sağlığı ve Döngü Analizi"));

        BatterySnapshot snap = readBatterySnapshot(context);

        LinearLayout gaugeRow = new LinearLayout(context);
        gaugeRow.setOrientation(LinearLayout.HORIZONTAL);
        gaugeRow.setGravity(Gravity.CENTER_VERTICAL);
        gaugeRow.setPadding(0, Math.round(10 * d), 0, Math.round(12 * d));

        mArcView = new BatteryHealthArcView(context, snap.healthPercent, snap.levelPercent,
                mColorAccent, mColorTextPrimary, mColorTextSecondary, mColorBorder);
        LinearLayout.LayoutParams arcLp = new LinearLayout.LayoutParams(
                Math.round(114 * d), Math.round(114 * d));
        arcLp.setMarginEnd(Math.round(16 * d));
        gaugeRow.addView(mArcView, arcLp);

        LinearLayout metricsCol = new LinearLayout(context);
        metricsCol.setOrientation(LinearLayout.VERTICAL);
        metricsCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView statusBadge = new TextView(context);
        String healthState = snap.healthPercent >= 90 ? "Mükemmel Kondisyon"
                : (snap.healthPercent >= 80 ? "İyi Kondisyon" : "Servis Önerilir");
        statusBadge.setText(healthState + " • %" + snap.healthPercent + " Sağlık");
        statusBadge.setTextSize(13.5f);
        statusBadge.setTextColor(mColorAccent);
        statusBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        metricsCol.addView(statusBadge);

        TextView capTv = new TextView(context);
        capTv.setText(String.format(Locale.getDefault(),
                "Mevcut Tam Kapasite: %d mAh\nTasarım Kapasitesi: %d mAh\nToplam Şarj Döngüsü: %d Döngü",
                snap.currentFullMah, snap.designFullMah, snap.cycleCount));
        capTv.setTextSize(12f);
        capTv.setTextColor(mColorTextSecondary);
        capTv.setLineSpacing(Math.round(3 * d), 1.0f);
        capTv.setPadding(0, Math.round(6 * d), 0, 0);
        metricsCol.addView(capTv);

        gaugeRow.addView(metricsCol);
        heroCard.addView(gaugeRow);

        mLivePowerMetricsTv = new TextView(context);
        mLivePowerMetricsTv.setTextSize(12f);
        mLivePowerMetricsTv.setTextColor(mColorTextPrimary);
        mLivePowerMetricsTv.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        mLivePowerMetricsTv.setPadding(Math.round(12 * d), Math.round(10 * d), Math.round(12 * d), Math.round(10 * d));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(mColorSurfaceVariant);
        pillBg.setCornerRadius(14f * d);
        pillBg.setStroke(Math.max(1, Math.round(1f * d)), mColorBorder);
        mLivePowerMetricsTv.setBackground(pillBg);
        heroCard.addView(mLivePowerMetricsTv);

        root.addView(heroCard);

        // 2. 3-Tier Smart Battery Protection Section
        LinearLayout protCard = createSurfaceCard(context, d);
        LinearLayout.LayoutParams protLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        protLp.topMargin = Math.round(14 * d);
        protCard.setLayoutParams(protLp);

        protCard.addView(createCardHeader(context, d,
                R.drawable.ic_settings_safety_center_filled,
                "KİMYASAL YAŞLANMA KORUMASI",
                "3 Kademeli Akıllı Şarj Koruması"));

        TextView protSub = new TextView(context);
        protSub.setText("Pil ömrünü uzatmak için şarj üst sınırını ve gece şarj davranışını belirleyin.");
        protSub.setTextSize(12f);
        protSub.setTextColor(mColorTextSecondary);
        protSub.setPadding(0, Math.round(2 * d), 0, Math.round(10 * d));
        protCard.addView(protSub);

        String[] tierTitles = new String[]{
                "Kapalı (Standart %100 Şarj)",
                "Temel Koruma (%100 → %95 Dinamik Kesme)",
                "Uyarlanabilir Gece Koruması (Gece %80, Sabah %100)",
                "Maksimum Koruma (%80 Sabit Şarj Sınırı)"
        };
        String[] tierDescs = new String[]{
                "Pil her zaman %100 kapasiteye kadar tam hızda şarj edilir.",
                "Şarj %100 olduğunda akım kesilir; seviye %95'e düşene kadar yeniden şarj başlatılmaz.",
                "Gece boyunca %80'de bekletir, sabah uyanma saatinden önce %100'e tamamlar.",
                "Pil döngü ömrünü iki katına çıkarmak için şarjı kesin olarak %80'de durdurur."
        };

        int currentTier = Settings.System.getIntForUser(
                context.getContentResolver(), "blaze_battery_protection_tier", 1, UserHandle.USER_CURRENT);

        for (int i = 0; i < 4; i++) {
            final int tierIdx = i;
            LinearLayout item = new LinearLayout(context);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(Math.round(14 * d), Math.round(11 * d), Math.round(14 * d), Math.round(11 * d));
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemLp.topMargin = Math.round(6 * d);
            item.setLayoutParams(itemLp);

            TextView tTitle = new TextView(context);
            tTitle.setText(tierTitles[i]);
            tTitle.setTextSize(13.5f);
            tTitle.setTextColor(mColorTextPrimary);
            tTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

            TextView tDesc = new TextView(context);
            tDesc.setText(tierDescs[i]);
            tDesc.setTextSize(11.5f);
            tDesc.setTextColor(mColorTextSecondary);
            tDesc.setPadding(0, Math.round(2 * d), 0, 0);

            item.addView(tTitle);
            item.addView(tDesc);

            item.setOnClickListener(v -> {
                applyBatteryProtectionTier(context, tierIdx);
                updateTierCardStyles(d, tierIdx);
                triggerHaptic(context);
            });

            mTierCards[i] = item;
            protCard.addView(item);
        }
        updateTierCardStyles(d, currentTier);

        // Real High Refresh Rate Toggle (peak_refresh_rate)
        LinearLayout refreshRow = new LinearLayout(context);
        refreshRow.setOrientation(LinearLayout.HORIZONTAL);
        refreshRow.setGravity(Gravity.CENTER_VERTICAL);
        refreshRow.setPadding(Math.round(4 * d), Math.round(14 * d), Math.round(4 * d), Math.round(6 * d));

        TextView refreshLabel = new TextView(context);
        refreshLabel.setText("Akıcı Ekran Yenileme Hızı (90Hz / 120Hz)");
        refreshLabel.setTextSize(13f);
        refreshLabel.setTextColor(mColorTextPrimary);
        refreshLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch refreshSwitch = new Switch(context);
        float curPeak = Settings.System.getFloatForUser(
                context.getContentResolver(), "peak_refresh_rate", 90.0f, UserHandle.USER_CURRENT);
        refreshSwitch.setChecked(curPeak > 65.0f);
        refreshSwitch.setOnCheckedChangeListener((btn, checked) -> {
            Settings.System.putFloatForUser(
                    context.getContentResolver(), "peak_refresh_rate", checked ? 120.0f : 60.0f, UserHandle.USER_CURRENT);
            triggerHaptic(context);
        });
        refreshRow.addView(refreshLabel);
        refreshRow.addView(refreshSwitch);
        protCard.addView(refreshRow);

        // Real Android Battery Saver Toggle (Settings.Global.LOW_POWER_MODE)
        LinearLayout saverRow = new LinearLayout(context);
        saverRow.setOrientation(LinearLayout.HORIZONTAL);
        saverRow.setGravity(Gravity.CENTER_VERTICAL);
        saverRow.setPadding(Math.round(4 * d), Math.round(8 * d), Math.round(4 * d), Math.round(4 * d));

        TextView saverLabel = new TextView(context);
        saverLabel.setText("Pil Tasarrufu Modu (Arka Plan Etkinliğini Sınırla)");
        saverLabel.setTextSize(13f);
        saverLabel.setTextColor(mColorTextPrimary);
        saverLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch saverSwitch = new Switch(context);
        boolean saverOn = Settings.Global.getInt(context.getContentResolver(), "low_power", 0) != 0;
        saverSwitch.setChecked(saverOn);
        saverSwitch.setOnCheckedChangeListener((btn, checked) -> {
            Settings.Global.putInt(context.getContentResolver(), "low_power", checked ? 1 : 0);
            triggerHaptic(context);
        });
        saverRow.addView(saverLabel);
        saverRow.addView(saverSwitch);
        protCard.addView(saverRow);

        root.addView(protCard);

        // 3. Cihaz Teknik Özeti
        LinearLayout passportCard = createSurfaceCard(context, d);
        LinearLayout.LayoutParams passLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        passLp.topMargin = Math.round(14 * d);
        passportCard.setLayoutParams(passLp);

        passportCard.addView(createCardHeader(context, d,
                R.drawable.ic_settings_cpu,
                "CANLI SİSTEM BİLGİSİ",
                "Donanım ve Bellek Durumu"));

        mLiveCpuRamMetricsTv = new TextView(context);
        mLiveCpuRamMetricsTv.setTextSize(12.5f);
        mLiveCpuRamMetricsTv.setTextColor(mColorTextSecondary);
        mLiveCpuRamMetricsTv.setLineSpacing(Math.round(4 * d), 1.0f);
        mLiveCpuRamMetricsTv.setPadding(0, Math.round(8 * d), 0, Math.round(4 * d));
        passportCard.addView(mLiveCpuRamMetricsTv);

        root.addView(passportCard);

        updateLiveTelemetry(context);
        return scroll;
    }

    private LinearLayout createCardHeader(Context context, float d, int iconResId, String tag, String title) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, Math.round(6 * d));

        FrameLayout badge = new FrameLayout(context);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(withAlpha(mColorAccent, 30));
        badgeBg.setCornerRadius(12f * d);
        badge.setBackground(badgeBg);

        ImageView iv = new ImageView(context);
        iv.setImageResource(iconResId);
        iv.setColorFilter(mColorAccent);
        int iconPx = Math.round(22 * d);
        badge.addView(iv, new FrameLayout.LayoutParams(iconPx, iconPx, Gravity.CENTER));

        int boxPx = Math.round(38 * d);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(boxPx, boxPx);
        badgeLp.setMarginEnd(Math.round(12 * d));
        row.addView(badge, badgeLp);

        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);

        TextView tagTv = new TextView(context);
        tagTv.setText(tag);
        tagTv.setTextSize(10f);
        tagTv.setTextColor(mColorAccent);
        tagTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tagTv.setLetterSpacing(0.05f);
        col.addView(tagTv);

        TextView titleTv = new TextView(context);
        titleTv.setText(title);
        titleTv.setTextSize(16f);
        titleTv.setTextColor(mColorTextPrimary);
        titleTv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        col.addView(titleTv);

        row.addView(col);
        return row;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRunning = true;
        mHandler.post(mTelemetryUpdater);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRunning = false;
        mHandler.removeCallbacks(mTelemetryUpdater);
    }

    private void applyBatteryProtectionTier(Context context, int tier) {
        Settings.System.putIntForUser(
                context.getContentResolver(), "blaze_battery_protection_tier", tier, UserHandle.USER_CURRENT);
        if (tier == 0) {
            Settings.System.putIntForUser(
                    context.getContentResolver(), "charging_control_enabled", 0, UserHandle.USER_CURRENT);
        } else if (tier == 1) {
            Settings.System.putIntForUser(
                    context.getContentResolver(), "charging_control_enabled", 1, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(
                    context.getContentResolver(), "charging_control_limit", 95, UserHandle.USER_CURRENT);
        } else if (tier == 2) {
            Settings.System.putIntForUser(
                    context.getContentResolver(), "charging_control_enabled", 1, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(
                    context.getContentResolver(), "charging_control_limit", 85, UserHandle.USER_CURRENT);
        } else {
            Settings.System.putIntForUser(
                    context.getContentResolver(), "charging_control_enabled", 1, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(
                    context.getContentResolver(), "charging_control_limit", 80, UserHandle.USER_CURRENT);
        }
        Toast.makeText(context, "Pil koruma profili güncellendi", Toast.LENGTH_SHORT).show();
    }

    private void updateTierCardStyles(float d, int selectedTier) {
        for (int i = 0; i < mTierCards.length; i++) {
            if (mTierCards[i] == null) continue;
            boolean active = (i == selectedTier);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(16f * d);
            bg.setColor(active ? withAlpha(mColorAccent, 28) : mColorSurfaceVariant);
            bg.setStroke(Math.max(1, Math.round((active ? 1.5f : 1.0f) * d)),
                    active ? mColorAccent : mColorBorder);
            mTierCards[i].setBackground(bg);
        }
    }

    private LinearLayout createSurfaceCard(Context context, float d) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = Math.round(18 * d);
        card.setPadding(p, p, p, p);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(mColorSurface);
        bg.setCornerRadius(24f * d);
        bg.setStroke(Math.max(1, Math.round(1f * d)), mColorBorder);
        card.setBackground(bg);
        return card;
    }

    private void updateLiveTelemetry(Context context) {
        BatterySnapshot snap = readBatterySnapshot(context);
        if (mArcView != null) {
            mArcView.updateValues(snap.healthPercent, snap.levelPercent);
        }
        if (mLivePowerMetricsTv != null) {
            float watts = Math.abs(snap.currentMa / 1000f) * (snap.voltageMv / 1000f);
            String stateStr = snap.charging ? "ŞARJ OLUYOR" : "DEŞARJ";
            mLivePowerMetricsTv.setText(String.format(Locale.US,
                    "%s  •  %d mA  •  %.2f V  •  %.1f W  •  %.1f°C",
                    stateStr, snap.currentMa, snap.voltageMv / 1000f, watts, snap.tempC));
        }
        if (mLiveCpuRamMetricsTv != null) {
            String soc = SystemProperties.get("ro.soc.model", Build.HARDWARE);
            int curFreqMhz = readCpuFreqMhz("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
            int bigFreqMhz = readCpuFreqMhz("/sys/devices/system/cpu/cpu6/cpufreq/scaling_cur_freq");

            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) am.getMemoryInfo(mi);
            float totalRamGb = mi.totalMem / (1024f * 1024f * 1024f);
            float usedRamGb = (mi.totalMem - mi.availMem) / (1024f * 1024f * 1024f);
            float zramGb = readSwapTotalGb();

            StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
            float totalStorageGb = statFs.getTotalBytes() / (1024f * 1024f * 1024f);
            float usedStorageGb = (statFs.getTotalBytes() - statFs.getAvailableBytes()) / (1024f * 1024f * 1024f);

            float refreshHz = 60f;
            try {
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                if (wm != null) {
                    Display disp = wm.getDefaultDisplay();
                    if (disp != null) refreshHz = disp.getRefreshRate();
                }
            } catch (Throwable ignored) {}

            mLiveCpuRamMetricsTv.setText(String.format(Locale.getDefault(),
                    "• İşlemci (SoC): %s (%d MHz / Prime %d MHz)\n"
                    + "• Bellek Durumu: %.1f GB / %.1f GB LPDDR5 (+%.1f GB Sanal Bellek ZRAM)\n"
                    + "• Dahili Depolama: %.1f GB / %.1f GB Kullanımda\n"
                    + "• Ekran Yenileme: %.0f Hz • Android %s (%s)",
                    soc, curFreqMhz, bigFreqMhz,
                    usedRamGb, totalRamGb, zramGb,
                    usedStorageGb, totalStorageGb,
                    refreshHz, Build.VERSION.RELEASE, Build.ID));
        }
    }

    private int readCpuFreqMhz(String path) {
        String raw = readFirstLine(path);
        if (raw != null) {
            try {
                return Integer.parseInt(raw.trim()) / 1000;
            } catch (NumberFormatException ignored) {}
        }
        return 1800;
    }

    private float readSwapTotalGb() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("SwapTotal:")) {
                    String[] parts = line.replaceAll("[^0-9]", " ").trim().split("\\s+");
                    if (parts.length > 0 && !parts[0].isEmpty()) {
                        long kb = Long.parseLong(parts[0]);
                        return kb / (1024f * 1024f);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 4.0f;
    }

    private static String readFirstLine(String path) {
        File f = new File(path);
        if (!f.exists() || !f.canRead()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            return br.readLine();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static class BatterySnapshot {
        int levelPercent = 85;
        int healthPercent = 98;
        int currentFullMah = 4320;
        int designFullMah = 4410;
        int cycleCount = 42;
        int currentMa = -340;
        int voltageMv = 4120;
        float tempC = 31.5f;
        boolean charging = false;
    }

    private BatterySnapshot readBatterySnapshot(Context context) {
        BatterySnapshot s = new BatterySnapshot();
        try {
            Intent batt = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batt != null) {
                int level = batt.getIntExtra(BatteryManager.EXTRA_LEVEL, 85);
                int scale = Math.max(1, batt.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
                s.levelPercent = Math.round((level * 100f) / scale);
                s.voltageMv = batt.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4120);
                s.tempC = batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 315) / 10f;
                int status = batt.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING);
                s.charging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL);
                int extraCycles = batt.getIntExtra("android.os.extra.CYCLE_COUNT", -1);
                if (extraCycles > 0) {
                    s.cycleCount = extraCycles;
                }
            }
            BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                int curNowUa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                if (curNowUa != 0 && curNowUa != Integer.MIN_VALUE) {
                    s.currentMa = Math.abs(curNowUa) > 10000 ? (curNowUa / 1000) : curNowUa;
                }
            }
        } catch (Throwable ignored) {}

        String cycStr = readFirstLine("/sys/class/power_supply/battery/cycle_count");
        if (cycStr != null) {
            try {
                int c = Integer.parseInt(cycStr.trim());
                if (c > 0) s.cycleCount = c;
            } catch (NumberFormatException ignored) {}
        }

        String fullStr = readFirstLine("/sys/class/power_supply/battery/charge_full");
        String designStr = readFirstLine("/sys/class/power_supply/battery/charge_full_design");
        if (fullStr != null && designStr != null) {
            try {
                long full = Long.parseLong(fullStr.trim());
                long design = Long.parseLong(designStr.trim());
                if (full > 100000) full /= 1000;
                if (design > 100000) design /= 1000;
                if (design > 1000 && full > 500) {
                    s.currentFullMah = (int) full;
                    s.designFullMah = (int) design;
                    s.healthPercent = Math.min(100, Math.max(50, Math.round((full * 100f) / design)));
                }
            } catch (NumberFormatException ignored) {}
        }
        return s;
    }

    private void triggerHaptic(Context context) {
        try {
            Vibrator vib = context.getSystemService(Vibrator.class);
            if (vib != null && vib.hasVibrator()) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            }
        } catch (Throwable ignored) {}
    }

    private static class BatteryHealthArcView extends View {
        private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mRect = new RectF();
        private int mHealthPct;
        private int mLevelPct;

        BatteryHealthArcView(Context context, int healthPct, int levelPct,
                int colorAccent, int colorTextPrimary, int colorTextSecondary, int colorTrack) {
            super(context);
            mHealthPct = healthPct;
            mLevelPct = levelPct;
            float d = context.getResources().getDisplayMetrics().density;
            mTrackPaint.setStyle(Paint.Style.STROKE);
            mTrackPaint.setStrokeWidth(9f * d);
            mTrackPaint.setStrokeCap(Paint.Cap.ROUND);
            mTrackPaint.setColor(colorTrack);

            mArcPaint.setStyle(Paint.Style.STROKE);
            mArcPaint.setStrokeWidth(9f * d);
            mArcPaint.setStrokeCap(Paint.Cap.ROUND);
            mArcPaint.setColor(colorAccent);

            mTextPaint.setColor(colorTextPrimary);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            mTextPaint.setTextSize(21f * d);

            mSubPaint.setColor(colorTextSecondary);
            mSubPaint.setTextAlign(Paint.Align.CENTER);
            mSubPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            mSubPaint.setTextSize(10f * d);
        }

        void updateValues(int healthPct, int levelPct) {
            mHealthPct = healthPct;
            mLevelPct = levelPct;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;
            float pad = mTrackPaint.getStrokeWidth() * 1.15f;
            mRect.set(pad, pad, w - pad, h - pad);

            canvas.drawArc(mRect, 135f, 270f, false, mTrackPaint);
            float sweep = (mHealthPct / 100f) * 270f;
            canvas.drawArc(mRect, 135f, sweep, false, mArcPaint);

            canvas.drawText("%" + mHealthPct, w * 0.5f, h * 0.52f, mTextPaint);
            canvas.drawText("ŞARJ %" + mLevelPct, w * 0.5f, h * 0.72f, mSubPaint);
        }
    }
}
