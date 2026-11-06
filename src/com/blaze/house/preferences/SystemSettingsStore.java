/*
 * Copyright (C) 2016-2018 crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.preferences;

import android.content.ContentResolver;
import android.preference.PreferenceDataStore;
import android.os.UserHandle;
import android.provider.Settings;

public class SystemSettingsStore extends androidx.preference.PreferenceDataStore
        implements PreferenceDataStore {

    private ContentResolver mContentResolver;

    public SystemSettingsStore(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    public boolean getBoolean(String key, boolean defValue) {
        return Settings.System.getIntForUser(mContentResolver, key, defValue ? 1 : 0, UserHandle.USER_CURRENT) != 0;
    }

    public float getFloat(String key, float defValue) {
        return Settings.System.getFloatForUser(mContentResolver, key, defValue, UserHandle.USER_CURRENT);
    }

    public int getInt(String key, int defValue) {
        if ("status_bar_show_battery_percent".equals(key)) {
            int mode = Settings.System.getIntForUser(
                    mContentResolver, "status_bar_show_battery_percent_mode", -1, UserHandle.USER_CURRENT);
            if (mode >= 0) return mode;
        }
        return Settings.System.getIntForUser(mContentResolver, key, defValue, UserHandle.USER_CURRENT);
    }

    public long getLong(String key, long defValue) {
        return Settings.System.getLongForUser(mContentResolver, key, defValue, UserHandle.USER_CURRENT);
    }

    public String getString(String key, String defValue) {
        if ("status_bar_show_battery_percent".equals(key)) {
            String mode = Settings.System.getStringForUser(
                    mContentResolver, "status_bar_show_battery_percent_mode", UserHandle.USER_CURRENT);
            if (mode != null && !mode.isEmpty()) return mode;
        }
        String result = Settings.System.getStringForUser(mContentResolver, key, UserHandle.USER_CURRENT);
        return result == null ? defValue : result;
    }

    public void putBoolean(String key, boolean value) {
        putInt(key, value ? 1 : 0);
    }

    public void putFloat(String key, float value) {
        try {
            Settings.System.putFloatForUser(mContentResolver, key, value, UserHandle.USER_CURRENT);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void putInt(String key, int value) {
        if ("status_bar_show_battery_percent".equals(key)) {
            Settings.System.putIntForUser(
                    mContentResolver, "status_bar_show_battery_percent_mode", value, UserHandle.USER_CURRENT);
            try {
                Settings.System.putIntForUser(
                        mContentResolver, key, value > 0 ? 1 : 0, UserHandle.USER_CURRENT);
            } catch (IllegalArgumentException ignored) {
            }
            return;
        }
        try {
            Settings.System.putIntForUser(mContentResolver, key, value, UserHandle.USER_CURRENT);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void putLong(String key, long value) {
        try {
            Settings.System.putLongForUser(mContentResolver, key, value, UserHandle.USER_CURRENT);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void putString(String key, String value) {
        if ("status_bar_show_battery_percent".equals(key)) {
            Settings.System.putStringForUser(
                    mContentResolver, "status_bar_show_battery_percent_mode", value, UserHandle.USER_CURRENT);
            String boolVal = ("0".equals(value) || value == null) ? "0" : "1";
            try {
                Settings.System.putStringForUser(mContentResolver, key, boolVal, UserHandle.USER_CURRENT);
            } catch (IllegalArgumentException ignored) {
            }
            return;
        }
        try {
            Settings.System.putStringForUser(mContentResolver, key, value, UserHandle.USER_CURRENT);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
