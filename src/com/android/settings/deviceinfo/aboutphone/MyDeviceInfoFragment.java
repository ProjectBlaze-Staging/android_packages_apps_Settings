/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.deviceinfo.aboutphone;

import static androidx.core.content.ContextCompat.getMainExecutor;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.BluetoothAddressPreferenceController;
import com.android.settings.deviceinfo.BuildNumberPreferenceController;
import com.android.settings.deviceinfo.DeviceNamePreferenceController;
import com.android.settings.deviceinfo.FccEquipmentIdPreferenceController;
import com.android.settings.deviceinfo.FeedbackPreferenceController;
import com.android.settings.deviceinfo.IpAddressPreferenceController;
import com.android.settings.deviceinfo.ManualPreferenceController;
import com.android.settings.deviceinfo.RegulatoryInfoPreferenceController;
import com.android.settings.deviceinfo.SafetyInfoPreferenceController;
import com.android.settings.deviceinfo.UptimePreferenceController;
import com.android.settings.deviceinfo.WifiMacAddressPreferenceController;
import com.android.settings.deviceinfo.imei.ImeiInfoPreferenceController;
import com.android.settings.deviceinfo.simstatus.EidStatus;
import com.android.settings.deviceinfo.simstatus.SimEidPreferenceController;
import com.android.settings.deviceinfo.simstatus.SimStatusPreferenceController;
import com.android.settings.deviceinfo.simstatus.SlotSimStatus;
import com.android.settings.flags.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

