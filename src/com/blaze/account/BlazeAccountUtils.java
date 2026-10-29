/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class BlazeAccountUtils {
    private static final String TAG = "BlazeAccountUtils";
    private static final String PREF_NAME = "blaze_account_prefs";
    public static final String KEY_ACCOUNT_CREATED = "blaze_account_created";
    public static final String KEY_ACCOUNT_NAME = "blaze_account_name";
    public static final String KEY_ACCOUNT_PHONE = "blaze_account_phone";
    private static final String AVATAR_FILE = "blaze_account_avatar.png";

    public static boolean isAccountConfigured(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ACCOUNT_CREATED, false)) {
            return true;
        }
        try {
            return Settings.System.getInt(context.getContentResolver(), KEY_ACCOUNT_CREATED, 0) == 1;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getAccountName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_ACCOUNT_NAME, "");
        if (TextUtils.isEmpty(name)) {
            try {
                name = Settings.System.getString(context.getContentResolver(), KEY_ACCOUNT_NAME);
            } catch (Exception ignored) {}
        }
        return name != null ? name : "";
    }

    public static String getAccountPhone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String phone = prefs.getString(KEY_ACCOUNT_PHONE, "");
        if (TextUtils.isEmpty(phone)) {
            try {
                phone = Settings.System.getString(context.getContentResolver(), KEY_ACCOUNT_PHONE);
            } catch (Exception ignored) {}
        }
        return phone != null ? phone : "";
    }

    public static File getAvatarFile(Context context) {
        return new File(context.getFilesDir(), AVATAR_FILE);
    }

    public static Drawable getCircularAvatar(Context context) {
        File avatarFile = getAvatarFile(context);
        if (avatarFile.exists()) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
                if (bitmap != null) {
                    RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(
                            context.getResources(), bitmap);
                    drawable.setCircular(true);
                    drawable.setAntiAlias(true);
                    return drawable;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load circular avatar", e);
            }
        }
        return null;
    }

    public static boolean saveAccount(Context context, String name, String phone, Bitmap avatarBitmap) {
        try {
            SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
            editor.putBoolean(KEY_ACCOUNT_CREATED, true);
            editor.putString(KEY_ACCOUNT_NAME, name);
            editor.putString(KEY_ACCOUNT_PHONE, phone);
            editor.apply();

            try {
                Settings.System.putInt(context.getContentResolver(), KEY_ACCOUNT_CREATED, 1);
                Settings.System.putString(context.getContentResolver(), KEY_ACCOUNT_NAME, name);
                Settings.System.putString(context.getContentResolver(), KEY_ACCOUNT_PHONE, phone);
            } catch (Exception ignored) {}

            if (avatarBitmap != null) {
                File avatarFile = getAvatarFile(context);
                try (FileOutputStream fos = new FileOutputStream(avatarFile)) {
                    avatarBitmap.compress(Bitmap.CompressFormat.PNG, 95, fos);
                    fos.flush();
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving Blaze Account", e);
            return false;
        }
    }

    public static boolean saveAvatarFromUri(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return false;
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                Bitmap cropped = cropToSquare(bitmap);
                File avatarFile = getAvatarFile(context);
                try (FileOutputStream fos = new FileOutputStream(avatarFile)) {
                    cropped.compress(Bitmap.CompressFormat.PNG, 95, fos);
                    fos.flush();
                }
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save avatar from URI", e);
        }
        return false;
    }

    public static Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = Math.min(width, height);
        int cropW = (width - height) / 2;
        cropW = Math.max(cropW, 0);
        int cropH = (height - width) / 2;
        cropH = Math.max(cropH, 0);
        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newWidth);
    }

    public static void deleteAccount(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        try {
            Settings.System.putInt(context.getContentResolver(), KEY_ACCOUNT_CREATED, 0);
            Settings.System.putString(context.getContentResolver(), KEY_ACCOUNT_NAME, "");
            Settings.System.putString(context.getContentResolver(), KEY_ACCOUNT_PHONE, "");
        } catch (Exception ignored) {}

        File avatarFile = getAvatarFile(context);
        if (avatarFile.exists()) {
            avatarFile.delete();
        }
    }
}
