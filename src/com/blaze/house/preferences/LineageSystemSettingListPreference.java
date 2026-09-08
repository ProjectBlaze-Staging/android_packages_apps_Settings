/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.preferences;

import android.content.Context;
import androidx.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

public class LineageSystemSettingListPreference extends ListPreference {

    private boolean mAutoSummary = false;

    public LineageSystemSettingListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setPreferenceDataStore(new LineageSystemSettingsStore(context.getContentResolver()));
    }

    public LineageSystemSettingListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPreferenceDataStore(new LineageSystemSettingsStore(context.getContentResolver()));
    }

    public LineageSystemSettingListPreference(Context context) {
        super(context);
        setPreferenceDataStore(new LineageSystemSettingsStore(context.getContentResolver()));
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        if (mAutoSummary || TextUtils.isEmpty(getSummary())) {
            setSummary(getEntry(), true);
        }
    }

    @Override
    public void setSummary(CharSequence summary) {
        setSummary(summary, false);
    }

    private void setSummary(CharSequence summary, boolean autoSummary) {
        mAutoSummary = autoSummary;
        super.setSummary(summary);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedString((String) defaultValue) : (String) defaultValue);
    }

    public int getIntValue(int defValue) {
        return getValue() == null ? defValue : Integer.parseInt(getValue());
    }
}
