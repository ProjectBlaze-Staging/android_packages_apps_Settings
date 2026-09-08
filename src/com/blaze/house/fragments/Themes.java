/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Themes fragment.
 *
 * themed_icon_enabled: backed by SecureSettingSwitchPreference which writes
 * Settings.Secure automatically via SecureSettingsStore PreferenceDataStore.
 *
 * wallpaper_and_style_settings: a plain Preference with an intent that opens
 * the system Wallpaper & Style picker — no additional logic needed.
 */
@SearchIndexable
public class Themes extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Themes";
    private static final String KEY_THEMED_ICONS = "themed_icon_enabled";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.blazehouse_themes);

        // SecureSettingSwitchPreference auto-writes to Settings.Secure via
        // SecureSettingsStore. Register listener only for additional side-effects.
        Preference themedIcons = findPreference(KEY_THEMED_ICONS);
        if (themedIcons != null) {
            themedIcons.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // The SecureSettingSwitchPreference already committed the value.
        // Return true to allow the UI to update.
        return true;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blazehouse_themes);
}
