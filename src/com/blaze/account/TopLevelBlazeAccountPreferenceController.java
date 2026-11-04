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
            String username = BlazeAccountUtils.getUsername(mContext);
            String realName = BlazeAccountUtils.getRealName(mContext);
            String title;
            if (!TextUtils.isEmpty(realName) && !TextUtils.isEmpty(username)
                    && !realName.equalsIgnoreCase(username)) {
                title = realName + " (@" + username + ")";
            } else if (!TextUtils.isEmpty(realName)) {
                title = realName;
            } else if (!TextUtils.isEmpty(username)) {
                title = username;
            } else {
                title = mContext.getString(R.string.blaze_account_title);
            }
            preference.setTitle(title);
            preference.setSummary(mContext.getString(R.string.blaze_account_title)
                    + " • " + BlazeAccountUtils.getMotto(mContext));
            Drawable avatar = BlazeAccountUtils.getCircularAvatar(mContext);
            if (avatar != null) {
                preference.setIcon(avatar);
            } else {
                preference.setIcon(R.drawable.ic_blaze_account_avatar);
            }
        } else {
            preference.setTitle(R.string.blaze_account_title);
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
