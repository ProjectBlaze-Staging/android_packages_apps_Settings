/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.content.ContentResolver;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * BlazeHouse Universal GSI & Multi-Device Hardware Compatibility Toolkit (TrebleSettings).
 *
 * Provides one-click runtime HAL workarounds for Xiaomi/Redmi/POCO, Samsung (Exynos/Snapdragon),
 * OnePlus/Realme/Oppo, Nothing, MediaTek, and Unisoc devices without requiring external root modules.
 */
@SearchIndexable
public class TrebleSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_HW_INFO = "treble_hardware_info";
    private static final String KEY_BACKLIGHT_SCALE = "blaze_treble_backlight_scale";
    private static final String KEY_FORCE_LIGHTS_HAL = "blaze_treble_force_lights_hal";
    private static final String KEY_REFRESH_RATE_LOCK = "blaze_treble_refresh_rate_lock";
    private static final String KEY_UDFPS_MODE = "blaze_treble_udfps_mode";
    private static final String KEY_BT_A2DP_OFFLOAD = "blaze_treble_bt_a2dp_offload_disable";
    private static final String KEY_AUDIO_SAFE_MODE = "blaze_treble_audio_safe_mode";
    private static final String KEY_FORCE_IMS_VOLTE = "blaze_treble_force_ims_volte";
    private static final String KEY_INCALL_AUDIO_FIX = "blaze_treble_incall_audio_fix";
    private static final String KEY_EXPOSE_AUX_CAMERAS = "blaze_treble_expose_aux_cameras";
    private static final String KEY_SF_GL_COMPAT = "blaze_treble_sf_gl_compat";

    private static final String[] BACKLIGHT_MAX_PATHS = new String[] {
        "/sys/class/backlight/panel0-backlight/max_brightness",
        "/sys/class/backlight/panel/max_brightness",
        "/sys/class/leds/lcd-backlight/max_brightness",
        "/sys/class/backlight/backlight/max_brightness"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.blazehouse_treble);

        updateHardwareDiagnosticSummary();

        bindListener(KEY_BACKLIGHT_SCALE);
        bindListener(KEY_FORCE_LIGHTS_HAL);
        bindListener(KEY_REFRESH_RATE_LOCK);
        bindListener(KEY_UDFPS_MODE);
        bindListener(KEY_BT_A2DP_OFFLOAD);
        bindListener(KEY_AUDIO_SAFE_MODE);
        bindListener(KEY_FORCE_IMS_VOLTE);
        bindListener(KEY_INCALL_AUDIO_FIX);
        bindListener(KEY_EXPOSE_AUX_CAMERAS);
        bindListener(KEY_SF_GL_COMPAT);

        Preference deviceImagePref = findPreference("blaze_about_device_image_picker");
        if (deviceImagePref != null) {
            updateDeviceImageSummary(deviceImagePref);
            deviceImagePref.setOnPreferenceClickListener(pref -> {
                com.blaze.house.utils.BlazeDeviceImageHelper.showDeviceImagePickerDialog(
                        getContext(),
                        () -> updateDeviceImageSummary(deviceImagePref),
                        () -> {
                            try {
                                android.content.Intent intent = new android.content.Intent(
                                        android.content.Intent.ACTION_OPEN_DOCUMENT);
                                intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
                                intent.setType("image/*");
                                startActivityForResult(intent, 9403);
                            } catch (Exception ignored) {}
                        }
                );
                return true;
            });
        }
    }

    private void updateDeviceImageSummary(Preference pref) {
        if (pref != null && getContext() != null) {
            String mode = com.blaze.house.utils.BlazeDeviceImageHelper.getCurrentMode(getContext());
            pref.setSummary(com.blaze.house.utils.BlazeDeviceImageHelper.getModeDisplayName(mode));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9403
                && resultCode == android.app.Activity.RESULT_OK
                && data != null
                && data.getData() != null
                && getContext() != null) {
            if (com.blaze.house.utils.BlazeDeviceImageHelper.saveCustomImageFromUri(
                    getContext(), data.getData())) {
                updateDeviceImageSummary(findPreference("blaze_about_device_image_picker"));
                android.widget.Toast.makeText(getContext(),
                        "Özel cihaz görseli başarıyla kaydedildi!",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void bindListener(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(this);
        }
    }

    private void updateHardwareDiagnosticSummary() {
        Preference hwPref = findPreference(KEY_HW_INFO);
        if (hwPref == null) return;

        String brandModel = Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")";
        String socMan = SystemProperties.get("ro.soc.manufacturer", "");
        String socModel = SystemProperties.get("ro.soc.model", "");
        String boardPlatform = SystemProperties.get("ro.board.platform", "unknown");
        String socLine = (!TextUtils.isEmpty(socMan) || !TextUtils.isEmpty(socModel))
                ? (socMan + " " + socModel + " [" + boardPlatform + "]").trim()
                : boardPlatform;

        String vndkVer = SystemProperties.get("ro.vndk.version", "37");
        String firstApi = SystemProperties.get("ro.board.first_api_level",
                String.valueOf(Build.VERSION.DEVICE_INITIAL_SDK_INT));
        String maxBrightness = detectMaxBacklight();

        String summary = brandModel
                + "\nSoC / Platform: " + socLine
                + "\nVendor VNDK: " + vndkVer + " • Vendor API: " + firstApi
                + "\nBacklight PWM Max: " + maxBrightness;
        hwPref.setSummary(summary);
    }

    private String detectMaxBacklight() {
        for (String path : BACKLIGHT_MAX_PATHS) {
            File f = new File(path);
            if (f.exists() && f.canRead()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line = br.readLine();
                    if (!TextUtils.isEmpty(line)) {
                        String val = line.trim();
                        if ("4095".equals(val)) return "4095 (12-Bit PWM)";
                        if ("2047".equals(val)) return "2047 (11-Bit PWM)";
                        if ("1023".equals(val)) return "1023 (10-Bit PWM)";
                        if ("255".equals(val)) return "255 (8-Bit Standard)";
                        return val;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return "HWC SurfaceControl (Float 0.0–1.0)";
    }

    private void setPropSafe(String key, String value) {
        try {
            SystemProperties.set(key, value);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        final ContentResolver cr = getContentResolver();

        if (KEY_BACKLIGHT_SCALE.equals(key)) {
            // Nudge screen_brightness_float slightly so LocalDisplayAdapter re-evaluates immediately
            try {
                float cur = Settings.System.getFloat(cr, "screen_brightness_float", 0.5f);
                float nudged = (cur > 0.5f) ? (cur - 0.0005f) : (cur + 0.0005f);
                Settings.System.putFloat(cr, "screen_brightness_float", nudged);
                Settings.System.putFloat(cr, "screen_brightness_float", cur);
            } catch (Throwable ignored) {
            }
            return true;
        }

        if (KEY_FORCE_LIGHTS_HAL.equals(key)) {
            boolean enabled = Boolean.TRUE.equals(newValue);
            setPropSafe("persist.sys.blaze.force_lights_hal", enabled ? "true" : "false");
            return true;
        }

        if (KEY_REFRESH_RATE_LOCK.equals(key)) {
            int hz = 0;
            try {
                hz = Integer.parseInt(String.valueOf(newValue));
            } catch (NumberFormatException ignored) {
            }
            if (hz <= 0) {
                Settings.System.putFloat(cr, "min_refresh_rate", 0f);
                Settings.System.putFloat(cr, "peak_refresh_rate", 120f);
            } else {
                Settings.System.putFloat(cr, "min_refresh_rate", (float) hz);
                Settings.System.putFloat(cr, "peak_refresh_rate", (float) hz);
            }
            return true;
        }

        if (KEY_BT_A2DP_OFFLOAD.equals(key)) {
            boolean disabled = Boolean.TRUE.equals(newValue);
            setPropSafe("persist.sys.blaze.bt_a2dp_offload_disable", disabled ? "1" : "0");
            setPropSafe("persist.bluetooth.a2dp_offload.disabled", disabled ? "true" : "false");
            return true;
        }

        if (KEY_AUDIO_SAFE_MODE.equals(key)) {
            boolean safe = Boolean.TRUE.equals(newValue);
            setPropSafe("persist.sys.phh.disable_audio_effects", safe ? "1" : "0");
            return true;
        }

        if (KEY_FORCE_IMS_VOLTE.equals(key)) {
            boolean force = Boolean.TRUE.equals(newValue);
            String v = force ? "1" : "0";
            setPropSafe("persist.dbg.volte_avail_ovr", v);
            setPropSafe("persist.dbg.vt_avail_ovr", v);
            setPropSafe("persist.dbg.wfc_avail_ovr", v);
            return true;
        }

        if (KEY_INCALL_AUDIO_FIX.equals(key)) {
            boolean fix = Boolean.TRUE.equals(newValue);
            setPropSafe("persist.sys.blaze.incall_audio_fix", fix ? "true" : "false");
            return true;
        }

        if (KEY_EXPOSE_AUX_CAMERAS.equals(key)) {
            boolean expose = Boolean.TRUE.equals(newValue);
            setPropSafe("persist.sys.phh.include_all_cameras", expose ? "true" : "false");
            return true;
        }

        if (KEY_SF_GL_COMPAT.equals(key)) {
            boolean compat = Boolean.TRUE.equals(newValue);
            setPropSafe("persist.sys.blaze.sf_gl_compat", compat ? "1" : "0");
            return true;
        }

        return true;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blazehouse_treble);
}
