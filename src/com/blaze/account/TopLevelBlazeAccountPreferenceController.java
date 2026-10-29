/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.account;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class TopLevelBlazeAccountPreferenceController extends BasePreferenceController {

    private Preference mPreference;

    public TopLevelBlazeAccountPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) return;

        if (BlazeAccountUtils.isAccountConfigured(mContext)) {
            String name = BlazeAccountUtils.getAccountName(mContext);
            String phone = BlazeAccountUtils.getAccountPhone(mContext);
            preference.setTitle(!TextUtils.isEmpty(name) ? name : mContext.getString(R.string.blaze_account_title));
            preference.setSummary(!TextUtils.isEmpty(phone) ? phone : mContext.getString(R.string.blaze_account_offline));
            Drawable avatar = BlazeAccountUtils.getCircularAvatar(mContext);
            if (avatar != null) {
                preference.setIcon(avatar);
            } else {
                preference.setIcon(R.drawable.ic_blaze_account_avatar);
            }
        } else {
            preference.setTitle(R.string.blaze_account_create_title);
            preference.setSummary(R.string.blaze_account_create_summary);
            preference.setIcon(R.drawable.ic_blaze_account_avatar);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            Intent intent = new Intent(mContext, BlazeAccountActivity.class);
            mContext.startActivity(intent);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }
}
