/*
 * Copyright (C) 2022-2026 crDroid Android Project & The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.settings.fragments.themes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.evolution.ThemeUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.Utils;

import java.util.ArrayList;
import java.util.List;

public class NavigationBarIcons extends SettingsPreferenceFragment {

    private RecyclerView mRecyclerView;
    private ThemeUtils mThemeUtils;
    private final String mCategory = "android.theme.customization.navbar";
    private final List<NavStyleItem> mItems = new ArrayList<>();

    public static class NavStyleItem {
        public final int styleId;
        public final String key;
        public final String title;

        public NavStyleItem(int styleId, String key, String title) {
            this.styleId = styleId;
            this.key = key;
            this.title = title;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            getActivity().setTitle(R.string.themes_navigation_bar_icons_title);
        }
        mThemeUtils = ThemeUtils.getInstance(getActivity());
        mItems.clear();
        mItems.add(new NavStyleItem(0, "default", "Standart Android 17 (Pixel)"));
        mItems.add(new NavStyleItem(1, "coloros17", "ColorOS 17 Minimal Çizgi"));
        mItems.add(new NavStyleItem(2, "oneui9", "One UI 9 Yumuşak Hatlar"));
        mItems.add(new NavStyleItem(3, "ios27", "iOS 27 Kapsül Geometrik"));
        mItems.add(new NavStyleItem(4, "filled_expressive", "Material 3 Expressive Dolgulu"));
        mItems.add(new NavStyleItem(5, "cyber_neon", "Blaze Cyber Noktalı"));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_view, container, false);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        mRecyclerView.setAdapter(new Adapter(requireContext()));
        return view;
    }

    @Override
    public void setDivider(@Nullable final Drawable divider) {
        RecyclerView list = getListView();
        if (list == null) {
            View root = getView();
            if (root != null) {
                root.post(() -> setDivider(divider));
            }
            return;
        }
        super.setDivider(divider);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.VIEW_UNKNOWN;
    }

    private static class NavButtonDrawable extends Drawable {
        private final int mStyle;
        private final int mButtonType; // 0=Back, 1=Home, 2=Recents
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        NavButtonDrawable(int style, int buttonType, int color) {
            mStyle = style;
            mButtonType = buttonType;
            mPaint.setColor(color);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            float cx = b.exactCenterX();
            float cy = b.exactCenterY();
            float r = Math.min(b.width(), b.height()) * 0.30f;
            float stroke = Math.max(3f, r * 0.22f);

            boolean filled = (mStyle == 0 || mStyle == 4);
            mPaint.setStyle(filled ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);
            mPaint.setStrokeWidth(filled ? stroke * 0.5f : stroke);

            if (mButtonType == 0) {
                // Back icon
                Path p = new Path();
                if (mStyle == 1 || mStyle == 2) {
                    // Chevron <
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(stroke * 1.15f);
                    p.moveTo(cx + r * 0.45f, cy - r * 0.75f);
                    p.lineTo(cx - r * 0.45f, cy);
                    p.lineTo(cx + r * 0.45f, cy + r * 0.75f);
                } else {
                    // Triangle
                    p.moveTo(cx - r * 0.75f, cy);
                    p.lineTo(cx + r * 0.65f, cy - r * 0.80f);
                    p.lineTo(cx + r * 0.65f, cy + r * 0.80f);
                    p.close();
                }
                canvas.drawPath(p, mPaint);
            } else if (mButtonType == 1) {
                // Home icon
                if (mStyle == 2) {
                    // One UI Squircle
                    RectF rf = new RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f);
                    canvas.drawRoundRect(rf, r * 0.55f, r * 0.55f, mPaint);
                } else if (mStyle == 3) {
                    // Capsule pill
                    RectF rf = new RectF(cx - r * 1.05f, cy - r * 0.52f, cx + r * 1.05f, cy + r * 0.52f);
                    canvas.drawRoundRect(rf, r * 0.52f, r * 0.52f, mPaint);
                } else if (mStyle == 5) {
                    mPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawCircle(cx, cy, r * 0.82f, mPaint);
                    mPaint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(cx, cy, r * 0.32f, mPaint);
                } else {
                    canvas.drawCircle(cx, cy, r * 0.78f, mPaint);
                }
            } else {
                // Recents icon
                if (mStyle == 2) {
                    // One UI III vertical bars
                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setStrokeWidth(stroke * 1.15f);
                    canvas.drawLine(cx - r * 0.65f, cy - r * 0.70f, cx - r * 0.65f, cy + r * 0.70f, mPaint);
                    canvas.drawLine(cx, cy - r * 0.70f, cx, cy + r * 0.70f, mPaint);
                    canvas.drawLine(cx + r * 0.65f, cy - r * 0.70f, cx + r * 0.65f, cy + r * 0.70f, mPaint);
                } else {
                    RectF rf = new RectF(cx - r * 0.75f, cy - r * 0.75f, cx + r * 0.75f, cy + r * 0.75f);
                    float rad = (mStyle == 0) ? r * 0.18f : r * 0.38f;
                    canvas.drawRoundRect(rf, rad, rad, mPaint);
                }
            }
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.CustomViewHolder> {
        private final Context mContext;
        private String mSelectedKey;

        public Adapter(Context context) {
            mContext = context;
            String saved = Settings.Secure.getString(context.getContentResolver(), "blaze_navbar_icon_style");
            mSelectedKey = (saved != null && !saved.isEmpty()) ? saved : "default";
        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.navbar_option, parent, false);
            return new CustomViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomViewHolder holder, final int position) {
            NavStyleItem item = mItems.get(position);
            boolean isSelected = item.key.equals(mSelectedKey);
            int color = Utils.getColorAttrDefaultColor(mContext,
                    isSelected ? android.R.attr.colorAccent : android.R.attr.textColorPrimary);

            holder.image1.setBackground(new NavButtonDrawable(item.styleId, 0, color));
            holder.image2.setBackground(new NavButtonDrawable(item.styleId, 1, color));
            holder.image3.setBackground(new NavButtonDrawable(item.styleId, 2, color));
            holder.name.setText(item.title);
            holder.itemView.setActivated(isSelected);

            holder.itemView.setOnClickListener(v -> {
                mSelectedKey = item.key;
                Settings.Secure.putString(mContext.getContentResolver(), "blaze_navbar_icon_style", item.key);
                if (mThemeUtils != null) {
                    try {
                        List<String> pkgs = mThemeUtils.getOverlayPackagesForCategory(mCategory, "com.android.launcher3");
                        if (pkgs != null && position < pkgs.size()) {
                            mThemeUtils.setOverlayEnabled(mCategory, pkgs.get(position), "com.android.launcher3");
                        }
                    } catch (Throwable ignored) {}
                }
                notifyDataSetChanged();
                Toast.makeText(mContext, "Gezinme çubuğu stili seçildi: " + item.title, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        public class CustomViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView image1;
            ImageView image2;
            ImageView image3;
            public CustomViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.option_label);
                image1 = itemView.findViewById(R.id.image1);
                image2 = itemView.findViewById(R.id.image2);
                image3 = itemView.findViewById(R.id.image3);
            }
        }
    }
}