// LINT.IfChange
@SearchIndexable
public class MyDeviceInfoFragment extends DashboardFragment
        implements DeviceNamePreferenceController.DeviceNamePreferenceHost {

    private static final String LOG_TAG = "MyDeviceInfoFragment";
    private static final String KEY_EID_INFO = "eid_info";
    private static final String KEY_MY_DEVICE_INFO_HEADER = "my_device_info_header";

    private BuildNumberPreferenceController mBuildNumberPreferenceController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVICEINFO;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(DeviceNamePreferenceController.class).setHost(this /* parent */);
        mBuildNumberPreferenceController = use(BuildNumberPreferenceController.class);
        mBuildNumberPreferenceController.setHost(this /* parent */);
    }

    @Override
    protected @NonNull Set<String> getPreferenceKeysInHierarchy() {
        Set<String> keys = super.getPreferenceKeysInHierarchy();
        // add async preference key manually
        keys.add(KEY_EID_INFO);
        return keys;
    }

    @Override
    protected void onPreferenceScreenCreatedFromResource(
            @NonNull PreferenceScreen preferenceScreen) {
        if (isCatalystEnabled()) {
            // remove the preference created from resource to avoid duplicated key
            preferenceScreen.removePreferenceRecursively(KEY_EID_INFO);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initHeader();
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.my_device_info;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this /* fragment */, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, MyDeviceInfoFragment fragment, Lifecycle lifecycle) {
        // disable catalyst for settings search (i.e. fragment is null)
        boolean isCatalystEnabled = Flags.catalystMyDeviceInfoPrefScreen() && fragment != null;
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        final Executor executor = (fragment == null) ? getMainExecutor(context) :
                Executors.newSingleThreadExecutor();
        androidx.lifecycle.Lifecycle lifecycleObject = (fragment == null) ? null :
                fragment.getLifecycle();
        final SlotSimStatus slotSimStatus = new SlotSimStatus(context, executor, lifecycleObject);

        controllers.add(new IpAddressPreferenceController(context, lifecycle));
        controllers.add(new WifiMacAddressPreferenceController(context, lifecycle));
        controllers.add(new BluetoothAddressPreferenceController(context, lifecycle));
        controllers.add(new RegulatoryInfoPreferenceController(context));
        controllers.add(new SafetyInfoPreferenceController(context));
        controllers.add(new ManualPreferenceController(context));
        controllers.add(new FeedbackPreferenceController(fragment, context));
        controllers.add(new FccEquipmentIdPreferenceController(context));
        controllers.add(new UptimePreferenceController(context, lifecycle));

        Consumer<String> imeiInfoList = imeiKey -> {
            if (Flags.catalystMyDeviceInfoPrefScreen()) {
                return;
            }
            ImeiInfoPreferenceController imeiRecord =
                    new ImeiInfoPreferenceController(context, imeiKey);
            imeiRecord.init(fragment, slotSimStatus);
            controllers.add(imeiRecord);
        };

        if (fragment != null) {
            imeiInfoList.accept(ImeiInfoPreferenceController.DEFAULT_KEY);
        }

        for (int slotIndex = 0; slotIndex < slotSimStatus.size(); slotIndex++) {
            SimStatusPreferenceController slotRecord =
                    new SimStatusPreferenceController(context,
                            slotSimStatus.getPreferenceKey(slotIndex));
            slotRecord.init(fragment, slotSimStatus);
            controllers.add(slotRecord);

            if (fragment != null) {
                imeiInfoList.accept(ImeiInfoPreferenceController.DEFAULT_KEY + (1 + slotIndex));
            }
        }

        if (!isCatalystEnabled) {
            EidStatus eidStatus = new EidStatus(slotSimStatus, context, executor);
            SimEidPreferenceController simEid = new SimEidPreferenceController(context,
                    KEY_EID_INFO);
            simEid.init(slotSimStatus, eidStatus);
            controllers.add(simEid);
        }

        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_CUSTOM_DEVICE_IMAGE
                && resultCode == Activity.RESULT_OK
                && data != null
                && data.getData() != null
                && getContext() != null) {
            boolean saved = com.blaze.house.utils.BlazeDeviceImageHelper.saveCustomImageFromUri(
                    getContext(), data.getData());
            if (saved) {
                initHeader();
                android.widget.Toast.makeText(getContext(),
                        "Özel cihaz görseli başarıyla uygulandı!",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (requestCode == REQUEST_PICK_CUSTOM_HERO_BG
                && resultCode == Activity.RESULT_OK
                && data != null
                && data.getData() != null
                && getContext() != null) {
            com.blaze.house.utils.BlazeDeviceImageHelper.showHeroImageCropDialog(
                    getContext(), data.getData(), this::initHeader);
            return;
        }
        if (mBuildNumberPreferenceController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        initHeader();
    }

    private boolean mEntranceAnimated = false;

    private void initHeader() {
        final LayoutPreference headerPreference =
                getPreferenceScreen().findPreference(KEY_MY_DEVICE_INFO_HEADER);
        if (headerPreference == null) {
            return;
        }

        final View blazeRoot = headerPreference.findViewById(R.id.blaze_about_header_root);
        if (blazeRoot != null) {
            headerPreference.setVisible(true);

            // 1. Top Hero Card (Device Name & Codename & Render)
            final View heroContainer = blazeRoot.findViewById(R.id.blaze_about_hero_container);
            final View heroCard = blazeRoot.findViewById(R.id.blaze_about_hero_card);
            final android.widget.TextView deviceNameView =
                    blazeRoot.findViewById(R.id.blaze_about_device_name_text);
            final android.widget.TextView codenameView =
                    blazeRoot.findViewById(R.id.blaze_about_codename_text);
            final android.widget.ImageView renderView =
                    blazeRoot.findViewById(R.id.blaze_about_device_render);

            String deviceName = android.provider.Settings.Global.getString(
                    getContext().getContentResolver(),
                    android.provider.Settings.Global.DEVICE_NAME);
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = android.os.Build.MODEL;
            }
            if (deviceNameView != null) {
                deviceNameView.setText(deviceName);
            }
            if (codenameView != null) {
                codenameView.setText(android.os.Build.DEVICE);
            }

            if (heroCard != null) {
                final View heroBgClipContainer =
                        blazeRoot.findViewById(R.id.blaze_about_hero_bg_clip_container);
                final View heroLivingFlowBg =
                        blazeRoot.findViewById(R.id.blaze_about_hero_living_flow_bg);
                final android.widget.ImageView heroCustomBgImg =
                        blazeRoot.findViewById(R.id.blaze_about_hero_custom_bg_img);
                final View heroBgScrim =
                        blazeRoot.findViewById(R.id.blaze_about_hero_bg_scrim);
                com.blaze.house.utils.BlazeDeviceImageHelper.bindHeroCardBackground(
                        getContext(),
                        heroCard,
                        heroBgClipContainer,
                        heroLivingFlowBg,
                        heroCustomBgImg,
                        heroBgScrim);
                com.blaze.house.utils.LivingFlowTouchHelper.attachCard(heroCard);
                heroCard.setOnClickListener(v -> {
                    com.android.settings.widget.ValidatedEditTextPreference pref =
                            findPreference("device_name");
                    if (pref != null) {
                        pref.performClick();
                    }
                });
                heroCard.setOnLongClickListener(v -> {
                    com.blaze.house.utils.BlazeDeviceImageHelper.showHeroCardBgPickerDialog(
                            getContext(),
                            this::initHeader,
                            this::launchCustomHeroBgPicker);
                    return true;
                });
            }

            // Disable clipping on parent containers so the 3D device render can overflow above the hero card
            if (blazeRoot instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) blazeRoot).setClipChildren(false);
                ((android.view.ViewGroup) blazeRoot).setClipToPadding(false);
                android.view.ViewParent parent = blazeRoot.getParent();
                while (parent instanceof android.view.ViewGroup) {
                    ((android.view.ViewGroup) parent).setClipChildren(false);
                    ((android.view.ViewGroup) parent).setClipToPadding(false);
                    parent = parent.getParent();
                }
            }

            // Smart Device Auto-Detection + Brand Fallback (GSI) + BlazeHouse Custom Image Picker
            if (renderView != null) {
                com.blaze.house.utils.BlazeDeviceImageHelper.bindDeviceRender(
                        getContext(), renderView);
                com.blaze.house.utils.LivingFlowTouchHelper.attach(renderView, 0.94f, true);
                View.OnClickListener openRenderPicker = v -> {
                    com.blaze.house.utils.BlazeDeviceImageHelper.showDeviceImagePickerDialog(
                            getContext(),
                            () -> com.blaze.house.utils.BlazeDeviceImageHelper.bindDeviceRender(
                                    getContext(), renderView),
                            () -> launchCustomDeviceImagePicker()
                    );
                };
                renderView.setOnClickListener(openRenderPicker);
                renderView.setOnLongClickListener(v -> {
                    openRenderPicker.onClick(v);
                    return true;
                });
            }

            // ProjectBlaze Animated "Updates" Capsule
            final View updatesPill = blazeRoot.findViewById(R.id.blaze_about_updates_pill);
            final View morphOverlay = blazeRoot.findViewById(R.id.blaze_about_updates_morph_overlay);
            final View flameIcon = blazeRoot.findViewById(R.id.blaze_about_updates_flame_icon);
            final View updatesText = blazeRoot.findViewById(R.id.blaze_about_updates_text);
            if (updatesPill != null && morphOverlay != null && flameIcon != null) {
                com.blaze.house.utils.BlazeDeviceImageHelper.startUpdatesPillAnimation(
                        updatesPill, morphOverlay, flameIcon, updatesText);
                com.blaze.house.utils.LivingFlowTouchHelper.attachPill(updatesPill);
                updatesPill.setOnClickListener(v -> openSystemUpdatesOrBlazeDialog());
            }

            // 2. Dual Cards: Official Blaze Logo & Firmware Card
            final View logoCard = blazeRoot.findViewById(R.id.blaze_about_logo_card);
            final View firmwareCard = blazeRoot.findViewById(R.id.blaze_about_firmware_card);
            final android.widget.TextView firmwareVerView =
                    blazeRoot.findViewById(R.id.blaze_about_firmware_version_text);
            final android.widget.TextView firmwareEditionView =
                    blazeRoot.findViewById(R.id.blaze_about_firmware_edition_text);

            if (logoCard != null) {
                com.blaze.house.utils.LivingFlowTouchHelper.attachCard(logoCard);
                logoCard.setOnClickListener(v -> {
                    try {
                        android.content.Intent easterEgg = new android.content.Intent(android.content.Intent.ACTION_MAIN);
                        easterEgg.setClassName("android", "com.android.internal.app.PlatLogoActivity");
                        startActivity(easterEgg);
                    } catch (Exception e) {
                        androidx.preference.Preference firmwarePref = findPreference("firmware_version");
                        if (firmwarePref != null) {
                            firmwarePref.performClick();
                        }
                    }
                });
            }

            String displayVersion = getString(R.string.blaze_about_firmware_version);
            if (firmwareVerView != null) {
                firmwareVerView.setText(displayVersion);
            }

            String releaseType = android.os.SystemProperties.get("ro.blaze.releasetype",
                    android.os.SystemProperties.get("ro.lineage.releasetype", "OFFICIAL"));
            if (firmwareEditionView != null) {
                firmwareEditionView.setText("Android 17 • " + releaseType);
            }

            if (firmwareCard != null) {
                com.blaze.house.utils.LivingFlowTouchHelper.attachCard(firmwareCard);
                firmwareCard.setOnClickListener(v -> openSystemUpdatesOrBlazeDialog());
            }

            // 3. Hardware Specs Card
            final View specsCard = blazeRoot.findViewById(R.id.blaze_about_specs_card);
            if (specsCard != null) {
                com.blaze.house.utils.LivingFlowTouchHelper.attachCard(specsCard);
            }
            final android.widget.TextView chipsetValue =
                    blazeRoot.findViewById(R.id.blaze_about_chipset_value);
            final android.widget.TextView resolutionValue =
                    blazeRoot.findViewById(R.id.blaze_about_resolution_value);
            final android.widget.TextView codenameValue =
                    blazeRoot.findViewById(R.id.blaze_about_codename_value);
            final android.widget.TextView stateValue =
                    blazeRoot.findViewById(R.id.blaze_about_state_value);

            if (chipsetValue != null) {
                String chipset = android.os.SystemProperties.get("ro.soc.model");
                if (chipset == null || chipset.isEmpty()) {
                    chipset = android.os.SystemProperties.get("ro.board.platform");
                }
                if (chipset == null || chipset.isEmpty()) {
                    chipset = android.os.Build.HARDWARE;
                }
                chipsetValue.setText(chipset);
            }

            if (resolutionValue != null) {
                try {
                    android.view.WindowManager wm = (android.view.WindowManager)
                            getContext().getSystemService(Context.WINDOW_SERVICE);
                    android.view.WindowMetrics metrics = wm.getMaximumWindowMetrics();
                    int width = metrics.getBounds().width();
                    int height = metrics.getBounds().height();
                    resolutionValue.setText(width + " x " + height);
                } catch (Exception e) {
                    resolutionValue.setText("1080 x 2400");
                }
            }

            if (codenameValue != null) {
                codenameValue.setText(android.os.Build.DEVICE);
            }

            if (stateValue != null) {
                stateValue.setText(releaseType);
            }

            // 4. Bottom Floating Pill Bar (About & BlazeHouse)
            final View bottomBar = blazeRoot.findViewById(R.id.blaze_about_bottom_bar);
            final View aboutTab = blazeRoot.findViewById(R.id.blaze_about_tab_about);
            final View blazeHouseTab = blazeRoot.findViewById(R.id.blaze_about_tab_blazehouse);
            int glassStyle = android.provider.Settings.Secure.getInt(
                    getContext().getContentResolver(), "blaze_ui_glass_style", 0);
            if (bottomBar != null && aboutTab != null) {
                float d = getContext().getResources().getDisplayMetrics().density;
                if (glassStyle == 3) {
                    // iOS 27 Dark Crystal + Recessed Active Lens Pill
                    android.graphics.drawable.GradientDrawable outerBg =
                            new android.graphics.drawable.GradientDrawable(
                                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                                    new int[] {0xE0181C28, 0xCC11141E, 0xD8151A26});
                    outerBg.setCornerRadius(30f * d);
                    outerBg.setStroke(Math.round(1.4f * d), 0x66FFFFFF);
                    bottomBar.setBackground(outerBg);

                    android.graphics.drawable.GradientDrawable innerLens =
                            new android.graphics.drawable.GradientDrawable(
                                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                    new int[] {0xEE090C14, 0xF205070B});
                    innerLens.setCornerRadius(22f * d);
                    innerLens.setStroke(Math.round(1.15f * d), 0x7088C8FF);
                    aboutTab.setBackground(innerLens);
                } else {
                    bottomBar.setBackgroundResource(R.drawable.blaze_about_bottom_bar_bg);
                    aboutTab.setBackgroundResource(R.drawable.blaze_about_tab_active_bg);
                }
            }
            if (blazeHouseTab != null) {
                com.blaze.house.utils.LivingFlowTouchHelper.attachPill(blazeHouseTab);
                blazeHouseTab.setOnClickListener(v -> {
                    try {
                        android.content.Intent intent = new android.content.Intent();
                        intent.setComponent(new android.content.ComponentName(getContext(),
                                "com.android.settings.Settings$BlazeHouseActivity"));
                        startActivity(intent);
                    } catch (Exception e) {
                        new com.android.settings.core.SubSettingLauncher(getContext())
                                .setDestination("com.blaze.house.BlazeHouse")
                                .setSourceMetricsCategory(getMetricsCategory())
                                .setTitleRes(R.string.blazehouse_title)
                                .launch();
                    }
                });
            }
            if (aboutTab != null) {
                com.blaze.house.utils.LivingFlowTouchHelper.attachPill(aboutTab);
            }
            if (!mEntranceAnimated) {
                mEntranceAnimated = true;
                com.blaze.house.utils.LivingFlowTouchHelper.playEntranceCascade(
                        20L, heroContainer, logoCard, firmwareCard, specsCard, bottomBar);
            }
            return;
        }

        final boolean shouldDisplayHeader = getContext().getResources().getBoolean(
                R.bool.config_show_device_header_in_device_info);
        headerPreference.setVisible(shouldDisplayHeader);
        if (!shouldDisplayHeader) {
            return;
        }
        final View headerView = headerPreference.findViewById(R.id.entity_header);
        final Activity context = getActivity();
        final Bundle bundle = getArguments();
        final EntityHeaderController controller = EntityHeaderController
                .newInstance(context, this, headerView)
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE);

        // TODO: There may be an avatar setting action we can use here.
        final int iconId = bundle != null ? bundle.getInt("icon_id", 0) : 0;
        if (iconId == 0) {
            final UserManager userManager = (UserManager) getActivity().getSystemService(
                    Context.USER_SERVICE);
            final UserInfo info = Utils.getExistingUser(userManager,
                    android.os.Process.myUserHandle());
            controller.setLabel(info.name);
            controller.setIcon(
                    com.android.settingslib.Utils.getUserIcon(getActivity(), userManager, info));
        }

        controller.done(true /* rebindActions */);
    }

    private static final int REQUEST_PICK_CUSTOM_DEVICE_IMAGE = 9401;
    private static final int REQUEST_PICK_CUSTOM_HERO_BG = 9403;

    private void launchCustomDeviceImagePicker() {
        try {
            android.content.Intent intent = new android.content.Intent(
                    android.content.Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_CUSTOM_DEVICE_IMAGE);
        } catch (Exception e) {
            android.widget.Toast.makeText(getContext(),
                    "Galeri seçici açılamadı.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void launchCustomHeroBgPicker() {
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

    private void openSystemUpdatesOrBlazeDialog() {
        Context ctx = getContext();
        if (ctx == null) return;
        com.blaze.house.updates.BlazeSoftwareUpdateScreen.launch(ctx);
    }

    @Override
    public void showDeviceNameWarningDialog(String deviceName) {
        DeviceNameWarningDialog.show(this);
    }

    public void onSetDeviceNameConfirm(boolean confirm) {
        final DeviceNamePreferenceController controller = use(
                DeviceNamePreferenceController.class);
        controller.updateDeviceName(confirm);
        initHeader();
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return MyDeviceInfoScreen.KEY;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.my_device_info) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* fragment */,
                            null /* lifecycle */);
                }
            };
}
// LINT.ThenChange(MyDeviceInfoScreen.kt, MyDeviceInfoApiFirstScreen.kt)
