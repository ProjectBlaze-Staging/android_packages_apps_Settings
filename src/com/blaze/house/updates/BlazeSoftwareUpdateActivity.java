/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.updates;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settingslib.widget.SettingsThemeHelper;

/**
 * Standalone Activity hosting the ColorOS 17 "Living Flow Design" Software Update experience.
 */
public class BlazeSoftwareUpdateActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            setTheme(R.style.Theme_SubSettings_Expressive);
        }
        super.onCreate(savedInstanceState);
        BlazeSoftwareUpdateScreen.bind(this);
    }
}
