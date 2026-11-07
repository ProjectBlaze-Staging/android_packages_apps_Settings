/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.app.GameManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SearchIndexable
public class GameSpaceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_APP_LIST = "gamespace_app_list_pref";
    private static final String KEY_SHOW_FPS = "gamespace_show_fps";
    private static final String KEY_PERF_MODE = "gamespace_performance_mode";

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

        Preference perfPref = findPreference(KEY_PERF_MODE);
        if (perfPref != null) {
            perfPref.setOnPreferenceChangeListener((p, val) -> {
                boolean on = Boolean.TRUE.equals(val);
                applyGameModeToRegisteredGames(getContext(),
                        on ? GameManager.GAME_MODE_PERFORMANCE : GameManager.GAME_MODE_STANDARD);
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

    private void applyGameModeToRegisteredGames(Context context, int gameMode) {
        if (context == null) return;
        try {
            GameManager gm = context.getSystemService(GameManager.class);
            if (gm == null) return;
            String gameList = Settings.System.getStringForUser(
                    context.getContentResolver(), "gamespace_game_list", UserHandle.USER_CURRENT);
            if (TextUtils.isEmpty(gameList)) return;
            for (String pkg : gameList.split(";")) {
                String trimmed = pkg.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        gm.setGameMode(trimmed, gameMode);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
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

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blaze_gamespace_settings);
}
