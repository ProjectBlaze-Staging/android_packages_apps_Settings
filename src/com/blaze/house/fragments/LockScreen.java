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
    private static final String KEY_DEPTH_WALLPAPER = "depth_wallpaper";
    private static final String KEY_LOCKSCREEN_BATTERY_INFO = "lockscreen_battery_info";

    // Pixel clock face JSON values matching LOCK_SCREEN_CUSTOM_CLOCK_FACE
    private static final String[] CLOCK_FACE_IDS = {
        "{\"clockId\":\"DEFAULT\"}",                               // 0 - Default (Double-line)
        "{\"clockId\":\"DIGITAL_CLOCK_METRO\"}",                   // 1 - Metro
        "{\"clockId\":\"DIGITAL_CLOCK_CALLIGRAPHY\"}",             // 2 - Calligraphy
        "{\"clockId\":\"DIGITAL_CLOCK_CLOCK_BUBBLE\"}",            // 3 - Bubble
        "{\"clockId\":\"ANALOG_CLOCK_GOING_DIGITAL\"}",            // 4 - Going Digital (Analog)
        "{\"clockId\":\"DIGITAL_CLOCK_FLEX\"}",                    // 5 - Flex
        "{\"clockId\":\"DIGITAL_CLOCK_NUMERIC_WEATHER\"}",         // 6 - Weather
        "{\"clockId\":\"DIGITAL_CLOCK_BOLD\"}",                    // 7 - Bold
        "{\"clockId\":\"DIGITAL_CLOCK_HANDWRITTEN\"}",             // 8 - Handwritten
        "{\"clockId\":\"DIGITAL_CLOCK_MINIMAL\"}",                 // 9 - Minimal
        "{\"clockId\":\"ANALOG_CLOCK_STOPWATCH\"}",                // 10 - Stopwatch (Analog)
        "{\"clockId\":\"ANALOG_CLOCK_VERSUS\"}",                   // 11 - Versus (Analog)
        "{\"clockId\":\"DIGITAL_CLOCK_TYPOGRAPHIC\"}"              // 12 - Typographic
    };

    private ListPreference mCustomClockStyle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.blazehouse_lock_screen);

        Context context = getContext();
        if (context == null) return;

        mCustomClockStyle = findPreference(KEY_CUSTOM_CLOCK_STYLE);
        if (mCustomClockStyle != null) {
            // Read current value from LOCK_SCREEN_CUSTOM_CLOCK_FACE and sync UI
            String currentFace = Settings.Secure.getStringForUser(
                    context.getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE,
                    UserHandle.USER_CURRENT);
            int currentIndex = 0;
            if (currentFace != null) {
                for (int i = 0; i < CLOCK_FACE_IDS.length; i++) {
                    if (CLOCK_FACE_IDS[i].equals(currentFace)) {
                        currentIndex = i;
                        break;
                    }
                }
            }
            mCustomClockStyle.setValue(String.valueOf(currentIndex));
            mCustomClockStyle.setOnPreferenceChangeListener(this);
        }

        // depth_wallpaper and lockscreen_battery_info use SystemSettingSwitchPreference
        // which auto-writes to Settings.System via PreferenceDataStore - no extra code needed
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
                if (style < 0 || style >= CLOCK_FACE_IDS.length) return false;

                String clockFaceJson = CLOCK_FACE_IDS[style];

                // Enable/disable double-line clock (DEFAULT uses it, others don't)
                int doubleLineClock = (style == 0) ? 1 : 0;
                Settings.Secure.putIntForUser(context.getContentResolver(),
                        Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK,
                        doubleLineClock,
                        UserHandle.USER_CURRENT);

                // Write the clock face JSON
                Settings.Secure.putStringForUser(context.getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE,
                        clockFaceJson,
                        UserHandle.USER_CURRENT);

                // Also write custom_clock_style to Settings.System so the preference
                // shows correct value on next open
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
