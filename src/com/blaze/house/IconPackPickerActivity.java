/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house;

import android.app.Activity;
import android.os.Bundle;

import com.blaze.house.utils.BlazeIconPackHelper;

/**
 * Trampoline activity for opening the BlazeHouse Icon Pack picker dialog from
 * external intents (e.g. Launcher3 desktop shortcut, ThemePicker, or com.blaze.house.action.ICON_PACK_PICKER).
 */
public class IconPackPickerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlazeIconPackHelper.showPickerDialog(this, this::finish, this::finish);
    }
}
