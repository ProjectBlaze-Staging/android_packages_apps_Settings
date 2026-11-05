/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Themes fragment.
 *
 * themed_icon_enabled: backed by SecureSettingSwitchPreference which writes
 * Settings.Secure automatically via SecureSettingsStore PreferenceDataStore.
 *
 * wallpaper_and_style_settings: a plain Preference with an intent that opens
 * the system Wallpaper & Style picker — no additional logic needed.
 */
@SearchIndexable
public class Themes extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Themes";
    private static final String KEY_THEMED_ICONS = "themed_icon_enabled";
    private static final String KEY_ABOUT_DEVICE_IMAGE = "blaze_about_device_image_picker";
    private static final String KEY_ABOUT_HERO_BG = "blaze_about_hero_bg_picker";
    private static final int REQUEST_PICK_CUSTOM_IMAGE = 9402;
    private static final int REQUEST_PICK_CUSTOM_HERO_BG = 9404;
    private Preference mAboutDeviceImagePref;
    private Preference mAboutHeroBgPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.blazehouse_themes);

        // SecureSettingSwitchPreference auto-writes to Settings.Secure via
        // SecureSettingsStore. Register listener only for additional side-effects.
        Preference themedIcons = findPreference(KEY_THEMED_ICONS);
        if (themedIcons != null) {
            themedIcons.setOnPreferenceChangeListener(this);
        }

        mAboutDeviceImagePref = findPreference(KEY_ABOUT_DEVICE_IMAGE);
        if (mAboutDeviceImagePref != null) {
            updateAboutDeviceImageSummary();
            mAboutDeviceImagePref.setOnPreferenceClickListener(pref -> {
                com.blaze.house.utils.BlazeDeviceImageHelper.showDeviceImagePickerDialog(
                        getContext(),
                        this::updateAboutDeviceImageSummary,
                        this::launchGalleryPicker
                );
                return true;
            });
        }

        mAboutHeroBgPref = findPreference(KEY_ABOUT_HERO_BG);
        if (mAboutHeroBgPref != null) {
            updateAboutHeroBgSummary();
            mAboutHeroBgPref.setOnPreferenceClickListener(pref -> {
                com.blaze.house.utils.BlazeDeviceImageHelper.showHeroCardBgPickerDialog(
                        getContext(),
                        this::updateAboutHeroBgSummary,
                        this::launchHeroBgGalleryPicker
                );
                return true;
            });
        }
    }

    private void updateAboutDeviceImageSummary() {
        if (mAboutDeviceImagePref != null && getContext() != null) {
            String mode = com.blaze.house.utils.BlazeDeviceImageHelper.getCurrentMode(getContext());
            mAboutDeviceImagePref.setSummary(
                    com.blaze.house.utils.BlazeDeviceImageHelper.getModeDisplayName(mode));
        }
    }

    private void updateAboutHeroBgSummary() {
        if (mAboutHeroBgPref != null && getContext() != null) {
            String mode = com.blaze.house.utils.BlazeDeviceImageHelper.getCurrentHeroBgMode(
                    getContext());
            mAboutHeroBgPref.setSummary(
                    com.blaze.house.utils.BlazeDeviceImageHelper.getHeroBgModeDisplayName(mode));
        }
    }

    private void launchGalleryPicker() {
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_CUSTOM_IMAGE);
        } catch (Exception e) {
            android.widget.Toast.makeText(getContext(),
                    "Galeri seçici açılamadı.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void launchHeroBgGalleryPicker() {
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_CUSTOM_HERO_BG);
        } catch (Exception e) {
            android.widget.Toast.makeText(getContext(),
                    "Galeri seçici açılamadı.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_CUSTOM_IMAGE
                && resultCode == android.app.Activity.RESULT_OK
                && data != null
                && data.getData() != null
                && getContext() != null) {
            if (com.blaze.house.utils.BlazeDeviceImageHelper.saveCustomImageFromUri(
                    getContext(), data.getData())) {
                updateAboutDeviceImageSummary();
                android.widget.Toast.makeText(getContext(),
                        "Özel cihaz görseli başarıyla kaydedildi!",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PICK_CUSTOM_HERO_BG
                && resultCode == android.app.Activity.RESULT_OK
                && data != null
                && data.getData() != null
                && getContext() != null) {
            com.blaze.house.utils.BlazeDeviceImageHelper.showHeroImageCropDialog(
                    getContext(), data.getData(), this::updateAboutHeroBgSummary);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // The SecureSettingSwitchPreference already committed the value.
        // Return true to allow the UI to update.
        return true;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blazehouse_themes);
}
