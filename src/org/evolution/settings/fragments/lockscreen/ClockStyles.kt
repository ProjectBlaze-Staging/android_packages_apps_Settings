/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.evolution.settings.fragments.lockscreen

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast

import androidx.preference.ListPreference
import androidx.preference.Preference

import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.internal.util.evolution.ThemeUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

import org.evolution.settings.utils.ImageUtils
import org.evolution.settings.utils.PreferenceUtils

class ClockStyles : SettingsPreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    companion object {
        private const val KEY_CLOCK_SOURCE = "lockscreen_clock_source"
        private const val KEY_CUSTOM_CLOCK_STYLE = "lockscreen_custom_clock_style"
        private const val KEY_CUSTOM_AOD_IMAGE = "lockscreen_custom_image"
        private const val CUSTOM_IMAGE_REQUEST_CODE = 1001
    }

    private var mClockSourcePref: ListPreference? = null
    private var mCustomClockStylePref: Preference? = null
    private var mCustomImagePreference: Preference? = null
    private var mThemeUtils: ThemeUtils? = null

    private val firstLaunch: Boolean 
        get() = Settings.System.getInt(requireContext().contentResolver, "custom_clock_disclaimer_dismissed", 0) == 0

    override fun getMetricsCategory(): Int = MetricsEvent.VIEW_UNKNOWN

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.clock_styles, rootKey)
        mThemeUtils = ThemeUtils.getInstance(requireContext())

        mClockSourcePref = findPreference(KEY_CLOCK_SOURCE)
        mCustomClockStylePref = findPreference(KEY_CUSTOM_CLOCK_STYLE)
        mCustomImagePreference = findPreference(KEY_CUSTOM_AOD_IMAGE)

        initClockSourcePreference()
        updateCustomImagePreference()
        if (firstLaunch) showDisclaimer()
    }

    private fun initClockSourcePreference() {
        val resolver = requireContext().contentResolver
        val sourceSet = Settings.Secure.getStringForUser(
            resolver,
            Settings.Secure.LOCKSCREEN_CLOCK_SOURCE,
            UserHandle.USER_CURRENT
        ) != null

        val currentStyle = Settings.Secure.getIntForUser(
            resolver,
            Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE,
            0,
            UserHandle.USER_CURRENT
        )

        val activeSource: Int
        if (!sourceSet) {
            if (currentStyle > 0) {
                // Migration: Existing install with active custom clock
                activeSource = 1
                Settings.Secure.putIntForUser(
                    resolver,
                    Settings.Secure.LOCKSCREEN_CLOCK_SOURCE,
                    1,
                    UserHandle.USER_CURRENT
                )
                Settings.Secure.putIntForUser(
                    resolver,
                    Settings.Secure.LAST_CHOSEN_CUSTOM_CLOCK_STYLE,
                    currentStyle,
                    UserHandle.USER_CURRENT
                )
            } else {
                // Default clean install: System clock
                activeSource = 0
                Settings.Secure.putIntForUser(
                    resolver,
                    Settings.Secure.LOCKSCREEN_CLOCK_SOURCE,
                    0,
                    UserHandle.USER_CURRENT
                )
            }
        } else {
            activeSource = Settings.Secure.getIntForUser(
                resolver,
                Settings.Secure.LOCKSCREEN_CLOCK_SOURCE,
                0,
                UserHandle.USER_CURRENT
            )
        }

        mClockSourcePref?.apply {
            value = activeSource.toString()
            summary = entry
            onPreferenceChangeListener = this@ClockStyles
        }
        updateClockSourceUI(activeSource)
    }

    private fun updateClockSourceUI(source: Int) {
        val isCustom = source == 1
        mCustomClockStylePref?.isEnabled = isCustom
    }

    override fun onResume() {
        super.onResume()
        PreferenceUtils.reloadCustomPrimarySwitches(getPreferenceScreen())
        val resolver = requireContext().contentResolver
        val currentSource = Settings.Secure.getIntForUser(
            resolver,
            Settings.Secure.LOCKSCREEN_CLOCK_SOURCE,
            0,
            UserHandle.USER_CURRENT
        )
        mClockSourcePref?.value = currentSource.toString()
        mClockSourcePref?.summary = mClockSourcePref?.entry
        updateClockSourceUI(currentSource)
        updateCustomImagePreference()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == KEY_CUSTOM_AOD_IMAGE) {
            try {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                startActivityForResult(intent, CUSTOM_IMAGE_REQUEST_CODE)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    R.string.quick_settings_header_needs_gallery,
                    Toast.LENGTH_LONG
                ).show()
            }
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        if (requestCode == CUSTOM_IMAGE_REQUEST_CODE
            && resultCode == Activity.RESULT_OK
            && result != null
        ) {
            val imgUri: Uri? = result.data
            if (imgUri != null) {
                val savedImagePath = ImageUtils.saveImageToInternalStorage(
                    context,
                    imgUri,
                    "lockscreen_aod_image",
                    "LOCKSCREEN_CUSTOM_AOD_IMAGE"
                )
                if (savedImagePath != null) {
                    Settings.System.putStringForUser(
                        requireContext().contentResolver,
                        "custom_aod_image_uri",
                        savedImagePath,
                        UserHandle.USER_CURRENT
                    )
                    mCustomImagePreference?.summary = savedImagePath
                }
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference == mClockSourcePref) {
            val sourceVal = (newValue as String).toIntOrNull() ?: 0
            val resolver = requireContext().contentResolver
            Settings.Secure.putIntForUser(
                resolver,
                Settings.Secure.LOCKSCREEN_CLOCK_SOURCE,
                sourceVal,
                UserHandle.USER_CURRENT
            )

            if (sourceVal == 1) {
                // Switching to Custom Clocks: restore last chosen style (or default to 1)
                val lastStyle = Settings.Secure.getIntForUser(
                    resolver,
                    Settings.Secure.LAST_CHOSEN_CUSTOM_CLOCK_STYLE,
                    1,
                    UserHandle.USER_CURRENT
                ).coerceAtLeast(1)
                Settings.Secure.putIntForUser(
                    resolver,
                    Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE,
                    lastStyle,
                    UserHandle.USER_CURRENT
                )
                updateClockOverlays(lastStyle)
            } else {
                // Switching to System Clock: preserve current custom style in LAST_CHOSEN, then set 0
                val currentStyle = Settings.Secure.getIntForUser(
                    resolver,
                    Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE,
                    0,
                    UserHandle.USER_CURRENT
                )
                if (currentStyle > 0) {
                    Settings.Secure.putIntForUser(
                        resolver,
                        Settings.Secure.LAST_CHOSEN_CUSTOM_CLOCK_STYLE,
                        currentStyle,
                        UserHandle.USER_CURRENT
                    )
                }
                Settings.Secure.putIntForUser(
                    resolver,
                    Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE,
                    0,
                    UserHandle.USER_CURRENT
                )
                updateClockOverlays(0)
            }

            val index = mClockSourcePref?.findIndexOfValue(newValue) ?: -1
            if (index >= 0) {
                mClockSourcePref?.summary = mClockSourcePref?.entries?.get(index)
            }
            updateClockSourceUI(sourceVal)
            return true
        }
        return false
    }

    private fun updateClockOverlays(clockStyle: Int) {
        try {
            mThemeUtils?.setOverlayEnabled(
                "android.theme.customization.smartspace",
                if (clockStyle != 0) "com.android.systemui.hide.smartspace" else "com.android.systemui",
                "com.android.systemui"
            )
        } catch (e: Exception) {
            android.util.Log.e("ClockStyles", "Error updating clock overlays", e)
        }
    }

    private fun showDisclaimer() {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle(ctx.getString(R.string.clock_styles_disclaimer_title))
            .setMessage(ctx.getString(R.string.clock_styles_disclaimer_message))
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                Settings.System.putIntForUser(
                    ctx.contentResolver,
                    "custom_clock_disclaimer_dismissed",
                    1,
                    UserHandle.USER_CURRENT
                    )
            }
            .setCancelable(false)
            .show()
    }

    private fun updateCustomImagePreference() {
        val pref = mCustomImagePreference ?: return
        val ctx = context ?: return

        val imagePath = Settings.System.getString(ctx.contentResolver, "custom_aod_image_uri")
        pref.summary = imagePath ?: ctx.getString(R.string.lockscreen_custom_image_pick_summary)
    }
}
