/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.account;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settingslib.widget.SettingsThemeHelper;

public class BlazeAccountActivity extends Activity {

    private static final int REQUEST_IMAGE_PICK = 1002;

    private ImageView mAvatarView;
    private EditText mUsernameEdit;
    private EditText mRealNameEdit;
    private EditText mMottoEdit;
    private View mSaveBtn;
    private View mDeleteBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            setTheme(R.style.Theme_SubSettings_Expressive);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blaze_account);

        View backBtn = findViewById(R.id.account_back_btn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        mAvatarView = findViewById(R.id.account_avatar);
        mUsernameEdit = findViewById(R.id.edit_account_username);
        mRealNameEdit = findViewById(R.id.edit_account_name);
        mMottoEdit = findViewById(R.id.edit_account_phone);
        mSaveBtn = findViewById(R.id.btn_save_account);
        mDeleteBtn = findViewById(R.id.btn_delete_account);

        FrameLayout avatarContainer = findViewById(R.id.avatar_container);
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> pickImage());
        }
        View changePhotoLabel = findViewById(R.id.btn_change_photo_label);
        if (changePhotoLabel != null) {
            changePhotoLabel.setOnClickListener(v -> pickImage());
        }

        loadAccountData();

        if (mSaveBtn != null) {
            mSaveBtn.setOnClickListener(v -> saveAccount());
        }

        if (mDeleteBtn != null) {
            mDeleteBtn.setOnClickListener(v -> deleteAccount());
        }
    }

    private void loadAccountData() {
        boolean configured = BlazeAccountUtils.isAccountConfigured(this);
        if (configured) {
            String username = BlazeAccountUtils.getUsername(this);
            String realName = BlazeAccountUtils.getRealName(this);
            String motto = BlazeAccountUtils.getMotto(this);
            if (mUsernameEdit != null) mUsernameEdit.setText(username);
            if (mRealNameEdit != null) mRealNameEdit.setText(realName);
            if (mMottoEdit != null) mMottoEdit.setText(motto);
            if (mDeleteBtn != null) mDeleteBtn.setVisibility(View.VISIBLE);
        } else {
            if (mMottoEdit != null) {
                mMottoEdit.setText(BlazeAccountUtils.DEFAULT_MOTTO);
            }
            if (mDeleteBtn != null) mDeleteBtn.setVisibility(View.GONE);
        }

        refreshAvatarPreview();
    }

    private void refreshAvatarPreview() {
        if (mAvatarView == null) return;
        Drawable circular = BlazeAccountUtils.getCircularAvatar(this);
        if (circular != null) {
            mAvatarView.setImageDrawable(circular);
        } else {
            mAvatarView.setImageResource(R.drawable.ic_blaze_account_avatar);
        }
    }

    private void pickImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        } catch (Exception e) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.setType("image/*");
            startActivityForResult(
                    Intent.createChooser(fallback, getString(R.string.blaze_account_change_photo)),
                    REQUEST_IMAGE_PICK);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                boolean saved = BlazeAccountUtils.saveAvatarFromUri(this, imageUri);
                if (saved) {
                    refreshAvatarPreview();
                }
            }
        }
    }

    private void saveAccount() {
        String username = mUsernameEdit != null && mUsernameEdit.getText() != null
                ? mUsernameEdit.getText().toString().trim() : "";
        String realName = mRealNameEdit != null && mRealNameEdit.getText() != null
                ? mRealNameEdit.getText().toString().trim() : "";
        String motto = mMottoEdit != null && mMottoEdit.getText() != null
                ? mMottoEdit.getText().toString().trim() : "";

        if (TextUtils.isEmpty(username) && TextUtils.isEmpty(realName)) {
            if (mUsernameEdit != null) {
                mUsernameEdit.setError(getString(R.string.blaze_account_username_hint));
            }
            return;
        }
        if (TextUtils.isEmpty(username)) {
            username = realName;
        }
        if (TextUtils.isEmpty(realName)) {
            realName = username;
        }
        if (TextUtils.isEmpty(motto)) {
            motto = BlazeAccountUtils.DEFAULT_MOTTO;
        }

        boolean saved = BlazeAccountUtils.saveAccountProfile(
                this, username, realName, motto, null);
        if (saved) {
            Toast.makeText(this, R.string.blaze_account_saved_toast, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void deleteAccount() {
        BlazeAccountUtils.deleteAccount(this);
        Toast.makeText(this, R.string.blaze_account_deleted_toast, Toast.LENGTH_SHORT).show();
        finish();
    }
}
