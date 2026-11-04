/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.android.settings.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

public class BlazeAccountUtils {
    private static final String TAG = "BlazeAccountUtils";
    private static final String PREF_NAME = "blaze_account_prefs";

    public static final String KEY_ACCOUNT_CREATED = "blaze_account_created";
    public static final String KEY_ACCOUNT_USERNAME = "blaze_account_username";
    public static final String KEY_ACCOUNT_REAL_NAME = "blaze_account_name";
    public static final String KEY_ACCOUNT_MOTTO = "blaze_account_motto";
    public static final String KEY_ACCOUNT_PHONE = "blaze_account_phone";
    public static final String DEFAULT_MOTTO = "Another day to live!";
    private static final String AVATAR_FILE = "blaze_account_avatar.png";

    public static boolean isAccountConfigured(Context context) {
        if (context == null) return false;
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

    public static String getUsername(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String username = prefs.getString(KEY_ACCOUNT_USERNAME, "");
        if (TextUtils.isEmpty(username)) {
            try {
                username = Settings.System.getString(
                        context.getContentResolver(), KEY_ACCOUNT_USERNAME);
            } catch (Exception ignored) {}
        }
        if (TextUtils.isEmpty(username)) {
            // Fallback to real name if only real name was set previously
            username = getRealName(context);
        }
        return username != null ? username.trim() : "";
    }

    public static String getRealName(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_ACCOUNT_REAL_NAME, "");
        if (TextUtils.isEmpty(name)) {
            try {
                name = Settings.System.getString(
                        context.getContentResolver(), KEY_ACCOUNT_REAL_NAME);
            } catch (Exception ignored) {}
        }
        return name != null ? name.trim() : "";
    }

    public static String getAccountName(Context context) {
        String real = getRealName(context);
        return !TextUtils.isEmpty(real) ? real : getUsername(context);
    }

    public static String getMotto(Context context) {
        if (context == null) return DEFAULT_MOTTO;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String motto = prefs.getString(KEY_ACCOUNT_MOTTO, "");
        if (TextUtils.isEmpty(motto)) {
            try {
                motto = Settings.System.getString(
                        context.getContentResolver(), KEY_ACCOUNT_MOTTO);
            } catch (Exception ignored) {}
        }
        return !TextUtils.isEmpty(motto) ? motto.trim() : DEFAULT_MOTTO;
    }

    public static String getAccountPhone(Context context) {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String phone = prefs.getString(KEY_ACCOUNT_PHONE, "");
        if (TextUtils.isEmpty(phone)) {
            try {
                phone = Settings.System.getString(
                        context.getContentResolver(), KEY_ACCOUNT_PHONE);
            } catch (Exception ignored) {}
        }
        return phone != null ? phone.trim() : "";
    }

    /**
     * Builds the dynamic time-of-day greeting line for the Settings Homepage header, e.g.:
     * "Good morning Tejas," / "Günaydın Tejas," or "Good morning Owner," / "Günaydın Sahip,"
     */
    public static String getDynamicGreetingTitle(Context context) {
        String displayUser;
        if (isAccountConfigured(context) && !TextUtils.isEmpty(getUsername(context))) {
            displayUser = getUsername(context);
        } else {
            displayUser = context.getString(R.string.blaze_account_default_owner);
        }

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greetingPrefix;
        if (hour >= 5 && hour < 12) {
            greetingPrefix = context.getString(R.string.blaze_greeting_morning);
        } else if (hour >= 12 && hour < 18) {
            greetingPrefix = context.getString(R.string.blaze_greeting_afternoon);
        } else if (hour >= 18 && hour < 23) {
            greetingPrefix = context.getString(R.string.blaze_greeting_evening);
        } else {
            greetingPrefix = context.getString(R.string.blaze_greeting_night);
        }
        return greetingPrefix + " " + displayUser + ",";
    }

    public static File getAvatarFile(Context context) {
        return new File(context.getFilesDir(), AVATAR_FILE);
    }

    public static Bitmap getAvatarBitmap(Context context) {
        if (context == null) return null;
        File avatarFile = getAvatarFile(context);
        if (avatarFile.exists() && avatarFile.length() > 0) {
            try {
                return BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode avatar bitmap", e);
            }
        }
        return null;
    }

    public static Drawable getCircularAvatar(Context context) {
        Bitmap bitmap = getAvatarBitmap(context);
        if (bitmap != null) {
            try {
                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(
                        context.getResources(), bitmap);
                drawable.setCircular(true);
                drawable.setAntiAlias(true);
                return drawable;
            } catch (Exception e) {
                Log.e(TAG, "Failed to create circular avatar", e);
            }
        }
        return null;
    }

    public static boolean saveAccountProfile(
            Context context,
            String username,
            String realName,
            String motto,
            Bitmap avatarBitmap) {
        try {
            if (TextUtils.isEmpty(motto)) {
                motto = DEFAULT_MOTTO;
            }
            SharedPreferences.Editor editor =
                    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
            editor.putBoolean(KEY_ACCOUNT_CREATED, true);
            editor.putString(KEY_ACCOUNT_USERNAME, username);
            editor.putString(KEY_ACCOUNT_REAL_NAME, realName);
            editor.putString(KEY_ACCOUNT_MOTTO, motto);
            editor.apply();

            try {
                Settings.System.putInt(context.getContentResolver(), KEY_ACCOUNT_CREATED, 1);
                Settings.System.putString(
                        context.getContentResolver(), KEY_ACCOUNT_USERNAME, username);
                Settings.System.putString(
                        context.getContentResolver(), KEY_ACCOUNT_REAL_NAME, realName);
                Settings.System.putString(
                        context.getContentResolver(), KEY_ACCOUNT_MOTTO, motto);
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

    public static boolean saveAccount(Context context, String name, String phone, Bitmap avatarBitmap) {
        return saveAccountProfile(context, name, name, DEFAULT_MOTTO, avatarBitmap);
    }

    public static boolean saveAvatarFromUri(Context context, Uri uri) {
        if (context == null || uri == null) return false;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, bounds);
            }
            int sampleSize = 1;
            int maxDim = Math.max(bounds.outWidth, bounds.outHeight);
            while (maxDim / sampleSize > 512) {
                sampleSize *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(is, null, opts);
            }
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
        int size = Math.min(width, height);
        int cropX = Math.max((width - size) / 2, 0);
        int cropY = Math.max((height - size) / 2, 0);
        return Bitmap.createBitmap(bitmap, cropX, cropY, size, size);
    }

    public static void deleteAccount(Context context) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        try {
            Settings.System.putInt(context.getContentResolver(), KEY_ACCOUNT_CREATED, 0);
            Settings.System.putString(context.getContentResolver(), KEY_ACCOUNT_USERNAME, "");
            Settings.System.putString(context.getContentResolver(), KEY_ACCOUNT_REAL_NAME, "");
            Settings.System.putString(context.getContentResolver(), KEY_ACCOUNT_MOTTO, "");
            Settings.System.putString(context.getContentResolver(), KEY_ACCOUNT_PHONE, "");
        } catch (Exception ignored) {}

        File avatarFile = getAvatarFile(context);
        if (avatarFile.exists()) {
            avatarFile.delete();
        }
    }
}
