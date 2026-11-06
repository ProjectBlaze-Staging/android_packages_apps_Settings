/*
 * Copyright (C) 2016-2024 crDroid Android Project
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

package org.evolution.settings.fragments.powermenu;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.controls.ControlsProviderService;

import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.applications.ServiceListing;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

import lineageos.app.LineageGlobalActions;
import lineageos.providers.LineageSettings;

import org.evolution.settings.utils.TelephonyUtils;
import org.lineageos.internal.util.PowerMenuConstants;

import static org.lineageos.internal.util.PowerMenuConstants.*;

@SearchIndexable
public class PowerMenuActions extends SettingsPreferenceFragment {
    final static String TAG = "PowerMenuActions";

    private static final String CATEGORY_POWER_MENU_ITEMS = "power_menu_items";
    private static final String GLOBAL_ACTION_KEY_ONTHEGO = "onthego";

    private PreferenceCategory mPowerMenuItemsCategory;

    private SwitchPreferenceCompat mScreenshotPref;
    private SwitchPreferenceCompat mOnTheGoPref;
    private SwitchPreferenceCompat mAirplanePref;
    private SwitchPreferenceCompat mUsersPref;
    private SwitchPreferenceCompat mLockDownPref;
    private SwitchPreferenceCompat mEmergencyPref;
    private SwitchPreferenceCompat mDeviceControlsPref;

    private LineageGlobalActions mLineageGlobalActions;

    Context mContext;
    private LockPatternUtils mLockPatternUtils;
    private UserManager mUserManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.power_menu_actions);
        mContext = getActivity().getApplicationContext();
        mLockPatternUtils = new LockPatternUtils(mContext);
        mUserManager = UserManager.get(mContext);
        mLineageGlobalActions = LineageGlobalActions.getInstance(mContext);

        mPowerMenuItemsCategory = findPreference(CATEGORY_POWER_MENU_ITEMS);

        for (String action : PowerMenuConstants.getAllActions()) {
            if (action.equals(GLOBAL_ACTION_KEY_SCREENSHOT)) {
                mScreenshotPref = (SwitchPreferenceCompat) findPreference(GLOBAL_ACTION_KEY_SCREENSHOT);
            } else if (action.equals(GLOBAL_ACTION_KEY_ONTHEGO)) {
                mOnTheGoPref = (SwitchPreferenceCompat) findPreference(GLOBAL_ACTION_KEY_ONTHEGO);
            } else if (action.equals(GLOBAL_ACTION_KEY_AIRPLANE)) {
                mAirplanePref = (SwitchPreferenceCompat) findPreference(GLOBAL_ACTION_KEY_AIRPLANE);
            } else if (action.equals(GLOBAL_ACTION_KEY_USERS)) {
                mUsersPref = (SwitchPreferenceCompat) findPreference(GLOBAL_ACTION_KEY_USERS);
            } else if (action.equals(GLOBAL_ACTION_KEY_LOCKDOWN)) {
                mLockDownPref = (SwitchPreferenceCompat) findPreference(GLOBAL_ACTION_KEY_LOCKDOWN);
            } else if (action.equals(GLOBAL_ACTION_KEY_EMERGENCY)) {
                mEmergencyPref = (SwitchPreferenceCompat) findPreference(GLOBAL_ACTION_KEY_EMERGENCY);
            } else if (action.equals(GLOBAL_ACTION_KEY_DEVICECONTROLS)) {
                mDeviceControlsPref = findPreference(GLOBAL_ACTION_KEY_DEVICECONTROLS);
            }
        }

        if (!TelephonyUtils.isVoiceCapable(getActivity())) {
            removeActionPreference(mEmergencyPref);
            mEmergencyPref = null;
        }
    }

    private void removeActionPreference(Preference pref) {
        if (pref == null) return;
        if (mPowerMenuItemsCategory != null) {
            mPowerMenuItemsCategory.removePreference(pref);
        } else if (getPreferenceScreen() != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private static final String DEFAULT_POWER_MENU_ACTIONS =
            "power,restart,screenshot,airplane,users,lockdown,emergency";

    private boolean safeUserConfigContains(String action) {
        try {
            if (mLineageGlobalActions != null) {
                return mLineageGlobalActions.userConfigContains(action);
            }
        } catch (Throwable ignored) {
        }
        String raw = Settings.Secure.getString(
                mContext.getContentResolver(), "power_menu_actions");
        if (raw == null) {
            try {
                raw = LineageSettings.Secure.getString(
                        mContext.getContentResolver(),
                        LineageSettings.Secure.POWER_MENU_ACTIONS);
            } catch (Throwable ignored) {
            }
        }
        if (raw == null) {
            raw = DEFAULT_POWER_MENU_ACTIONS;
        }
        for (String token : raw.split(",")) {
            if (action.equals(token.trim())) {
                return true;
            }
        }
        return false;
    }

    private void safeUpdateUserConfig(boolean enabled, String action) {
        try {
            if (mLineageGlobalActions != null) {
                mLineageGlobalActions.updateUserConfig(enabled, action);
                return;
            }
        } catch (Throwable ignored) {
        }
        String raw = Settings.Secure.getString(
                mContext.getContentResolver(), "power_menu_actions");
        if (raw == null) {
            raw = DEFAULT_POWER_MENU_ACTIONS;
        }
        List<String> list = new ArrayList<>();
        for (String token : raw.split(",")) {
            String t = token.trim();
            if (!t.isEmpty() && !t.equals(action)) {
                list.add(t);
            }
        }
        if (enabled) {
            list.add(action);
        }
        String updated = String.join(",", list);
        Settings.Secure.putString(mContext.getContentResolver(), "power_menu_actions", updated);
        try {
            LineageSettings.Secure.putString(
                    mContext.getContentResolver(),
                    LineageSettings.Secure.POWER_MENU_ACTIONS,
                    updated);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mScreenshotPref != null) {
            mScreenshotPref.setChecked(safeUserConfigContains(
                    GLOBAL_ACTION_KEY_SCREENSHOT));
        }

        if (mOnTheGoPref != null) {
            mOnTheGoPref.setChecked(safeUserConfigContains(
                    GLOBAL_ACTION_KEY_ONTHEGO));
        }

        if (mAirplanePref != null) {
            mAirplanePref.setChecked(safeUserConfigContains(
                    GLOBAL_ACTION_KEY_AIRPLANE));
        }

        if (mEmergencyPref != null) {
            mEmergencyPref.setChecked(safeUserConfigContains(
                    GLOBAL_ACTION_KEY_EMERGENCY));
        }

        if (mDeviceControlsPref != null) {
            mDeviceControlsPref.setChecked(safeUserConfigContains(
                    GLOBAL_ACTION_KEY_DEVICECONTROLS));

            // Enable preference if any device control app is installed
            ServiceListing serviceListing = new ServiceListing.Builder(mContext)
                    .setIntentAction(ControlsProviderService.SERVICE_CONTROLS)
                    .setPermission(Manifest.permission.BIND_CONTROLS)
                    .setNoun("Controls Provider")
                    .setSetting("controls_providers")
                    .setTag("controls_providers")
                    .build();
            serviceListing.addCallback(
                    services -> mDeviceControlsPref.setEnabled(!services.isEmpty()));
            serviceListing.reload();
        }

        updatePreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        boolean value;

        if (preference == mScreenshotPref) {
            value = mScreenshotPref.isChecked();
            safeUpdateUserConfig(value, GLOBAL_ACTION_KEY_SCREENSHOT);

        } else if (preference == mOnTheGoPref) {
            value = mOnTheGoPref.isChecked();
            safeUpdateUserConfig(value, GLOBAL_ACTION_KEY_ONTHEGO);

        } else if (preference == mAirplanePref) {
            value = mAirplanePref.isChecked();
            safeUpdateUserConfig(value, GLOBAL_ACTION_KEY_AIRPLANE);

        } else if (preference == mUsersPref) {
            value = mUsersPref.isChecked();
            safeUpdateUserConfig(value, GLOBAL_ACTION_KEY_USERS);

        } else if (preference == mLockDownPref) {
            value = mLockDownPref.isChecked();
            safeUpdateUserConfig(value, GLOBAL_ACTION_KEY_LOCKDOWN);

        } else if (preference == mEmergencyPref) {
            value = mEmergencyPref.isChecked();
            safeUpdateUserConfig(value, GLOBAL_ACTION_KEY_EMERGENCY);

        } else if (preference == mDeviceControlsPref) {
            value = mDeviceControlsPref.isChecked();
            safeUpdateUserConfig(value, GLOBAL_ACTION_KEY_DEVICECONTROLS);

        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private void updatePreferences() {
        boolean isKeyguardSecure = mLockPatternUtils.isSecure(UserHandle.myUserId());
        if (mLockDownPref != null) {
            mLockDownPref.setEnabled(isKeyguardSecure);
            mLockDownPref.setChecked(safeUserConfigContains(
                    GLOBAL_ACTION_KEY_LOCKDOWN));
            if (isKeyguardSecure) {
                mLockDownPref.setSummary(null);
            } else {
                mLockDownPref.setSummary(R.string.power_menu_actions_lockdown_unavailable);
            }
        }
        if (mUsersPref != null) {
            if (!UserHandle.MU_ENABLED || !UserManager.supportsMultipleUsers()) {
                removeActionPreference(mUsersPref);
                mUsersPref = null;
            } else {
                List<UserInfo> users = mUserManager.getUsers();
                boolean enabled = (users.size() > 1);
                mUsersPref.setChecked(safeUserConfigContains(
                        GLOBAL_ACTION_KEY_USERS) && enabled);
                mUsersPref.setEnabled(enabled);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.power_menu_actions);
}
