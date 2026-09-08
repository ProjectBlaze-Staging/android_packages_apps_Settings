/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

public class TopLevelBlazeHousePreferenceController extends BasePreferenceController {

    public TopLevelBlazeHousePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
