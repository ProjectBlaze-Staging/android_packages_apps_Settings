/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameSpaceAppListFragment extends SettingsPreferenceFragment {

    private static final String TAG = "GameSpaceAppListFragment";
    private static final String KEY_CATEGORY_APPS = "gamespace_category_apps";

    private PackageManager mPackageManager;
    private PreferenceCategory mCategoryApps;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Set<String> mSelectedGames = new HashSet<>();

    private static class GameAppItem {
        final String packageName;
        final String label;
        final Drawable icon;
        final boolean isSelected;

        GameAppItem(String packageName, String label, Drawable icon, boolean isSelected) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.isSelected = isSelected;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.blaze_gamespace_app_list);

        final Context context = getContext();
        if (context == null) {
            return;
        }

        mPackageManager = context.getPackageManager();
        mCategoryApps = findPreference(KEY_CATEGORY_APPS);

        loadSelectedGames();
        loadApplications();
    }

    private void loadSelectedGames() {
        mSelectedGames.clear();
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final String gameList = Settings.System.getStringForUser(
                context.getContentResolver(), "gamespace_game_list", UserHandle.USER_CURRENT);
        if (!TextUtils.isEmpty(gameList)) {
            for (String pkg : gameList.split(";")) {
                final String trimmed = pkg.trim();
                if (!trimmed.isEmpty()) {
                    mSelectedGames.add(trimmed);
                }
            }
        }
    }

    private synchronized void saveSelectedGames() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String pkg : mSelectedGames) {
            if (!first) {
                sb.append(";");
            }
            sb.append(pkg);
            first = false;
        }
        Settings.System.putStringForUser(context.getContentResolver(),
                "gamespace_game_list", sb.toString(), UserHandle.USER_CURRENT);
    }

    private void loadApplications() {
        if (mCategoryApps == null) {
            return;
        }
        mCategoryApps.removeAll();

        mExecutor.execute(() -> {
            final Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
            launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final List<ResolveInfo> activities = mPackageManager.queryIntentActivities(launcherIntent, 0);
            final Set<String> processedPackages = new HashSet<>();
            final List<GameAppItem> appItems = new ArrayList<>();

            final String currentPkg = getContext() != null ? getContext().getPackageName() : "com.android.settings";

            for (ResolveInfo info : activities) {
                if (info.activityInfo == null) {
                    continue;
                }
                final String pkgName = info.activityInfo.packageName;
                if (pkgName == null || pkgName.isEmpty() || processedPackages.contains(pkgName)) {
                    continue;
                }
                if ("android".equals(pkgName) || currentPkg.equals(pkgName)) {
                    continue;
                }

                processedPackages.add(pkgName);
                final String label = info.loadLabel(mPackageManager).toString();
                final Drawable icon = info.loadIcon(mPackageManager);

                boolean isSelected = mSelectedGames.contains(pkgName);
                if (!isSelected) {
                    try {
                        ApplicationInfo appInfo = mPackageManager.getApplicationInfo(pkgName, 0);
                        if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                            isSelected = true;
                            mSelectedGames.add(pkgName);
                        }
                    } catch (Exception ignored) {}
                }

                appItems.add(new GameAppItem(pkgName, label, icon, isSelected));
            }

            // Save automatically if any CATEGORY_GAME apps were added
            saveSelectedGames();

            Collections.sort(appItems, Comparator.comparing(a -> a.label.toLowerCase()));

            mMainHandler.post(() -> {
                final Context context = getContext();
                if (context == null || mCategoryApps == null) {
                    return;
                }

                for (GameAppItem item : appItems) {
                    SwitchPreferenceCompat pref = new SwitchPreferenceCompat(context);
                    pref.setKey("gamespace_app_" + item.packageName);
                    pref.setTitle(item.label);
                    pref.setSummary(item.packageName);
                    pref.setIcon(item.icon);
                    pref.setChecked(item.isSelected);
                    pref.setPersistent(false);

                    pref.setOnPreferenceChangeListener((preference, newValue) -> {
                        final boolean enable = (Boolean) newValue;
                        if (enable) {
                            mSelectedGames.add(item.packageName);
                        } else {
                            mSelectedGames.remove(item.packageName);
                        }
                        saveSelectedGames();
                        return true;
                    });

                    mCategoryApps.addPreference(pref);
                }
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
    }
}
