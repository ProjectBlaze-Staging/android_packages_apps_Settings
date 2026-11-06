/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.preferences;

import android.content.Context;
import android.os.UserHandle;
import android.util.AttributeSet;

import lineageos.preference.SelfRemovingSwitchPreference;
import lineageos.providers.LineageSettings;

public class LineageSystemSettingSwitchPreference extends SelfRemovingSwitchPreference {

    public LineageSystemSettingSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public LineageSystemSettingSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineageSystemSettingSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean isPersisted() {
        return LineageSettings.System.getString(getContext().getContentResolver(), getKey()) != null
                || android.provider.Settings.System.getString(getContext().getContentResolver(), getKey()) != null;
    }

    @Override
    protected void putBoolean(String key, boolean value) {
        int intVal = value ? 1 : 0;
        try {
            LineageSettings.System.putIntForUser(
                    getContext().getContentResolver(), key, intVal, UserHandle.USER_CURRENT);
        } catch (Exception ignored) {
        }
        try {
            android.provider.Settings.System.putIntForUser(
                    getContext().getContentResolver(), key, intVal, UserHandle.USER_CURRENT);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected boolean getBoolean(String key, boolean defaultValue) {
        int def = defaultValue ? 1 : 0;
        int val = LineageSettings.System.getIntForUser(
                getContext().getContentResolver(), key, -1, UserHandle.USER_CURRENT);
        if (val != -1) {
            return val != 0;
        }
        return android.provider.Settings.System.getIntForUser(
                getContext().getContentResolver(), key, def, UserHandle.USER_CURRENT) != 0;
    }
}
