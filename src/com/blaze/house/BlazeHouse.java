/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.blaze.house.fragments.Themes;
import com.blaze.house.utils.BlazeDeviceImageHelper;

import lineageos.providers.LineageSettings;

@SearchIndexable
public class BlazeHouse extends SettingsPreferenceFragment {

    private static final String TAG = "BlazeHouse";
    private static final int MENU_INFO = Menu.FIRST + 1;

    private static final int REQUEST_PICK_CUSTOM_IMAGE = 9402;
    private static final int REQUEST_PICK_CUSTOM_HERO_BG = 9404;

    public static final int TAB_THEMES = 0;
    public static final int TAB_QS = 1;
    public static final int TAB_STATUSBAR = 2;
    public static final int TAB_LOCKSCREEN = 3;
    public static final int TAB_SYSTEM = 4;

    private int mCurrentTab = TAB_SYSTEM;

    private LinearLayout[] mTabViews = new LinearLayout[5];
    private ImageView[] mTabIcons = new ImageView[5];
    private TextView[] mTabTexts = new TextView[5];

    private Preference mAboutDeviceImagePref;
    private Preference mAboutHeroBgPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mCurrentTab = savedInstanceState.getInt("blaze_current_tab", TAB_SYSTEM);
        }
        loadTabPreferences(mCurrentTab);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("blaze_current_tab", mCurrentTab);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupFloatingTabs();
    }

    private void setupFloatingTabs() {
        View headerView = setPinnedHeaderView(R.layout.blaze_floating_tabs);
        if (headerView == null) return;

        View pinnedContainer = getActivity() != null ? getActivity().findViewById(R.id.pinned_header) : null;
        if (pinnedContainer != null) {
            pinnedContainer.setBackgroundColor(Color.TRANSPARENT);
        }

        mTabViews[TAB_THEMES] = headerView.findViewById(R.id.tab_themes);
        mTabViews[TAB_QS] = headerView.findViewById(R.id.tab_qs);
        mTabViews[TAB_STATUSBAR] = headerView.findViewById(R.id.tab_statusbar);
        mTabViews[TAB_LOCKSCREEN] = headerView.findViewById(R.id.tab_lockscreen);
        mTabViews[TAB_SYSTEM] = headerView.findViewById(R.id.tab_system);

        mTabIcons[TAB_THEMES] = headerView.findViewById(R.id.icon_themes);
        mTabIcons[TAB_QS] = headerView.findViewById(R.id.icon_qs);
        mTabIcons[TAB_STATUSBAR] = headerView.findViewById(R.id.icon_statusbar);
        mTabIcons[TAB_LOCKSCREEN] = headerView.findViewById(R.id.icon_lockscreen);
        mTabIcons[TAB_SYSTEM] = headerView.findViewById(R.id.icon_system);

        mTabTexts[TAB_THEMES] = headerView.findViewById(R.id.text_themes);
        mTabTexts[TAB_QS] = headerView.findViewById(R.id.text_qs);
        mTabTexts[TAB_STATUSBAR] = headerView.findViewById(R.id.text_statusbar);
        mTabTexts[TAB_LOCKSCREEN] = headerView.findViewById(R.id.text_lockscreen);
        mTabTexts[TAB_SYSTEM] = headerView.findViewById(R.id.text_system);

        for (int i = 0; i < 5; i++) {
            final int tabIndex = i;
            if (mTabViews[i] != null) {
                mTabViews[i].setOnClickListener(v -> selectTab(tabIndex));
            }
        }

        updateTabUI(mCurrentTab);
    }

    private void selectTab(int tabIndex) {
        if (mCurrentTab == tabIndex) return;
        mCurrentTab = tabIndex;
        updateTabUI(tabIndex);
        loadTabPreferences(tabIndex);
    }

    private void updateTabUI(int selectedTab) {
        final Context context = getContext();
        if (context == null) return;

        final int selectedBg = R.drawable.blaze_tab_item_selected_bg;
        final int unselectedColor = context.getColor(R.color.blaze_tab_unselected_icon_color);
        final int selectedColor = context.getColor(R.color.blaze_tab_selected_text_color);

        for (int i = 0; i < 5; i++) {
            if (mTabViews[i] == null) continue;

            if (i == selectedTab) {
                mTabViews[i].setBackgroundResource(selectedBg);
                if (mTabIcons[i] != null) {
                    mTabIcons[i].setColorFilter(selectedColor);
                }
                if (mTabTexts[i] != null) {
                    mTabTexts[i].setVisibility(View.VISIBLE);
                }
            } else {
                mTabViews[i].setBackground(null);
                if (mTabIcons[i] != null) {
                    mTabIcons[i].setColorFilter(unselectedColor);
                }
                if (mTabTexts[i] != null) {
                    mTabTexts[i].setVisibility(View.GONE);
                }
            }
        }
    }

    private void loadTabPreferences(int tabIndex) {
        PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            screen.removeAll();
        }

        switch (tabIndex) {
            case TAB_THEMES:
                addPreferencesFromResource(R.xml.blazehouse_themes);
                wireThemesTab();
                break;
            case TAB_QS:
                addPreferencesFromResource(R.xml.blazehouse_quick_settings);
                wireQuickSettingsTab();
                break;
            case TAB_STATUSBAR:
                addPreferencesFromResource(R.xml.blazehouse_status_bar);
                wireStatusBarTab();
                break;
            case TAB_LOCKSCREEN:
                addPreferencesFromResource(R.xml.blazehouse_lock_screen);
                wireLockScreenTab();
                break;
            case TAB_SYSTEM:
            default:
                addPreferencesFromResource(R.xml.blazehouse_miscellaneous);
                wireSystemTab();
                break;
        }
    }

    private void wireThemesTab() {
        final Context context = getContext();
        if (context == null) return;

        Preference themedIcons = findPreference("themed_icon_enabled");
        if (themedIcons != null) {
            themedIcons.setOnPreferenceChangeListener((pref, newVal) -> {
                boolean enabled = Boolean.TRUE.equals(newVal);
                Themes.applyThemedIcons(context, enabled);
                triggerClickHaptic(context);
                Toast.makeText(context,
                        enabled ? "Temalı İkonlar (Monokrom) etkinleştirildi" : "Standart uygulama ikonlarına dönüldü",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        Preference glassStyle = findPreference("blaze_ui_glass_style");
        if (glassStyle != null) {
            glassStyle.setOnPreferenceChangeListener((pref, newVal) -> {
                String styleVal = String.valueOf(newVal);
                try {
                    SystemProperties.set("persist.sys.blaze.glass_style", styleVal);
                } catch (Throwable ignored) {}
                Settings.System.putIntForUser(
                        context.getContentResolver(),
                        "blaze_ui_glass_style_version",
                        (int) (System.currentTimeMillis() & 0x7FFFFFFF),
                        UserHandle.USER_CURRENT);
                triggerClickHaptic(context);
                return true;
            });
        }

        Preference animEngine = findPreference("blaze_animation_engine");
        if (animEngine != null) {
            animEngine.setOnPreferenceChangeListener((pref, newVal) -> {
                String val = String.valueOf(newVal);
                try {
                    SystemProperties.set("persist.sys.blaze.anim_engine", val);
                } catch (Throwable ignored) {}
                triggerClickHaptic(context);
                return true;
            });
        }

        mAboutDeviceImagePref = findPreference("blaze_about_device_image_picker");
        if (mAboutDeviceImagePref != null) {
            updateAboutDeviceImageSummary();
            mAboutDeviceImagePref.setOnPreferenceClickListener(pref -> {
                BlazeDeviceImageHelper.showDeviceImagePickerDialog(
                        getContext(),
                        this::updateAboutDeviceImageSummary,
                        this::launchGalleryPicker
                );
                return true;
            });
        }

        mAboutHeroBgPref = findPreference("blaze_about_hero_bg_picker");
        if (mAboutHeroBgPref != null) {
            updateAboutHeroBgSummary();
            mAboutHeroBgPref.setOnPreferenceClickListener(pref -> {
                BlazeDeviceImageHelper.showHeroCardBgPickerDialog(
                        getContext(),
                        this::updateAboutHeroBgSummary,
                        this::launchHeroBgGalleryPicker
                );
                return true;
            });
        }
    }

    private void wireQuickSettingsTab() {
        final Context context = getContext();
        if (context == null) return;

        Preference quickPulldown = findPreference("qs_quick_pulldown");
        if (quickPulldown != null) {
            quickPulldown.setOnPreferenceChangeListener((pref, newVal) -> {
                try {
                    int v = Integer.parseInt(String.valueOf(newVal));
                    Settings.System.putIntForUser(
                            context.getContentResolver(),
                            "status_bar_quick_qs_pulldown",
                            v,
                            UserHandle.USER_CURRENT);
                    LineageSettings.System.putIntForUser(
                            context.getContentResolver(),
                            "qs_quick_pulldown",
                            v,
                            UserHandle.USER_CURRENT);
                } catch (Throwable ignored) {}
                triggerClickHaptic(context);
                return true;
            });
        }
    }

    private void wireStatusBarTab() {
        final Context context = getContext();
        if (context == null) return;

        Preference doubleTapStatusBar = findPreference("double_tap_sleep_gesture");
        if (doubleTapStatusBar != null) {
            doubleTapStatusBar.setOnPreferenceChangeListener((pref, newVal) -> {
                int v = Boolean.TRUE.equals(newVal) ? 1 : 0;
                try {
                    Settings.System.putIntForUser(
                            context.getContentResolver(), "double_tap_sleep_gesture", v, UserHandle.USER_CURRENT);
                    LineageSettings.System.putIntForUser(
                            context.getContentResolver(), "double_tap_sleep_gesture", v, UserHandle.USER_CURRENT);
                } catch (Throwable ignored) {}
                triggerClickHaptic(context);
                return true;
            });
        }
    }

    private void wireLockScreenTab() {
        final Context context = getContext();
        if (context == null) return;

        Preference doubleTapLockscreen = findPreference("double_tap_sleep_lockscreen");
        if (doubleTapLockscreen != null) {
            doubleTapLockscreen.setOnPreferenceChangeListener((pref, newVal) -> {
                int v = Boolean.TRUE.equals(newVal) ? 1 : 0;
                try {
                    Settings.System.putIntForUser(
                            context.getContentResolver(), "double_tap_sleep_lockscreen", v, UserHandle.USER_CURRENT);
                    Settings.Secure.putIntForUser(
                            context.getContentResolver(), Settings.Secure.DOUBLE_TAP_TO_SLEEP, v, UserHandle.USER_CURRENT);
                } catch (Throwable ignored) {}
                triggerClickHaptic(context);
                return true;
            });
        }
    }

    private void wireSystemTab() {
        final Context context = getContext();
        if (context == null) return;

        String[] hapticKeys = new String[]{
                "haptic_feedback_enabled",
                "qs_tile_haptic",
                "qs_brightness_slider_haptic"
        };
        for (String key : hapticKeys) {
            Preference p = findPreference(key);
            if (p != null) {
                p.setOnPreferenceChangeListener((pref, newVal) -> {
                    if (Boolean.TRUE.equals(newVal)) {
                        triggerClickHaptic(context);
                    }
                    return true;
                });
            }
        }

        Preference aggressiveDoze = findPreference("aggressive_doze");
        if (aggressiveDoze != null) {
            aggressiveDoze.setOnPreferenceChangeListener((pref, newVal) -> {
                boolean on = Boolean.TRUE.equals(newVal);
                try {
                    SystemProperties.set("persist.sys.blaze.aggressive_doze", on ? "1" : "0");
                } catch (Throwable ignored) {}
                triggerClickHaptic(context);
                return true;
            });
        }
    }

    private void triggerClickHaptic(Context context) {
        try {
            Vibrator vib = context.getSystemService(Vibrator.class);
            if (vib != null && vib.hasVibrator()) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            }
        } catch (Throwable ignored) {}
    }

    private void updateAboutDeviceImageSummary() {
        if (mAboutDeviceImagePref != null && getContext() != null) {
            String mode = BlazeDeviceImageHelper.getCurrentMode(getContext());
            mAboutDeviceImagePref.setSummary(BlazeDeviceImageHelper.getModeDisplayName(mode));
        }
    }

    private void updateAboutHeroBgSummary() {
        if (mAboutHeroBgPref != null && getContext() != null) {
            String mode = BlazeDeviceImageHelper.getCurrentHeroBgMode(getContext());
            mAboutHeroBgPref.setSummary(BlazeDeviceImageHelper.getHeroBgModeDisplayName(mode));
        }
    }

    private void launchGalleryPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_CUSTOM_IMAGE);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Galeri seçici açılamadı.", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchHeroBgGalleryPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_CUSTOM_HERO_BG);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Galeri seçici açılamadı.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_CUSTOM_IMAGE
                && resultCode == Activity.RESULT_OK
                && data != null
                && data.getData() != null
                && getContext() != null) {
            if (BlazeDeviceImageHelper.saveCustomImageFromUri(getContext(), data.getData())) {
                updateAboutDeviceImageSummary();
                Toast.makeText(getContext(),
                        "Özel cihaz görseli başarıyla kaydedildi!",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PICK_CUSTOM_HERO_BG
                && resultCode == Activity.RESULT_OK
                && data != null
                && data.getData() != null
                && getContext() != null) {
            BlazeDeviceImageHelper.showHeroImageCropDialog(
                    getContext(), data.getData(), this::updateAboutHeroBgSummary);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.add(Menu.NONE, MENU_INFO, Menu.NONE, R.string.blazehouse_info_title);
        item.setIcon(R.drawable.ic_blazehouse_info);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_INFO) {
            showBlazeHouseInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showBlazeHouseInfoDialog() {
        if (getActivity() == null) return;
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.blazehouse_title)
                .setMessage(R.string.blazehouse_info_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(R.drawable.ic_homepage_blaze)
                .show();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.VIEW_UNKNOWN;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blazehouse_miscellaneous);
}
