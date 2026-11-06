/*
 * Copyright (C) 2022-2026 crDroid Android Project & The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.settings.fragments.themes;

import static com.android.internal.util.evolution.ThemeUtils.ICON_SHAPE_KEY;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.PathParser;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.evolution.ThemeUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.Utils;

import java.util.ArrayList;
import java.util.List;

public class IconShapes extends SettingsPreferenceFragment {

    private RecyclerView mRecyclerView;
    private ThemeUtils mThemeUtils;
    private final String mCategory = ICON_SHAPE_KEY;
    private final List<ShapeOption> mOptions = new ArrayList<>();

    public static class ShapeOption {
        public final String key;
        public final String title;
        public final String svgPath;
        public final String overlayPkg;

        public ShapeOption(String key, String title, String svgPath, String overlayPkg) {
            this.key = key;
            this.title = title;
            this.svgPath = svgPath;
            this.overlayPkg = overlayPkg;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isAdded()) return;
        requireActivity().setTitle(R.string.themes_icon_shape_title);
        mThemeUtils = ThemeUtils.getInstance(requireContext());
        loadShapeOptions(requireContext());
    }

    private void loadShapeOptions(Context context) {
        mOptions.clear();
        ContentResolver resolver = context.getContentResolver();

        // 1. Query Launcher3 GridCustomizationsProvider (/shape_options)
        String[] authorities = new String[]{
                "com.android.launcher3.grid_control",
                "com.google.android.apps.nexuslauncher.grid_control"
        };
        for (String auth : authorities) {
            Uri uri = new Uri.Builder()
                    .scheme("content")
                    .authority(auth)
                    .appendPath("shape_options")
                    .build();
            try (Cursor c = resolver.query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int keyIdx = c.getColumnIndex("shape_key");
                    int titleIdx = c.getColumnIndex("shape_title");
                    int pathIdx = c.getColumnIndex("path");
                    do {
                        String key = keyIdx >= 0 ? c.getString(keyIdx) : "";
                        String title = titleIdx >= 0 ? c.getString(titleIdx) : key;
                        String path = pathIdx >= 0 ? c.getString(pathIdx) : "";
                        if (key != null && !key.isEmpty()) {
                            mOptions.add(new ShapeOption(key, title != null ? title : key, path, null));
                        }
                    } while (c.moveToNext());
                    if (!mOptions.isEmpty()) break;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Built-in Flagship Shapes (always available even if Launcher provider is restricted)
        addIfMissing("circle", "Daire (Pixel)",
                "M50,0 C77.6,0 100,22.4 100,50 C100,77.6 77.6,100 50,100 C22.4,100 0,77.6 0,50 C0,22.4 22.4,0 50,0 Z");
        addIfMissing("squircle", "ColorOS 17 Süper Elips",
                "M50,0 C10,0 0,10 0,50 C0,90 10,100 50,100 C90,100 100,90 100,50 C100,10 90,0 50,0 Z");
        addIfMissing("rounded_square", "iOS 27 Yuvarlatılmış",
                "M22,0 L78,0 C90.15,0 100,9.85 100,22 L100,78 C100,90.15 90.15,100 78,100 L22,100 C9.85,100 0,90.15 0,78 L0,22 C0,9.85 9.85,0 22,0 Z");
        addIfMissing("teardrop", "Damla (Teardrop)",
                "M50,0 C77.6,0 100,22.4 100,50 L100,86 C100,93.7 93.7,100 86,100 L50,100 C22.4,100 0,77.6 0,50 C0,22.4 22.4,0 50,0 Z");
        addIfMissing("pebble", "One UI 9 Çakıl Taşı",
                "M55,0 C82,0 100,18 100,45 C100,78 78,100 45,100 C18,100 0,82 0,55 C0,22 22,0 55,0 Z");
        addIfMissing("hexagon", "Altıgen (Cyber)",
                "M50,0 L95,25 L95,75 L50,100 L5,75 L5,25 Z");

        // 3. Also append any installed RRO Icon Shape overlays
        if (mThemeUtils != null) {
            List<String> pkgs = mThemeUtils.getOverlayPackagesForCategory(mCategory, "android");
            if (pkgs != null) {
                for (String pkg : pkgs) {
                    if ("android".equals(pkg)) continue;
                    String label = pkg;
                    try {
                        label = context.getPackageManager()
                                .getApplicationInfo(pkg, 0)
                                .loadLabel(context.getPackageManager()).toString();
                    } catch (Throwable ignored) {}
                    mOptions.add(new ShapeOption(pkg, label, null, pkg));
                }
            }
        }
    }

    private void addIfMissing(String key, String title, String svgPath) {
        for (ShapeOption opt : mOptions) {
            if (opt.key.equalsIgnoreCase(key)) return;
        }
        mOptions.add(new ShapeOption(key, title, svgPath, null));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_view, container, false);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        mRecyclerView.setAdapter(new Adapter(requireContext(), mOptions, mThemeUtils, mCategory));
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

    private static class SvgShapeDrawable extends Drawable {
        private final Path mSourcePath;
        private final Path mDrawPath = new Path();
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix mMatrix = new Matrix();

        SvgShapeDrawable(String pathData, int color) {
            Path parsed = null;
            try {
                if (pathData != null && !pathData.isEmpty()) {
                    parsed = PathParser.createPathFromPathData(pathData);
                }
            } catch (Throwable ignored) {}
            if (parsed == null) {
                parsed = new Path();
                parsed.addCircle(50f, 50f, 50f, Path.Direction.CW);
            }
            mSourcePath = parsed;
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(color);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            RectF src = new RectF(0f, 0f, 100f, 100f);
            float pad = bounds.width() * 0.1f;
            RectF dst = new RectF(bounds.left + pad, bounds.top + pad,
                    bounds.right - pad, bounds.bottom - pad);
            mMatrix.reset();
            mMatrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
            mSourcePath.transform(mMatrix, mDrawPath);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.drawPath(mDrawPath, mPaint);
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

    public static class Adapter extends RecyclerView.Adapter<Adapter.CustomViewHolder> {
        private final Context mContext;
        private final List<ShapeOption> mOptions;
        private final ThemeUtils mThemeUtils;
        private final String mCategory;
        private String mSelectedKey;

        public Adapter(Context context, List<ShapeOption> options, ThemeUtils themeUtils, String category) {
            mContext = context;
            mOptions = options;
            mThemeUtils = themeUtils;
            mCategory = category;
            String saved = Settings.Secure.getString(context.getContentResolver(), "blaze_icon_shape_key");
            mSelectedKey = (saved != null && !saved.isEmpty()) ? saved : "circle";
        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
            ShapeOption opt = mOptions.get(position);
            boolean isSelected = opt.key.equalsIgnoreCase(mSelectedKey);
            int accent = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorAccent);
            int tintColor = ColorUtils.setAlphaComponent(accent, isSelected ? 220 : 95);

            Drawable shapeDrawable = null;
            if (opt.overlayPkg != null && mThemeUtils != null) {
                shapeDrawable = mThemeUtils.createShapeDrawable(opt.overlayPkg);
                if (shapeDrawable != null) {
                    shapeDrawable.setTintList(ColorStateList.valueOf(tintColor));
                }
            }
            if (shapeDrawable == null) {
                shapeDrawable = new SvgShapeDrawable(opt.svgPath, tintColor);
            }

            holder.image.setBackground(shapeDrawable);
            holder.name.setText(opt.title);
            View tile = holder.itemView.findViewById(R.id.option_tile);
            if (tile != null) tile.setBackground(null);
            holder.itemView.setActivated(isSelected);

            holder.itemView.setOnClickListener(v -> {
                mSelectedKey = opt.key;
                Settings.Secure.putString(mContext.getContentResolver(), "blaze_icon_shape_key", opt.key);
                applyShapeToLauncherAndSystem(opt);
                notifyDataSetChanged();
                Toast.makeText(mContext, "İkon şekli uygulandı: " + opt.title, Toast.LENGTH_SHORT).show();
            });
        }

        private void applyShapeToLauncherAndSystem(ShapeOption opt) {
            String[] authorities = new String[]{
                    "com.android.launcher3.grid_control",
                    "com.google.android.apps.nexuslauncher.grid_control"
            };
            for (String auth : authorities) {
                try {
                    Uri uri = new Uri.Builder()
                            .scheme("content")
                            .authority(auth)
                            .appendPath("shape")
                            .build();
                    ContentValues values = new ContentValues();
                    values.put("shape_key", opt.key);
                    mContext.getContentResolver().update(uri, values, null, null);
                } catch (Throwable ignored) {}
            }
            if (mThemeUtils != null && opt.overlayPkg != null) {
                try {
                    mThemeUtils.setOverlayEnabled(mCategory, opt.overlayPkg, "android");
                } catch (Throwable ignored) {}
            }
        }

        @Override
        public int getItemCount() {
            return mOptions.size();
        }

        public static class CustomViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView image;
            public CustomViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.option_label);
                image = itemView.findViewById(R.id.option_thumbnail);
            }
        }
    }
}
