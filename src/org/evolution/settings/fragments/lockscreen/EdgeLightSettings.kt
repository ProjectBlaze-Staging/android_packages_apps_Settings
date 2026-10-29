/*
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.settings.fragments.lockscreen

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class EdgeLightSettings : SettingsPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.edge_light_settings)
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.VIEW_UNKNOWN

    companion object {
        @JvmStatic
        fun reset(context: Context) {
            val resolver = context.contentResolver
            Settings.System.putIntForUser(resolver,
                    "edge_light_enabled", 0, UserHandle.USER_CURRENT)
            Settings.System.putStringForUser(resolver,
                    "edge_light_color_mode", "accent", UserHandle.USER_CURRENT)
            Settings.System.putIntForUser(resolver,
                    "edge_light_custom_color", Color.WHITE, UserHandle.USER_CURRENT)
            Settings.System.putIntForUser(resolver,
                    "edge_light_pulse_count", 1, UserHandle.USER_CURRENT)
            Settings.System.putIntForUser(resolver,
                    "edge_light_stroke_width", 8, UserHandle.USER_CURRENT)
            Settings.System.putStringForUser(resolver,
                    "edge_light_style", "default", UserHandle.USER_CURRENT)
            Settings.System.putStringForUser(resolver,
                    "edge_light_animation_effect", "none", UserHandle.USER_CURRENT)
        }
    }
}
