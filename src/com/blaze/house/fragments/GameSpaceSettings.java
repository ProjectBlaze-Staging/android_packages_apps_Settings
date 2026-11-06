/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SearchIndexable
public class GameSpaceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "GameSpaceSettings";
    private static final String KEY_APP_LIST = "gamespace_app_list_pref";
    private static final String KEY_SHOW_FPS = "gamespace_show_fps";
    private static final String KEY_PERF_MODE = "gamespace_performance_mode";
    private static final String KEY_TOUCH_POLLING = "gamespace_high_touch_polling";

    private Preference mAppListPref;
    private SwitchPreferenceCompat mShowFpsPref;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.blaze_gamespace_settings);

        mAppListPref = findPreference(KEY_APP_LIST);
        mShowFpsPref = findPreference(KEY_SHOW_FPS);

        // Insert interactive HyperBoost Championship Dashboard Card at order -10
        if (getContext() != null && getPreferenceScreen() != null) {
            HyperBoostDashboardPreference dash = new HyperBoostDashboardPreference(getContext());
            dash.setKey("blaze_hyperboost_dashboard");
            dash.setOrder(-10);
            getPreferenceScreen().addPreference(dash);
        }

        Preference perfPref = findPreference(KEY_PERF_MODE);
        if (perfPref != null) {
            perfPref.setOnPreferenceChangeListener((p, val) -> {
                boolean on = Boolean.TRUE.equals(val);
                try {
                    SystemProperties.set("persist.sys.blaze.perf_mode", on ? "monster" : "balanced");
                } catch (Throwable ignored) {}
                return true;
            });
        }

        Preference touchPref = findPreference(KEY_TOUCH_POLLING);
        if (touchPref != null) {
            touchPref.setOnPreferenceChangeListener((p, val) -> {
                boolean on = Boolean.TRUE.equals(val);
                try {
                    SystemProperties.set("persist.sys.blaze.touch_boost", on ? "1" : "0");
                } catch (Throwable ignored) {}
                return true;
            });
        }

        updateAppListSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAppListSummary();
    }

    private void updateAppListSummary() {
        if (mAppListPref == null || getContext() == null) {
            return;
        }
        String gameList = Settings.System.getStringForUser(
                getContext().getContentResolver(), "gamespace_game_list", UserHandle.USER_CURRENT);
        if (gameList == null) {
            gameList = autoDetectInstalledGames(getContext());
        }
        if (TextUtils.isEmpty(gameList)) {
            mAppListPref.setSummary(R.string.gamespace_app_list_empty_summary);
        } else {
            final String[] games = gameList.split(";");
            int count = 0;
            for (String g : games) {
                if (!TextUtils.isEmpty(g.trim())) {
                    count++;
                }
            }
            if (count == 0) {
                mAppListPref.setSummary(R.string.gamespace_app_list_empty_summary);
            } else {
                mAppListPref.setSummary(getString(R.string.gamespace_app_list_count_summary, count));
            }
        }
    }

    private String autoDetectInstalledGames(Context context) {
        try {
            final PackageManager pm = context.getPackageManager();
            final Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
            launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            final List<ResolveInfo> activities = pm.queryIntentActivities(launcherIntent, 0);
            final Set<String> detected = new HashSet<>();
            for (ResolveInfo info : activities) {
                if (info.activityInfo == null || info.activityInfo.packageName == null) continue;
                final String pkg = info.activityInfo.packageName;
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);
                    if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                        detected.add(pkg);
                    }
                } catch (Exception ignored) {}
            }
            if (!detected.isEmpty()) {
                final String joined = String.join(";", detected);
                Settings.System.putStringForUser(
                        context.getContentResolver(), "gamespace_game_list", joined,
                        UserHandle.USER_CURRENT);
                return joined;
            }
        } catch (Exception ignored) {}
        return "";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    private class HyperBoostDashboardPreference extends Preference {

        HyperBoostDashboardPreference(Context context) {
            super(context);
            setSelectable(false);
        }

        @Override
        public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
            super.onBindViewHolder(holder);
            Context ctx = getContext();
            if (!(holder.itemView instanceof android.view.ViewGroup)) return;
            android.view.ViewGroup root = (android.view.ViewGroup) holder.itemView;
            root.removeAllViews();

            float d = ctx.getResources().getDisplayMetrics().density;
            LinearLayout card = new LinearLayout(ctx);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            int mH = Math.round(16 * d);
            int mV = Math.round(10 * d);
            cardLp.setMargins(mH, mV, mH, mV);
            card.setLayoutParams(cardLp);
            int pad = Math.round(18 * d);
            card.setPadding(pad, pad, pad, pad);

            GradientDrawable cardBg = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF0F172A, 0xFF1E1B4B, 0xFF31102F});
            cardBg.setCornerRadius(24 * d);
            cardBg.setStroke(Math.max(1, Math.round(1.2f * d)), 0x5538BDF8);
            card.setBackground(cardBg);

            // Title row
            TextView badge = new TextView(ctx);
            badge.setText("HYPERBOOST OYUN MOTORU • LAMINAR FLOW 17");
            badge.setTextColor(0xFF38BDF8);
            badge.setTextSize(10.5f);
            badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            card.addView(badge);

            TextView headline = new TextView(ctx);
            headline.setText("Blaze Oyun Alanı Stüdyosu");
            headline.setTextColor(Color.WHITE);
            headline.setTextSize(19f);
            headline.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            headline.setPadding(0, Math.round(4 * d), 0, Math.round(4 * d));
            card.addView(headline);

            // Live Telemetry Strip
            float tempC = 32.0f;
            int battLevel = 85;
            try {
                Intent b = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (b != null) {
                    tempC = b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320) / 10f;
                    battLevel = b.getIntExtra(BatteryManager.EXTRA_LEVEL, 85);
                }
            } catch (Throwable ignored) {}

            TextView telemetry = new TextView(ctx);
            telemetry.setText(String.format(Locale.US,
                    "⚡ 120Hz Kare Hızı  •  🌡️ %.1f°C  •  🔋 %%%d  •  360Hz Dokunma",
                    tempC, battLevel));
            telemetry.setTextColor(0xFFCBD5E1);
            telemetry.setTextSize(12.5f);
            telemetry.setPadding(0, 0, 0, Math.round(14 * d));
            card.addView(telemetry);

            // 3 Mode Pills: Düşük Güç | Dengeli | Canavar Modu (Monster+)
            LinearLayout modeRow = new LinearLayout(ctx);
            modeRow.setOrientation(LinearLayout.HORIZONTAL);
            int currentProfile = Settings.System.getIntForUser(
                    ctx.getContentResolver(), "blaze_game_profile_tier", 2, UserHandle.USER_CURRENT);

            String[] labels = new String[]{"Pil Dostu", "Dengeli", "Canavar+"};
            for (int i = 0; i < 3; i++) {
                final int tier = i;
                TextView pill = new TextView(ctx);
                pill.setText(labels[i]);
                pill.setGravity(Gravity.CENTER);
                pill.setTextSize(12.5f);
                pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                int pV = Math.round(9 * d);
                pill.setPadding(0, pV, 0, pV);

                boolean active = (currentProfile == i);
                pill.setTextColor(active ? 0xFF0F172A : 0xFFE2E8F0);
                GradientDrawable pBg = new GradientDrawable();
                pBg.setCornerRadius(999f);
                pBg.setColor(active ? (i == 2 ? 0xFFFB923C : 0xFF38BDF8) : 0x28FFFFFF);
                pill.setBackground(pBg);

                LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                if (i > 0) pLp.setMarginStart(Math.round(8 * d));
                pill.setLayoutParams(pLp);

                pill.setOnClickListener(v -> {
                    Settings.System.putIntForUser(
                            ctx.getContentResolver(), "blaze_game_profile_tier", tier, UserHandle.USER_CURRENT);
                    Settings.System.putIntForUser(
                            ctx.getContentResolver(), KEY_PERF_MODE, tier >= 1 ? 1 : 0, UserHandle.USER_CURRENT);
                    try {
                        SystemProperties.set("persist.sys.blaze.perf_mode",
                                tier == 2 ? "monster" : (tier == 1 ? "balanced" : "powersave"));
                    } catch (Throwable ignored) {}
                    Toast.makeText(ctx, "Oyun modu: " + labels[tier] + " etkinleştirildi", Toast.LENGTH_SHORT).show();
                    notifyChanged();
                });
                modeRow.addView(pill);
            }
            card.addView(modeRow);

            // Quick Toggle for Floating Live FPS HUD
            boolean fpsOn = Settings.System.getIntForUser(
                    ctx.getContentResolver(), KEY_SHOW_FPS, 0, UserHandle.USER_CURRENT) != 0;
            TextView fpsBtn = new TextView(ctx);
            fpsBtn.setText(fpsOn
                    ? "🎯 Yüzen Canlı FPS & CPU Göstergesi: AÇIK (Kapatmak için dokun)"
                    : "🎯 Yüzen Canlı FPS & CPU Göstergesini Ekranda Göster");
            fpsBtn.setGravity(Gravity.CENTER);
            fpsBtn.setTextSize(12.5f);
            fpsBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            fpsBtn.setTextColor(fpsOn ? 0xFF34D399 : 0xFFE2E8F0);
            GradientDrawable fpsBg = new GradientDrawable();
            fpsBg.setCornerRadius(14 * d);
            fpsBg.setColor(fpsOn ? 0x3310B981 : 0x22FFFFFF);
            fpsBg.setStroke(Math.max(1, Math.round(1 * d)), fpsOn ? 0x8834D399 : 0x33FFFFFF);
            fpsBtn.setBackground(fpsBg);
            int fPad = Math.round(10 * d);
            fpsBtn.setPadding(fPad, fPad, fPad, fPad);
            LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            fLp.topMargin = Math.round(12 * d);
            fpsBtn.setLayoutParams(fLp);

            fpsBtn.setOnClickListener(v -> {
                boolean next = !fpsOn;
                Settings.System.putIntForUser(
                        ctx.getContentResolver(), KEY_SHOW_FPS, next ? 1 : 0, UserHandle.USER_CURRENT);
                if (mShowFpsPref != null) {
                    mShowFpsPref.setChecked(next);
                }
                notifyChanged();
            });
            card.addView(fpsBtn);

            root.addView(card);
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blaze_gamespace_settings);
}
