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
                heroCard.setOnClickListener(v -> {
                    com.android.settings.widget.ValidatedEditTextPreference pref =
                            findPreference("device_name");
                    if (pref != null) {
                        pref.performClick();
                    }
                });
            }

            // GSI / Custom Device Render Handling:
            // For GSI builds, renderView remains View.GONE.
            // For custom device builds with custom render asset, renderView is shown.
            if (renderView != null) {
                boolean isGsi = android.os.SystemProperties.getBoolean("ro.blaze.is_gsi", false)
                        || android.os.Build.PRODUCT.startsWith("gsi_")
                        || android.os.Build.PRODUCT.contains("_gsi");
                int customRenderRes = getContext().getResources().getIdentifier(
                        "blaze_device_custom", "drawable", getContext().getPackageName());
                if (!isGsi && customRenderRes != 0) {
                    renderView.setImageResource(customRenderRes);
                    renderView.setVisibility(View.VISIBLE);
                } else {
                    renderView.setVisibility(View.GONE);
                }
            }

            // 2. Dual Cards: Official Blaze Logo & Firmware Card
            final View logoCard = blazeRoot.findViewById(R.id.blaze_about_logo_card);
            final View firmwareCard = blazeRoot.findViewById(R.id.blaze_about_firmware_card);
            final android.widget.TextView firmwareVerView =
                    blazeRoot.findViewById(R.id.blaze_about_firmware_version_text);
            final android.widget.TextView firmwareEditionView =
                    blazeRoot.findViewById(R.id.blaze_about_firmware_edition_text);

            if (logoCard != null) {
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
                firmwareCard.setOnClickListener(v -> {
                    androidx.preference.Preference firmwarePref = findPreference("firmware_version");
                    if (firmwarePref != null) {
                        firmwarePref.performClick();
                    }
                });
            }

            // 3. Hardware Specs Card
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
            final View blazeHouseTab = blazeRoot.findViewById(R.id.blaze_about_tab_blazehouse);
            if (blazeHouseTab != null) {
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
