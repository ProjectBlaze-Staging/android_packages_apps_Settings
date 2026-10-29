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
import androidx.appcompat.app.AppCompatActivity;

import com.android.settings.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class BlazeAccountActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1002;

    private ImageView mAvatarView;
    private EditText mNameEdit;
    private EditText mPhoneEdit;
    private MaterialButton mSaveBtn;
    private MaterialButton mDeleteBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blaze_account);

        MaterialToolbar toolbar = findViewById(R.id.account_toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        mAvatarView = findViewById(R.id.account_avatar);
        mNameEdit = findViewById(R.id.edit_account_name);
        mPhoneEdit = findViewById(R.id.edit_account_phone);
        mSaveBtn = findViewById(R.id.btn_save_account);
        mDeleteBtn = findViewById(R.id.btn_delete_account);

        FrameLayout avatarContainer = findViewById(R.id.avatar_container);
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(v -> pickImage());
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
            String name = BlazeAccountUtils.getAccountName(this);
            String phone = BlazeAccountUtils.getAccountPhone(this);
            mNameEdit.setText(name);
            mPhoneEdit.setText(phone);
            mDeleteBtn.setVisibility(View.VISIBLE);
        } else {
            mDeleteBtn.setVisibility(View.GONE);
        }

        Drawable avatar = BlazeAccountUtils.getCircularAvatar(this);
        if (avatar != null && mAvatarView != null) {
            mAvatarView.setImageDrawable(avatar);
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.blaze_account_change_photo)), REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                boolean saved = BlazeAccountUtils.saveAvatarFromUri(this, imageUri);
                if (saved) {
                    Drawable avatar = BlazeAccountUtils.getCircularAvatar(this);
                    if (avatar != null && mAvatarView != null) {
                        mAvatarView.setImageDrawable(avatar);
                    }
                }
            }
        }
    }

    private void saveAccount() {
        String name = mNameEdit.getText() != null ? mNameEdit.getText().toString().trim() : "";
        String phone = mPhoneEdit.getText() != null ? mPhoneEdit.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            mNameEdit.setError(getString(R.string.blaze_account_name_hint));
            return;
        }

        boolean saved = BlazeAccountUtils.saveAccount(this, name, phone, null);
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
