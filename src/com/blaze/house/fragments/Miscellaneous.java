/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Miscellaneous BlazeHouse settings.
 *
 * All toggle preferences here use custom PreferenceDataStore classes
 * (GlobalSettingSwitchPreference → Settings.Global,
 *  SecureSettingSwitchPreference → Settings.Secure,
 *  SystemSettingSwitchPreference → Settings.System)
 * which already write the value automatically via PreferenceDataStore.
 *
 * This fragment only needs to handle preferences that require additional
 * side-effects beyond a simple settings write.
 */
@SearchIndexable
public class Miscellaneous extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Miscellaneous";

    // Keys matching blazehouse_miscellaneous.xml
    private static final String KEY_HAPTIC_FEEDBACK       = "haptic_feedback_enabled";
    private static final String KEY_VOLBTN_MUSIC          = "volbtn_music_controls";
    private static final String KEY_VOLUME_WAKE           = "volume_wake_screen";
    private static final String KEY_PHOTOS_SPOOF          = "pi_photos_spoof";
    private static final String KEY_GAME_FPS_SPOOF        = "game_props_unlimited_fps";
    private static final String KEY_NETFLIX_SPOOF         = "netflix_spoof";
    private static final String KEY_PI_PP_SPOOF           = "pi_pp_spoof";
    private static final String KEY_WINDOW_IGNORE_SECURE  = "window_ignore_secure";
    private static final String KEY_BLOCK_SS_DETECT       = "window_block_screenshot_detection";
    private static final String KEY_BLOCK_SR_DETECT       = "window_block_screenrecord_detection";
    private static final String KEY_BYPASS_SDK_BLOCK      = "bypass_low_target_sdk_block";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.blazehouse_miscellaneous);
        // All preferences use typed DataStore classes - no manual listener registration needed.
        // They write to their respective Settings.* namespaces automatically.
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // All switches are handled by their PreferenceDataStore.
        // Return true to update the UI state.
        return true;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blazehouse_miscellaneous);
}
