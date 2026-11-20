/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Icon pack selection for BlazeHouse, similar to Good Lock's Theme Park.
 *
 * The choice is stored in {@link Settings.Secure} under {@link #KEY_ICON_PACK} and consumed by the
 * BlazeAOSP launcher:
 * <ul>
 *   <li>unset: BlazeIcons, the stock BlazeAOSP pack (if installed)</li>
 *   <li>{@link #VALUE_NONE}: stock LineageOS icons</li>
 *   <li>a package name: that icon pack</li>
 * </ul>
 * Any app that exposes one of the well known ADW / Nova / Apex theme actions (for example an icon
 * pack installed from the Play Store or from an APK) shows up in the list automatically.
 */
public final class BlazeIconPackHelper {

    private static final String TAG = "BlazeIconPackHelper";

    public static final String KEY_ICON_PACK = "blaze_icon_pack";
    public static final String DEFAULT_PACK = "org.blazeos.blazeicons";
    public static final String VALUE_NONE = "none";

    private static final String[] ICON_PACK_ACTIONS = {
            "org.adw.launcher.THEMES",
            "org.adw.ActivityStarter.THEMES",
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "org.adw.launcher.icons.ACTION_PICK_ICON",
    };

    private BlazeIconPackHelper() {}

    /** One selectable row in the picker. */
    private static final class Entry {
        final String value;
        final CharSequence label;
        final Drawable icon;

        Entry(String value, CharSequence label, Drawable icon) {
            this.value = value;
            this.label = label;
            this.icon = icon;
        }
    }

    /** Returns the stored selection with the default applied. */
    public static String getSelection(Context context) {
        String value = Settings.Secure.getString(context.getContentResolver(), KEY_ICON_PACK);
        return TextUtils.isEmpty(value) ? DEFAULT_PACK : value;
    }

    /** Human readable name of the current selection, for the preference summary. */
    public static CharSequence getSelectionLabel(Context context) {
        String selection = getSelection(context);
        if (VALUE_NONE.equals(selection)) {
            return context.getString(R.string.blaze_icon_pack_stock_label);
        }
        if (DEFAULT_PACK.equals(selection)) {
            return context.getString(R.string.blaze_icon_pack_default_label);
        }
        CharSequence label = loadLabel(context, selection);
        return label != null ? label : context.getString(R.string.blaze_icon_pack_stock_label);
    }

    public static void showPickerDialog(Context context, Runnable onChanged) {
        showPickerDialog(context, onChanged, null);
    }

    public static void showPickerDialog(Context context, Runnable onChanged, Runnable onDismiss) {
        final List<Entry> entries = buildEntries(context);
        final String current = getSelection(context);
        final int pad = dp(context, 16);

        ArrayAdapter<Entry> adapter = new ArrayAdapter<Entry>(context, 0, entries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Entry entry = getItem(position);
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(pad, dp(context, 10), pad, dp(context, 10));

                ImageView icon = new ImageView(context);
                icon.setImageDrawable(entry.icon);
                row.addView(icon, new LinearLayout.LayoutParams(dp(context, 44), dp(context, 44)));

                TextView label = new TextView(context);
                label.setText(entry.label);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                label.setPadding(pad, 0, pad, 0);
                row.addView(label, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                if (entry.value != null && entry.value.equals(current)) {
                    TextView check = new TextView(context);
                    check.setText("\u2713");
                    check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                    row.addView(check);
                }
                return row;
            }
        };

        new AlertDialog.Builder(context)
                .setTitle(R.string.blaze_icon_pack_dialog_title)
                .setAdapter(adapter, (dialog, which) -> {
                    Entry chosen = entries.get(which);
                    if (chosen.value == null) {
                        openStoreForIconPacks(context);
                        if (onDismiss != null) {
                            onDismiss.run();
                        }
                        return;
                    }
                    select(context, chosen.value);
                    Toast.makeText(context, R.string.blaze_icon_pack_applied,
                            Toast.LENGTH_SHORT).show();
                    if (onChanged != null) {
                        onChanged.run();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(dialog -> {
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                })
                .show();
    }

    private static void select(Context context, String value) {
        try {
            Settings.Secure.putString(context.getContentResolver(), KEY_ICON_PACK, value);
        } catch (Exception e) {
            Log.w(TAG, "Unable to store icon pack selection", e);
        }
    }

    private static List<Entry> buildEntries(Context context) {
        PackageManager pm = context.getPackageManager();
        List<Entry> entries = new ArrayList<>();

        Drawable fallbackIcon = pm.getDefaultActivityIcon();

        // 1. The stock BlazeAOSP pack, always first so it is easy to return to.
        Drawable blazeIcon = loadIcon(pm, DEFAULT_PACK);
        entries.add(new Entry(DEFAULT_PACK,
                context.getString(R.string.blaze_icon_pack_default_label),
                blazeIcon != null ? blazeIcon : fallbackIcon));

        // 2. Stock LineageOS icons.
        Drawable launcherIcon = loadIcon(pm, "com.android.launcher3");
        entries.add(new Entry(VALUE_NONE,
                context.getString(R.string.blaze_icon_pack_stock_label),
                launcherIcon != null ? launcherIcon : fallbackIcon));

        // 3. Everything installed that behaves like an icon pack.
        for (Entry pack : queryInstalledPacks(context, pm)) {
            entries.add(pack);
        }

        // 4. Shortcut to get more packs.
        entries.add(new Entry(null,
                context.getString(R.string.blaze_icon_pack_get_more), fallbackIcon));
        return entries;
    }

    private static List<Entry> queryInstalledPacks(Context context, PackageManager pm) {
        Map<String, Entry> found = new LinkedHashMap<>();
        for (String action : ICON_PACK_ACTIONS) {
            Intent intent = new Intent(action);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            List<ResolveInfo> infos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);
            for (ResolveInfo info : infos) {
                if (info.activityInfo == null) {
                    continue;
                }
                String pkg = info.activityInfo.packageName;
                if (DEFAULT_PACK.equals(pkg) || found.containsKey(pkg)) {
                    continue;
                }
                CharSequence label = info.activityInfo.applicationInfo.loadLabel(pm);
                found.put(pkg, new Entry(pkg, label, info.activityInfo.applicationInfo.loadIcon(pm)));
            }
        }
        return new ArrayList<>(found.values());
    }

    private static void openStoreForIconPacks(Context context) {
        Intent market = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://search?q=icon%20pack&c=apps"));
        market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(market);
        } catch (Exception e) {
            try {
                Intent web = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/search?q=icon%20pack&c=apps"));
                web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(web);
            } catch (Exception ignored) {
                Toast.makeText(context, R.string.blaze_icon_pack_no_store,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static CharSequence loadLabel(Context context, String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return info.loadLabel(pm);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static Drawable loadIcon(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }
}
