/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.updates;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.penguin.luminous.Luminous;
import com.android.internal.penguin.luminous.SpringInterpolator;
import com.android.settings.R;
import com.blaze.account.BlazeAccountActivity;
import com.blaze.house.utils.BlazeDeviceImageHelper;
import com.blaze.house.utils.LivingFlowTouchHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ColorOS 17 "Living Flow Design" Software Update experience controller.
 */
public final class BlazeSoftwareUpdateScreen {

    public static final String EXTRA_SOFTWARE_UPDATE_MODE = "blaze_software_update_mode";
    public static final int REQUEST_PICK_OTA_ZIP = 9502;

    private final Activity mActivity;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private LivingFlowBackdropView mBackdrop;
    private LivingFlowVersionEmblemView mEmblem17;
    private View mHeroContainer;
    private ImageView mHeroFlame;
    private TextView mDeviceModelText;
    private TextView mLiveStatusSubtext;
    private View mGlassCard;
    private TextView mChannelBadge;
    private TextView mBuildSignatureText;
    private View mAboutHeaderRow;
    private ImageView mChevronIcon;
    private View mChangelogContainer;
    private TextView mChangelogP4;
    private View mActionPill;
    private TextView mActionPillText;

    private boolean mChangelogExpanded = true;
    private boolean mChecking = false;

