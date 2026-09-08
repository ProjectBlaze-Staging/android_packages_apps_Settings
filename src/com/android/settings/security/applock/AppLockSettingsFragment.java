/*
 * Copyright (C) 2026 The BlazeAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.security.applock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

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

/**
 * Fragment that displays all installed applications and allows locking/unlocking them
 * using Android 17 native AppLock service and biometric/PIN credentials.
 */
public class AppLockSettingsFragment extends SettingsPreferenceFragment {

    private static final String TAG = "AppLockSettingsFragment";
    private static final String KEY_CATEGORY_APPS = "app_lock_applications_category";
    private static final int REQUEST_CODE_CONFIRM_CREDENTIAL = 1001;
    private static final int REQUEST_CODE_SET_SCREEN_LOCK = 1002;

    private KeyguardManager mKeyguardManager;
    private PackageManager mPackageManager;
    private PreferenceCategory mCategoryApps;
    private boolean mAuthenticated = false;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private static class AppInfoItem {
        final String packageName;
        final String label;
        final Drawable icon;
        final boolean isLocked;

        AppInfoItem(String packageName, String label, Drawable icon, boolean isLocked) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.isLocked = isLocked;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.app_lock_settings);

        final Context context = getContext();
        if (context == null) {
            return;
        }

        mKeyguardManager = context.getSystemService(KeyguardManager.class);
        mPackageManager = context.getPackageManager();
        mCategoryApps = findPreference(KEY_CATEGORY_APPS);

        if (savedInstanceState != null) {
            mAuthenticated = savedInstanceState.getBoolean("authenticated", false);
        }

        checkSecurityAndAuthenticate();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("authenticated", mAuthenticated);
    }

    private void checkSecurityAndAuthenticate() {
        if (mKeyguardManager == null) {
            return;
        }

        if (!mKeyguardManager.isDeviceSecure()) {
            Toast.makeText(getContext(), R.string.app_lock_set_screen_lock_first, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
            startActivityForResult(intent, REQUEST_CODE_SET_SCREEN_LOCK);
            return;
        }

        if (!mAuthenticated) {
            Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(
                    getString(R.string.app_lock_title),
                    getString(R.string.app_lock_authentication_dialog_reason));
            if (intent != null) {
                startActivityForResult(intent, REQUEST_CODE_CONFIRM_CREDENTIAL);
            } else {
                mAuthenticated = true;
                loadInstalledApplications();
            }
        } else {
            loadInstalledApplications();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CONFIRM_CREDENTIAL) {
            if (resultCode == Activity.RESULT_OK) {
                mAuthenticated = true;
                loadInstalledApplications();
            } else {
                // User failed or canceled authentication
                finish();
            }
        } else if (requestCode == REQUEST_CODE_SET_SCREEN_LOCK) {
            if (mKeyguardManager != null && mKeyguardManager.isDeviceSecure()) {
                mAuthenticated = true;
                loadInstalledApplications();
            } else {
                finish();
            }
        }
    }

    private void loadInstalledApplications() {
        if (mCategoryApps == null) {
            return;
        }
        mCategoryApps.removeAll();

        mExecutor.execute(() -> {
            Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
            launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> activities = mPackageManager.queryIntentActivities(launcherIntent, 0);
            Set<String> processedPackages = new HashSet<>();
            List<AppInfoItem> appItems = new ArrayList<>();

            // Current context package (Settings) and critical system packages to exempt
            final String currentPkg = getContext() != null ? getContext().getPackageName() : "com.android.settings";

            for (ResolveInfo info : activities) {
                if (info.activityInfo == null) {
                    continue;
                }
                String pkgName = info.activityInfo.packageName;
                if (pkgName == null || pkgName.isEmpty() || processedPackages.contains(pkgName)) {
                    continue;
                }
                if ("android".equals(pkgName) || currentPkg.equals(pkgName)) {
                    continue;
                }

                processedPackages.add(pkgName);
                String label = info.loadLabel(mPackageManager).toString();
                Drawable icon = info.loadIcon(mPackageManager);
                boolean isLocked = false;
                try {
                    isLocked = mPackageManager.isPackageAppLockEnabled(pkgName);
                } catch (Exception e) {
                    // Fallback
                }
                appItems.add(new AppInfoItem(pkgName, label, icon, isLocked));
            }

            // Sort alphabetically by label
            Collections.sort(appItems, Comparator.comparing(a -> a.label.toLowerCase()));

            mMainHandler.post(() -> {
                final Context context = getContext();
                if (context == null || mCategoryApps == null) {
                    return;
                }

                for (AppInfoItem item : appItems) {
                    SwitchPreferenceCompat pref = new SwitchPreferenceCompat(context);
                    pref.setKey("app_lock_" + item.packageName);
                    pref.setTitle(item.label);
                    pref.setSummary(item.packageName);
                    pref.setIcon(item.icon);
                    pref.setChecked(item.isLocked);
                    pref.setPersistent(false);

                    pref.setOnPreferenceChangeListener((preference, newValue) -> {
                        boolean enable = (Boolean) newValue;
                        try {
                            boolean success = mPackageManager.setPackageAppLockEnabled(item.packageName, enable);
                            if (!success) {
                                Toast.makeText(context, R.string.app_lock_toggle_failed, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            return true;
                        } catch (Exception e) {
                            Toast.makeText(context, R.string.app_lock_toggle_failed, Toast.LENGTH_SHORT).show();
                            return false;
                        }
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
