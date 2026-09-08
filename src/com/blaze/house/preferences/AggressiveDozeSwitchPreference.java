/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.preferences;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;

import lineageos.preference.SelfRemovingSwitchPreference;

public class AggressiveDozeSwitchPreference extends SelfRemovingSwitchPreference {

    private static final String AGGRESSIVE_IDLE_CONSTANTS =
            "light_after_inactive_to=15000,inactive_to=60000,sensing_to=0,locating_to=0,"
            + "motion_inactive_to=0,idle_after_inactive_to=30000,quick_doze_delay_to=15000";

    public AggressiveDozeSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AggressiveDozeSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AggressiveDozeSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean isPersisted() {
        return Settings.System.getString(getContext().getContentResolver(), getKey()) != null;
    }

    @Override
    protected void putBoolean(String key, boolean value) {
        Settings.System.putIntForUser(getContext().getContentResolver(), key,
                value ? 1 : 0, UserHandle.USER_CURRENT);
        try {
            Settings.Global.putString(getContext().getContentResolver(),
                    Settings.Global.DEVICE_IDLE_CONSTANTS,
                    value ? AGGRESSIVE_IDLE_CONSTANTS : null);
        } catch (Throwable ignored) {}
    }

    @Override
    protected boolean getBoolean(String key, boolean defaultValue) {
        return Settings.System.getIntForUser(getContext().getContentResolver(),
                key, defaultValue ? 1 : 0, UserHandle.USER_CURRENT) != 0;
    }
}