    public static void launch(Context context) {
        if (context == null) return;
        try {
            Intent direct = new Intent(context, BlazeSoftwareUpdateActivity.class);
            if (!(context instanceof Activity)) {
                direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            if (context.getPackageManager().resolveActivity(direct, 0) != null) {
                context.startActivity(direct);
                return;
            }
        } catch (Exception ignored) {
        }

        // Seamless live DSU fallback using already-registered BlazeAccountActivity host
        Intent hostIntent = new Intent(context, BlazeAccountActivity.class);
        hostIntent.putExtra(EXTRA_SOFTWARE_UPDATE_MODE, true);
        if (!(context instanceof Activity)) {
            hostIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(hostIntent);
    }

    public static BlazeSoftwareUpdateScreen bind(Activity activity) {
        BlazeSoftwareUpdateScreen screen = new BlazeSoftwareUpdateScreen(activity);
        screen.init();
        return screen;
    }

    private BlazeSoftwareUpdateScreen(Activity activity) {
        mActivity = activity;
    }

    private void init() {
        mActivity.setContentView(R.layout.activity_blaze_software_update);
        configureEdgeToEdgeWindow();

        mBackdrop = mActivity.findViewById(R.id.blaze_update_backdrop);
        mEmblem17 = mActivity.findViewById(R.id.blaze_update_emblem_17);
        mHeroContainer = mActivity.findViewById(R.id.blaze_update_hero_container);
        mHeroFlame = mActivity.findViewById(R.id.blaze_update_hero_flame);
        mDeviceModelText = mActivity.findViewById(R.id.blaze_update_device_model_text);
        mLiveStatusSubtext = mActivity.findViewById(R.id.blaze_update_live_status_subtext);
        mGlassCard = mActivity.findViewById(R.id.blaze_update_glass_card);
        mChannelBadge = mActivity.findViewById(R.id.blaze_update_channel_badge);
        mBuildSignatureText = mActivity.findViewById(R.id.blaze_update_build_signature_text);
        mAboutHeaderRow = mActivity.findViewById(R.id.blaze_update_about_header_row);
        mChevronIcon = mActivity.findViewById(R.id.blaze_update_chevron_icon);
        mChangelogContainer = mActivity.findViewById(R.id.blaze_update_changelog_container);
        mChangelogP4 = mActivity.findViewById(R.id.blaze_update_changelog_p4);
        mActionPill = mActivity.findViewById(R.id.blaze_update_action_pill);
        mActionPillText = mActivity.findViewById(R.id.blaze_update_action_pill_text);

        View backBtn = mActivity.findViewById(R.id.blaze_update_back_btn);
        if (backBtn != null) {
            LivingFlowTouchHelper.attachPill(backBtn);
            backBtn.setOnClickListener(v -> mActivity.finish());
        }

        View menuBtn = mActivity.findViewById(R.id.blaze_update_menu_btn);
        if (menuBtn != null) {
            LivingFlowTouchHelper.attachPill(menuBtn);
            menuBtn.setOnClickListener(this::showOverflowMenu);
        }

        populateDeviceAndBuildMetadata();

        // Attach Living Flow tactile spring physics
        LivingFlowTouchHelper.attach(mHeroContainer, 0.95f, true);
        LivingFlowTouchHelper.attachCard(mGlassCard);
        LivingFlowTouchHelper.attachPill(mAboutHeaderRow);
        LivingFlowTouchHelper.attachPill(mActionPill);

        if (mHeroContainer != null) {
            mHeroContainer.setOnClickListener(v -> runUpdateCheck());
        }

        if (mAboutHeaderRow != null) {
            mAboutHeaderRow.setOnClickListener(v -> toggleChangelog());
        }

        if (mActionPill != null) {
            mActionPill.setOnClickListener(v -> runUpdateCheck());
        }

        // Play ColorOS 17 Laminar staggered entrance cascade
        LivingFlowTouchHelper.playEntranceCascade(
                40L, mHeroContainer, mGlassCard, mActionPill);

        // Automatically check for updates on launch
        mHandler.postDelayed(this::runUpdateCheck, 220L);
    }

    private void configureEdgeToEdgeWindow() {
        Window window = mActivity.getWindow();
        if (window == null) return;
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(0xFF070914);
        View decor = window.peekDecorView();
        WindowInsetsController controller = decor != null ? decor.getWindowInsetsController() : null;
        if (controller != null) {
            controller.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
    }

    private void populateDeviceAndBuildMetadata() {
        String vendorModel = SystemProperties.get("ro.product.vendor.model", "");
        String deviceName = Settings.Global.getString(
                mActivity.getContentResolver(), Settings.Global.DEVICE_NAME);
        String resolvedMode = BlazeDeviceImageHelper.resolveAutoMode();
        String friendlyModel;
        if (!TextUtils.isEmpty(vendorModel) && !vendorModel.contains("GSI")) {
            friendlyModel = vendorModel;
        } else if (BlazeDeviceImageHelper.MODE_PIXEL_6A.equals(resolvedMode)) {
            friendlyModel = "Google Pixel 6a";
        } else if (BlazeDeviceImageHelper.MODE_SAMSUNG_S21.equals(resolvedMode)) {
            friendlyModel = "Samsung Galaxy S21 Ultra";
        } else if (BlazeDeviceImageHelper.MODE_SAMSUNG_S10.equals(resolvedMode)) {
            friendlyModel = "Samsung Galaxy S10+";
        } else if (BlazeDeviceImageHelper.MODE_REDMI_NOTE11.equals(resolvedMode)) {
            friendlyModel = "Xiaomi Redmi Note 11";
        } else if (!TextUtils.isEmpty(deviceName)) {
            friendlyModel = deviceName;
        } else {
            friendlyModel = Build.MODEL;
        }

        if (mDeviceModelText != null) {
            mDeviceModelText.setText(friendlyModel);
        }

        String releaseType = SystemProperties.get("ro.blaze.releasetype",
                SystemProperties.get("ro.lineage.releasetype", "OFFICIAL"));
        if (mChannelBadge != null) {
            mChannelBadge.setText(releaseType + " • Living Flow");
        }

        String buildId = Build.ID != null ? Build.ID : "CP2A.260605.016";
        String securityPatch = Build.VERSION.SECURITY_PATCH != null
                ? Build.VERSION.SECURITY_PATCH : "2026-06-05";
        if (mBuildSignatureText != null) {
            mBuildSignatureText.setText(
                    "Blaze_v5.0_17.0.0.100(" + buildId + ") | 3,84 GB");
        }

        String kernel = System.getProperty("os.version", "Linux 6.1");
        if (mChangelogP4 != null) {
            mChangelogP4.setText(mActivity.getString(
                    R.string.blaze_update_changelog_p4_formatted,
                    securityPatch,
                    kernel,
                    buildId));
        }
    }

    public void runUpdateCheck() {
        if (mChecking) return;
        mChecking = true;

        if (mBackdrop != null) {
            mBackdrop.setScanning(true);
        }
        if (mLiveStatusSubtext != null) {
            mLiveStatusSubtext.setText(R.string.blaze_update_status_checking);
        }
        if (mActionPillText != null) {
            mActionPillText.setText(R.string.blaze_update_btn_checking);
        }
        if (mEmblem17 != null) {
            Luminous.spring(mEmblem17, Luminous.SCALE_X, 1.06f, 240f, 0.55f);
            Luminous.spring(mEmblem17, Luminous.SCALE_Y, 1.06f, 240f, 0.55f);
        }
        if (mHeroFlame != null) {
            mHeroFlame.animate()
                    .rotationBy(360f)
                    .setDuration(1100L)
                    .setInterpolator(new SpringInterpolator(180f, 0.72f, 0f))
                    .start();
        }

        mHandler.postDelayed(() -> {
            if (mActivity.isFinishing() || mActivity.isDestroyed()) return;
            mChecking = false;
            if (mBackdrop != null) {
                mBackdrop.setScanning(false);
            }
            if (mEmblem17 != null) {
                Luminous.spring(mEmblem17, Luminous.SCALE_X, 1.0f, 220f, 0.68f);
                Luminous.spring(mEmblem17, Luminous.SCALE_Y, 1.0f, 220f, 0.68f);
            }
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            if (mLiveStatusSubtext != null) {
                mLiveStatusSubtext.setText(
                        mActivity.getString(R.string.blaze_update_status_up_to_date, timeStr));
            }
            if (mActionPillText != null) {
                mActionPillText.setText(R.string.blaze_update_btn_check_again);
            }
            if (mActionPill != null) {
                mActionPill.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            }
        }, 1650L);
    }

    private void toggleChangelog() {
        mChangelogExpanded = !mChangelogExpanded;
        SpringInterpolator spring = new SpringInterpolator(280f, 0.76f, 0f);
        if (mChevronIcon != null) {
            mChevronIcon.animate()
                    .rotation(mChangelogExpanded ? 0f : 180f)
                    .setDuration(360L)
                    .setInterpolator(spring)
                    .start();
        }
        if (mChangelogContainer != null) {
            if (mChangelogExpanded) {
                mChangelogContainer.setVisibility(View.VISIBLE);
                mChangelogContainer.setAlpha(0f);
                mChangelogContainer.setTranslationY(-18f);
                mChangelogContainer.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(360L)
                        .setInterpolator(spring)
                        .start();
            } else {
                mChangelogContainer.animate()
                        .alpha(0f)
                        .translationY(-14f)
                        .setDuration(220L)
                        .withEndAction(() -> mChangelogContainer.setVisibility(View.GONE))
                        .start();
            }
        }
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu popup = new PopupMenu(mActivity, anchor);
        Menu menu = popup.getMenu();
        menu.add(0, 1, 0, R.string.blaze_update_menu_check_now);
        menu.add(0, 2, 1, R.string.blaze_update_menu_local_ota);
        menu.add(0, 3, 2, R.string.blaze_update_menu_system_updater);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                runUpdateCheck();
                return true;
            } else if (id == 2) {
                launchLocalOtaPicker();
                return true;
            } else if (id == 3) {
                launchExternalSystemUpdater();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void launchLocalOtaPicker() {
        try {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("application/zip");
            mActivity.startActivityForResult(pick, REQUEST_PICK_OTA_ZIP);
        } catch (Exception e) {
            Toast.makeText(mActivity, R.string.blaze_update_local_ota_unavailable,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void launchExternalSystemUpdater() {
        try {
            Intent otaIntent = new Intent("android.settings.SYSTEM_UPDATE_SETTINGS");
            List<ResolveInfo> list = mActivity.getPackageManager()
                    .queryIntentActivities(otaIntent, 0);
            if (list != null) {
                for (ResolveInfo info : list) {
                    if (info.activityInfo != null
                            && !mActivity.getPackageName().equals(info.activityInfo.packageName)) {
                        Intent target = new Intent("android.settings.SYSTEM_UPDATE_SETTINGS");
                        target.setComponent(new ComponentName(
                                info.activityInfo.packageName, info.activityInfo.name));
                        mActivity.startActivity(target);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        Toast.makeText(mActivity, R.string.blaze_update_already_latest_toast,
                Toast.LENGTH_SHORT).show();
    }
}
