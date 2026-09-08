/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class GameSpaceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "GameSpaceSettings";
    private static final String KEY_APP_LIST = "gamespace_app_list_pref";

    private Preference mAppListPref;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.blaze_gamespace_settings);

        mAppListPref = findPreference(KEY_APP_LIST);
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
        final String gameList = Settings.System.getStringForUser(
                getContext().getContentResolver(), "gamespace_game_list", UserHandle.USER_CURRENT);
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blaze_gamespace_settings);
}
