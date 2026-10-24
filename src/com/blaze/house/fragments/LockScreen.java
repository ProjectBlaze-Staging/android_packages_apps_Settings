/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class LockScreen extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "LockScreen";
    private static final String KEY_CUSTOM_CLOCK_STYLE = "custom_clock_style";
    private static final String KEY_LOCKSCREEN_BATTERY_INFO = "lockscreen_battery_info";

    private ListPreference mCustomClockStyle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.blazehouse_lock_screen);

        Context context = getContext();
        if (context == null) return;

        mCustomClockStyle = findPreference(KEY_CUSTOM_CLOCK_STYLE);
        if (mCustomClockStyle != null) {
            // Read current value from LOCKSCREEN_USE_DOUBLE_LINE_CLOCK: 1 = double-line (0), 0 = single-line (1)
            int doubleLine = Settings.Secure.getIntForUser(
                    context.getContentResolver(),
                    Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK,
                    1,
                    UserHandle.USER_CURRENT);
            int currentIndex = (doubleLine == 1) ? 0 : 1;
            mCustomClockStyle.setValue(String.valueOf(currentIndex));
            mCustomClockStyle.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomClockStyle) {
            Context context = getContext();
            if (context == null) return false;
            try {
                int style = Integer.parseInt((String) newValue);
                // 0 = Double-line clock (default dynamic), 1 = Single-line clock (compact)
                int doubleLineClock = (style == 0) ? 1 : 0;
                Settings.Secure.putIntForUser(context.getContentResolver(),
                        Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK,
                        doubleLineClock,
                        UserHandle.USER_CURRENT);

                // Reset custom clock face so AOSP standard clock provider handles it cleanly
                Settings.Secure.putStringForUser(context.getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE,
                        "",
                        UserHandle.USER_CURRENT);

                Settings.System.putIntForUser(context.getContentResolver(),
                        "custom_clock_style", style, UserHandle.USER_CURRENT);

                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blazehouse_lock_screen);
}
