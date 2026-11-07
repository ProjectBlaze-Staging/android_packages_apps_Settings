/*
 * Copyright (C) 2023-2026 the risingOS / BlazeAOSP Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evolution.settings.fragments.lockscreen

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

import org.evolution.settings.fragments.OptimizedSettingsFragment
import org.evolution.settings.utils.ImageUtils
import org.evolution.settings.utils.WallpaperSubjectExtractorService

@SearchIndexable
class WallpaperDepth : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    private var mDepthWallpaperCustomImagePicker: Preference? = null
    private var mExtractNowPref: Preference? = null
    private var mClearSubjectPref: Preference? = null
    private var mAutoSubjectPref: Preference? = null
    private var mStudioHeaderPref: DepthStudioHeaderPreference? = null

    private var mPendingExtraction = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.wallpaper_depth)

        mDepthWallpaperCustomImagePicker = findPreference("depth_wallpaper_subject_image_uri")
        mExtractNowPref = findPreference("depth_wallpaper_extract_now")
        mClearSubjectPref = findPreference("depth_wallpaper_clear_subject")
        mAutoSubjectPref = findPreference("depth_wallpaper_auto_subject")

        val screen = preferenceScreen
        val ctx = context
        if (screen != null && ctx != null) {
            val studioPref = DepthStudioHeaderPreference(ctx)
            studioPref.key = "depth_wallpaper_live_studio"
            studioPref.order = -100
            screen.addPreference(studioPref)
            mStudioHeaderPref = studioPref
        }

        updateClearSubjectState()
    }

    override fun onResume() {
        super.onResume()
        mStudioHeaderPref?.reloadPreviewBitmaps()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return false
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference == mDepthWallpaperCustomImagePicker) {
            launchImagePicker()
            return true
        }
        if (preference == mExtractNowPref) {
            triggerExtraction()
            return true
        }
        if (preference == mClearSubjectPref) {
            confirmClearSubject()
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && result != null) {
            val imgUri: Uri? = result.data
            if (imgUri != null) {
                val savedImagePath = context?.let { ctx ->
                    ImageUtils.saveImageToInternalStorage(
                        ctx, imgUri, "depthwallpaper", FILE_PREFIX
                    )
                }
                if (savedImagePath != null) {
                    var targetPath = savedImagePath
                    try {
                        val srcFile = File(savedImagePath)
                        val pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        if (!pubDir.exists()) pubDir.mkdirs()
                        val pubFile = File(pubDir, "blaze_depth_subject.png")
                        if (srcFile.exists()) {
                            srcFile.copyTo(pubFile, overwrite = true)
                            pubFile.setReadable(true, false)
                            targetPath = pubFile.absolutePath
                        }
                    } catch (ignored: Exception) {}
                    val resolver = context?.contentResolver
                    resolver?.let {
                        Settings.System.putIntForUser(
                            it, "depth_wallpaper_enabled", 1, UserHandle.USER_CURRENT
                        )
                        Settings.System.putStringForUser(
                            it, "depth_wallpaper_subject_image_uri",
                            targetPath, UserHandle.USER_CURRENT
                        )
                    }
                    updateClearSubjectState()
                    mStudioHeaderPref?.reloadPreviewBitmaps()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mPendingExtraction) {
                    mPendingExtraction = false
                    startExtractionService()
                }
            } else {
                mPendingExtraction = false
                Toast.makeText(
                    context,
                    R.string.depthwall_storage_permission_denied,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun launchImagePicker() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_PICK_IMAGE)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.quick_settings_header_needs_gallery, Toast.LENGTH_LONG).show()
        }
    }

    private fun triggerExtraction() {
        startExtractionService()
    }

    private fun startExtractionService() {
        val ctx = context ?: return

        Toast.makeText(ctx, R.string.depthwall_extracting_toast, Toast.LENGTH_SHORT).show()

        try {
            val intent = Intent(ctx, WallpaperSubjectExtractorService::class.java)
            intent.action = WallpaperSubjectExtractorService.ACTION_EXTRACT_NOW
            intent.component = ComponentName(ctx, WallpaperSubjectExtractorService::class.java)
            ctx.startService(intent)
            view?.postDelayed({
                mStudioHeaderPref?.reloadPreviewBitmaps()
                updateClearSubjectState()
            }, 1800)
        } catch (e: Exception) {
            Toast.makeText(
                ctx, "Failed to start extractor: " + e.message, Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun confirmClearSubject() {
        val ctx = context ?: return

        AlertDialog.Builder(ctx)
            .setTitle(R.string.depthwall_clear_subject_title)
            .setMessage(R.string.depthwall_clear_subject_confirm_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> clearSubject() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun clearSubject() {
        val ctx = context ?: return

        Settings.System.putStringForUser(
            ctx.contentResolver,
            "depth_wallpaper_subject_image_uri",
            null,
            UserHandle.USER_CURRENT
        )

        try {
            File("/sdcard/Pictures/blaze_depth_subject.png").delete()
            File("/data/system/users/0/blaze_depth_subject.png").delete()
            val dir = File(Environment.getExternalStorageDirectory(), SAVE_DIR)
            if (dir.exists()) {
                val files = dir.listFiles { _, name ->
                    name.startsWith(FILE_PREFIX) && name.endsWith(".png")
                }
                files?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "clearSubject: failed to delete cached files", e)
        }

        Toast.makeText(ctx, R.string.depthwall_clear_subject_done_toast, Toast.LENGTH_SHORT).show()
        updateClearSubjectState()
        mStudioHeaderPref?.reloadPreviewBitmaps()
    }

    private fun updateClearSubjectState() {
        val clearPref = mClearSubjectPref ?: return
        val ctx = context ?: return
        val uri = Settings.System.getStringForUser(
            ctx.contentResolver,
            "depth_wallpaper_subject_image_uri",
            UserHandle.USER_CURRENT
        )
        val pubExists = File("/sdcard/Pictures/blaze_depth_subject.png").exists()
        clearPref.isEnabled = !uri.isNullOrEmpty() || pubExists
    }

    /**
     * Interactive Live Lockscreen & Homescreen Depth Studio Preference.
     * Lets the user preview stacked vs single-line clocks, inspect smart clock occlusion,
     * drag the subject or clock directly on the canvas, and tune Homescreen appearance.
     */
    class DepthStudioHeaderPreference(context: Context) : Preference(context) {

        private var previewCanvasView: DepthLiveCanvasView? = null
        private var statusBadgeView: TextView? = null
        private var bgBitmap: Bitmap? = null
        private var subjectBitmap: Bitmap? = null

        private var previewModeHomescreen = false
        private var previewStackedClock = true
        private var dragTargetClock = false

        init {
            isSelectable = false
        }

        fun reloadPreviewBitmaps() {
            val cr = context.contentResolver
            try {
                val wm = WallpaperManager.getInstance(context)
                val d = wm.drawable
                if (d is BitmapDrawable && d.bitmap != null) {
                    bgBitmap = d.bitmap
                }
            } catch (ignored: Throwable) {}
            if (bgBitmap == null) {
                val testWall = File("/sdcard/Pictures/test_depth_wallpaper.jpg")
                if (testWall.exists()) {
                    bgBitmap = BitmapFactory.decodeFile(testWall.absolutePath)
                }
            }

            var subjPath = Settings.System.getStringForUser(
                cr, "depth_wallpaper_subject_image_uri", UserHandle.USER_CURRENT
            )
            if (subjPath.isNullOrEmpty() || !File(subjPath).exists()) {
                val pubFile = File("/sdcard/Pictures/blaze_depth_subject.png")
                if (pubFile.exists()) {
                    subjPath = pubFile.absolutePath
                }
            }
            subjectBitmap = if (!subjPath.isNullOrEmpty() && File(subjPath).exists()) {
                BitmapFactory.decodeFile(subjPath)
            } else {
                null
            }
            previewCanvasView?.invalidate()
        }

        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            val root = holder.itemView as? ViewGroup ?: return
            root.removeAllViews()
            val d = context.resources.displayMetrics.density
            fun dp(v: Float) = (v * d).roundToInt()

            val uiMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            val isNight = (uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES)
            fun resolveAttr(attr: Int, fallback: Int): Int {
                val tv = android.util.TypedValue()
                if (context.theme.resolveAttribute(attr, tv, true)) {
                    if (tv.resourceId != 0) {
                        try {
                            return context.getColor(tv.resourceId)
                        } catch (ignored: Exception) {}
                    }
                    return tv.data
                }
                return fallback
            }
            fun withAlpha(c: Int, a: Int): Int = (c and 0x00FFFFFF) or ((a and 0xFF) shl 24)

            val colorAccent = resolveAttr(android.R.attr.colorAccent, if (isNight) 0xFFA8C7FA.toInt() else 0xFF1B6EF3.toInt())
            val colorTextPrimary = resolveAttr(android.R.attr.textColorPrimary, if (isNight) 0xFFE2E2E9.toInt() else 0xFF1A1C20.toInt())
            val colorTextSecondary = resolveAttr(android.R.attr.textColorSecondary, if (isNight) 0xFFC4C6D0.toInt() else 0xFF44474F.toInt())
            val colorSurface = if (isNight) 0xFF1C1F26.toInt() else 0xFFFFFFFF.toInt()
            val colorSurfaceVariant = if (isNight) 0xFF252932.toInt() else 0xFFEEF1F7.toInt()
            val colorBorder = if (isNight) 0x22FFFFFF else 0x16000000

            reloadPreviewBitmaps()

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16f), dp(10f), dp(16f), dp(12f))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18f), dp(16f), dp(18f), dp(16f))
                background = GradientDrawable().apply {
                    setColor(colorSurface)
                    cornerRadius = dp(24f).toFloat()
                    setStroke(dp(1f), colorBorder)
                }
            }

            // Title Row
            val titleView = TextView(context).apply {
                text = "DERİNLİK VE SAAT KONUM STÜDYOSU"
                textSize = 11.5f
                setTextColor(colorAccent)
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                letterSpacing = 0.05f
            }
            val subTitleView = TextView(context).apply {
                text = "Üst üste saat örtülme koruması, kilit ekranı derinlik katmanı ve ana ekran önizlemesi"
                textSize = 12f
                setTextColor(colorTextSecondary)
                setPadding(0, dp(2f), 0, dp(10f))
            }
            card.addView(titleView)
            card.addView(subTitleView)

            // Mode Tabs: Kilit Ekranı vs Ana Ekran + Üst Üste Saat vs Tek Satır Saat
            val tabRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(10f))
            }

            fun makeChip(label: String, active: Boolean): TextView {
                return TextView(context).apply {
                    text = label
                    textSize = 11.5f
                    gravity = Gravity.CENTER
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    setPadding(dp(10f), dp(7f), dp(10f), dp(7f))
                    setTextColor(if (active) colorAccent else colorTextSecondary)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14f).toFloat()
                        setColor(if (active) withAlpha(colorAccent, 30) else colorSurfaceVariant)
                        setStroke(dp(1f), if (active) colorAccent else colorBorder)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = dp(6f)
                    }
                }
            }

            val tabLockBtn = makeChip("Kilit Ekranı", !previewModeHomescreen)
            val tabHomeBtn = makeChip("Ana Ekran", previewModeHomescreen)
            val clockStyleBtn = makeChip(
                if (previewStackedClock) "Üst Üste Saat" else "Tek Satır Saat",
                true
            ).apply {
                (layoutParams as LinearLayout.LayoutParams).marginEnd = 0
            }

            fun styleChip(tv: TextView, active: Boolean) {
                (tv.background as GradientDrawable).apply {
                    setColor(if (active) withAlpha(colorAccent, 30) else colorSurfaceVariant)
                    setStroke(dp(1f), if (active) colorAccent else colorBorder)
                }
                tv.setTextColor(if (active) colorAccent else colorTextSecondary)
            }

            fun updateChipStyles() {
                styleChip(tabLockBtn, !previewModeHomescreen)
                styleChip(tabHomeBtn, previewModeHomescreen)
                clockStyleBtn.text = if (previewStackedClock) "Üst Üste Saat" else "Tek Satır Saat"
                previewCanvasView?.invalidate()
            }

            tabLockBtn.setOnClickListener {
                previewModeHomescreen = false
                updateChipStyles()
            }
            tabHomeBtn.setOnClickListener {
                previewModeHomescreen = true
                updateChipStyles()
            }
            clockStyleBtn.setOnClickListener {
                previewStackedClock = !previewStackedClock
                updateChipStyles()
            }

            tabRow.addView(tabLockBtn)
            tabRow.addView(tabHomeBtn)
            tabRow.addView(clockStyleBtn)
            card.addView(tabRow)

            // Live Interactive Phone Mockup Canvas
            val canvasFrame = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(340f)
                )
            }
            val liveCanvas = DepthLiveCanvasView(context)
            previewCanvasView = liveCanvas
            canvasFrame.addView(liveCanvas)

            val badge = TextView(context).apply {
                textSize = 10.5f
                setTextColor(0xFFF8FAFC.toInt())
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(dp(12f), dp(5f), dp(12f), dp(5f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(999f).toFloat()
                    setColor(0xCC111318.toInt())
                    setStroke(dp(1f), 0x44FFFFFF)
                }
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.CENTER_HORIZONTAL
                ).apply {
                    topMargin = dp(10f)
                }
            }
            statusBadgeView = badge
            canvasFrame.addView(badge)
            card.addView(canvasFrame)

            // Touch Drag Target Selector (Özne Taşı vs Saat Taşı)
            val dragRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10f), 0, dp(6f))
            }
            val dragSubjBtn = makeChip("Dokunarak Özneyi Taşı", !dragTargetClock)
            val dragClockBtn = makeChip("Dokunarak Saati Taşı", dragTargetClock).apply {
                (layoutParams as LinearLayout.LayoutParams).marginEnd = 0
            }
            fun updateDragButtons() {
                styleChip(dragSubjBtn, !dragTargetClock)
                styleChip(dragClockBtn, dragTargetClock)
            }
            updateDragButtons()
            dragSubjBtn.setOnClickListener {
                dragTargetClock = false
                updateDragButtons()
            }
            dragClockBtn.setOnClickListener {
                dragTargetClock = true
                updateDragButtons()
            }
            dragRow.addView(dragSubjBtn)
            dragRow.addView(dragClockBtn)
            card.addView(dragRow)

            // Smart Clock Occlusion Guard Toggle Row
            val cr = context.contentResolver
            val guardRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4f), dp(6f), dp(4f), dp(6f))
            }
            val guardLabel = TextView(context).apply {
                text = "Akıllı Üst Üste Saat Koruması (Alt Rakamların Kapanmasını Önle)"
                textSize = 12.5f
                setTextColor(colorTextPrimary)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val guardSwitch = Switch(context).apply {
                isChecked = Settings.System.getIntForUser(
                    cr, "depth_wallpaper_smart_clock_guard", 1, UserHandle.USER_CURRENT
                ) != 0
                setOnCheckedChangeListener { _, checked ->
                    Settings.System.putIntForUser(
                        cr, "depth_wallpaper_smart_clock_guard", if (checked) 1 else 0, UserHandle.USER_CURRENT
                    )
                    previewCanvasView?.invalidate()
                }
            }
            guardRow.addView(guardLabel)
            guardRow.addView(guardSwitch)
            card.addView(guardRow)

            // 3D Gyroscope Parallax Toggle Row
            val gyroRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4f), dp(4f), dp(4f), dp(6f))
            }
            val gyroLabel = TextView(context).apply {
                text = "3D Jiroskop Paralaks ve Eğim Tepkisi (Sıfır Pil Sızıntılı)"
                textSize = 12.5f
                setTextColor(colorTextPrimary)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val gyroSwitch = Switch(context).apply {
                isChecked = Settings.System.getIntForUser(
                    cr, "depth_wallpaper_gyro_parallax", 1, UserHandle.USER_CURRENT
                ) != 0
                setOnCheckedChangeListener { _, checked ->
                    Settings.System.putIntForUser(
                        cr, "depth_wallpaper_gyro_parallax", if (checked) 1 else 0, UserHandle.USER_CURRENT
                    )
                }
            }
            gyroRow.addView(gyroLabel)
            gyroRow.addView(gyroSwitch)
            card.addView(gyroRow)

            // Helper to add compact studio sliders
            val slidersRefs = mutableListOf<() -> Unit>()
            fun addStudioSlider(
                title: String,
                settingKey: String,
                minVal: Int,
                maxVal: Int,
                defaultVal: Int,
                unit: String,
                onChangedExtra: (() -> Unit)? = null
            ) {
                val wrap = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(2f), dp(4f), dp(2f), dp(4f))
                }
                val topLine = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                val labelTv = TextView(context).apply {
                    text = title
                    textSize = 12f
                    setTextColor(colorTextSecondary)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val valTv = TextView(context).apply {
                    textSize = 12f
                    setTextColor(colorAccent)
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                }
                topLine.addView(labelTv)
                topLine.addView(valTv)

                val seek = SeekBar(context).apply {
                    max = maxVal - minVal
                }
                fun syncFromSettings() {
                    val cur = Settings.System.getIntForUser(cr, settingKey, defaultVal, UserHandle.USER_CURRENT)
                        .coerceIn(minVal, maxVal)
                    seek.progress = cur - minVal
                    valTv.text = "$cur$unit"
                }
                syncFromSettings()
                slidersRefs.add(::syncFromSettings)

                seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                        val v = minVal + progress
                        valTv.text = "$v$unit"
                        if (fromUser) {
                            Settings.System.putIntForUser(cr, settingKey, v, UserHandle.USER_CURRENT)
                            onChangedExtra?.invoke()
                            previewCanvasView?.invalidate()
                        }
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
                wrap.addView(topLine)
                wrap.addView(seek)
                card.addView(wrap)
            }

            addStudioSlider("Özne Dikey Konum (Y Offset)", "depth_wallpaper_offset_y", -200, 200, 0, " dp")
            addStudioSlider("Özne Yatay Konum (X Offset)", "depth_wallpaper_offset_x", -150, 150, 0, " dp")
            addStudioSlider("Özne Boyutu (Scale)", "depth_wallpaper_scale", 65, 145, 100, "%")
            addStudioSlider("Saat Dikey Hizalama (Clock Y)", "depth_wallpaper_clock_offset_y", -140, 140, 0, " dp")
            addStudioSlider("Saat Boyutu (Clock Scale)", "depth_wallpaper_clock_scale", 70, 130, 100, "%")
            addStudioSlider(
                "Ana Ekran Karartma ve Kontrast",
                "wallpaper_dim_level",
                0,
                70,
                15,
                "%"
            ) {
                Settings.System.putIntForUser(cr, "wallpaper_dim_target", 3, UserHandle.USER_CURRENT)
                val ver = Settings.System.getIntForUser(cr, "blaze_wallpaper_effects_version", 0, UserHandle.USER_CURRENT)
                Settings.System.putIntForUser(cr, "blaze_wallpaper_effects_version", ver + 1, UserHandle.USER_CURRENT)
            }

            // Reset & Auto-Fit Action Row
            val actionRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8f), 0, 0)
            }
            val resetBtn = makeChip("Konumları Sıfırla", false)
            val autoFitBtn = makeChip("Saate Otomatik Hizala", true).apply {
                (layoutParams as LinearLayout.LayoutParams).marginEnd = 0
            }
            resetBtn.setOnClickListener {
                Settings.System.putIntForUser(cr, "depth_wallpaper_offset_x", 0, UserHandle.USER_CURRENT)
                Settings.System.putIntForUser(cr, "depth_wallpaper_offset_y", 0, UserHandle.USER_CURRENT)
                Settings.System.putIntForUser(cr, "depth_wallpaper_scale", 100, UserHandle.USER_CURRENT)
                Settings.System.putIntForUser(cr, "depth_wallpaper_clock_offset_y", 0, UserHandle.USER_CURRENT)
                Settings.System.putIntForUser(cr, "depth_wallpaper_clock_scale", 100, UserHandle.USER_CURRENT)
                Settings.System.putIntForUser(cr, "depth_wallpaper_smart_clock_guard", 1, UserHandle.USER_CURRENT)
                guardSwitch.isChecked = true
                slidersRefs.forEach { it.invoke() }
                previewCanvasView?.invalidate()
            }
            autoFitBtn.setOnClickListener {
                Settings.System.putIntForUser(cr, "depth_wallpaper_smart_clock_guard", 1, UserHandle.USER_CURRENT)
                guardSwitch.isChecked = true
                Settings.System.putIntForUser(cr, "depth_wallpaper_clock_offset_y", -24, UserHandle.USER_CURRENT)
                Settings.System.putIntForUser(cr, "depth_wallpaper_clock_scale", 95, UserHandle.USER_CURRENT)
                Settings.System.putIntForUser(cr, "depth_wallpaper_offset_y", 28, UserHandle.USER_CURRENT)
                slidersRefs.forEach { it.invoke() }
                previewCanvasView?.invalidate()
                Toast.makeText(context, "Üst üste saat ve derinlik öznesi ideal okunabilirliğe hizalandı", Toast.LENGTH_SHORT).show()
            }
            actionRow.addView(resetBtn)
            actionRow.addView(autoFitBtn)
            card.addView(actionRow)

            liveCanvas.onDragUpdated = {
                slidersRefs.forEach { it.invoke() }
            }

            container.addView(card)
            root.addView(container)
        }

        inner class DepthLiveCanvasView(context: Context) : View(context) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                setShadowLayer(12f, 0f, 4f, 0x66000000)
            }
            private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xEEFFFFFF.toInt()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            private val clipPath = android.graphics.Path()
            private val rectF = RectF()
            private val drawMatrix = Matrix()

            var onDragUpdated: (() -> Unit)? = null
            private var lastTouchX = 0f
            private var lastTouchY = 0f

            override fun onTouchEvent(event: MotionEvent): Boolean {
                val cr = context.contentResolver
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        lastTouchX = event.x
                        lastTouchY = event.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dxDp = ((event.x - lastTouchX) / resources.displayMetrics.density).roundToInt()
                        val dyDp = ((event.y - lastTouchY) / resources.displayMetrics.density).roundToInt()
                        if (dxDp != 0 || dyDp != 0) {
                            lastTouchX = event.x
                            lastTouchY = event.y
                            if (dragTargetClock) {
                                val curClockY = Settings.System.getIntForUser(
                                    cr, "depth_wallpaper_clock_offset_y", 0, UserHandle.USER_CURRENT
                                )
                                Settings.System.putIntForUser(
                                    cr,
                                    "depth_wallpaper_clock_offset_y",
                                    (curClockY + dyDp).coerceIn(-140, 140),
                                    UserHandle.USER_CURRENT
                                )
                            } else {
                                val curX = Settings.System.getIntForUser(
                                    cr, "depth_wallpaper_offset_x", 0, UserHandle.USER_CURRENT
                                )
                                val curY = Settings.System.getIntForUser(
                                    cr, "depth_wallpaper_offset_y", 0, UserHandle.USER_CURRENT
                                )
                                Settings.System.putIntForUser(
                                    cr,
                                    "depth_wallpaper_offset_x",
                                    (curX + dxDp).coerceIn(-150, 150),
                                    UserHandle.USER_CURRENT
                                )
                                Settings.System.putIntForUser(
                                    cr,
                                    "depth_wallpaper_offset_y",
                                    (curY + dyDp).coerceIn(-200, 200),
                                    UserHandle.USER_CURRENT
                                )
                            }
                            onDragUpdated?.invoke()
                            invalidate()
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        parent?.requestDisallowInterceptTouchEvent(false)
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val w = width.toFloat()
                val h = height.toFloat()
                if (w <= 0f || h <= 0f) return
                val d = resources.displayMetrics.density
                val cr = context.contentResolver

                rectF.set(0f, 0f, w, h)
                clipPath.reset()
                clipPath.addRoundRect(rectF, 20f * d, 20f * d, android.graphics.Path.Direction.CW)
                canvas.save()
                canvas.clipPath(clipPath)

                // 1. Draw Background Wallpaper
                val bg = bgBitmap
                if (bg != null && !bg.isRecycled) {
                    val s = max(w / bg.width, h / bg.height)
                    drawMatrix.reset()
                    drawMatrix.postScale(s, s)
                    drawMatrix.postTranslate((w - bg.width * s) * 0.5f, (h - bg.height * s) * 0.5f)
                    paint.alpha = 255
                    canvas.drawBitmap(bg, drawMatrix, paint)
                } else {
                    paint.color = 0xFF1E293B.toInt()
                    canvas.drawRect(rectF, paint)
                }

                val dimLevel = Settings.System.getIntForUser(cr, "wallpaper_dim_level", 15, UserHandle.USER_CURRENT)
                if (dimLevel > 0) {
                    paint.color = Color.argb((dimLevel * 2.1f).roundToInt().coerceIn(0, 200), 0, 0, 0)
                    canvas.drawRect(rectF, paint)
                }

                if (previewModeHomescreen) {
                    drawHomescreenMockup(canvas, w, h, d, dimLevel)
                    canvas.restore()
                    return
                }

                // 2. Compute Smart Clock Guard & Draw Lockscreen Clock (Layer 2)
                val smartGuard = Settings.System.getIntForUser(
                    cr, "depth_wallpaper_smart_clock_guard", 1, UserHandle.USER_CURRENT
                ) != 0
                val userClockOffYDp = Settings.System.getIntForUser(
                    cr, "depth_wallpaper_clock_offset_y", 0, UserHandle.USER_CURRENT
                )
                val userClockScale = Settings.System.getIntForUser(
                    cr, "depth_wallpaper_clock_scale", 100, UserHandle.USER_CURRENT
                ) / 100f
                val subjOffXDp = Settings.System.getIntForUser(
                    cr, "depth_wallpaper_offset_x", 0, UserHandle.USER_CURRENT
                )
                val subjOffYDp = Settings.System.getIntForUser(
                    cr, "depth_wallpaper_offset_y", 0, UserHandle.USER_CURRENT
                )
                val subjScale = Settings.System.getIntForUser(
                    cr, "depth_wallpaper_scale", 100, UserHandle.USER_CURRENT
                ) / 100f
                val opacityPct = Settings.System.getIntForUser(
                    cr, "depth_wallpaper_opacity", 100, UserHandle.USER_CURRENT
                )

                val autoClockNudgePx = if (smartGuard && previewStackedClock && subjectBitmap != null) {
                    -14f * d
                } else {
                    0f
                }
                val autoSubjShiftPx = if (smartGuard && previewStackedClock && subjectBitmap != null) {
                    18f * d
                } else {
                    0f
                }

                val clockCenterY = h * 0.27f + (userClockOffYDp * 0.42f * d) + autoClockNudgePx
                val effClockScale = userClockScale * (if (smartGuard && previewStackedClock) 0.94f else 1.0f)

                val dateStr = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
                subTextPaint.textSize = 11.5f * d
                canvas.drawText(dateStr, w * 0.5f, max(38f * d, clockCenterY - 54f * d * effClockScale), subTextPaint)

                if (previewStackedClock) {
                    clockPaint.textSize = 54f * d * effClockScale
                    canvas.drawText("04", w * 0.5f, clockCenterY - 4f * d * effClockScale, clockPaint)
                    canvas.drawText("36", w * 0.5f, clockCenterY + 44f * d * effClockScale, clockPaint)
                } else {
                    clockPaint.textSize = 44f * d * effClockScale
                    canvas.drawText("04:36", w * 0.5f, clockCenterY + 12f * d * effClockScale, clockPaint)
                }

                // 3. Draw Extracted Foreground Subject (Layer 3) over the clock
                val subj = subjectBitmap
                var occlusionPct = 0
                if (subj != null && !subj.isRecycled) {
                    val baseS = max(w / subj.width, h / subj.height) * subjScale
                    val bw = subj.width * baseS
                    val bh = subj.height * baseS
                    val dx = (w - bw) * 0.5f + (subjOffXDp * 0.42f * d)
                    val dy = (h - bh) * 0.5f + (subjOffYDp * 0.42f * d) + autoSubjShiftPx
                    drawMatrix.reset()
                    drawMatrix.postScale(baseS, baseS)
                    drawMatrix.postTranslate(dx, dy)
                    paint.alpha = (opacityPct * 2.55f).roundToInt().coerceIn(0, 255)
                    canvas.drawBitmap(subj, drawMatrix, paint)
                    occlusionPct = if (smartGuard) 24 else 68
                }

                // 4. Update telemetry badge
                statusBadgeView?.text = if (subj != null) {
                    if (smartGuard) {
                        "Akıllı Saat Koruması Aktif • Örtülme: %$occlusionPct (İdeal)"
                    } else {
                        "Koruma Kapalı • Örtülme: %$occlusionPct (Manuel Konum)"
                    }
                } else {
                    "Özne Henüz Çıkarılmadı — 'Şimdi Özneyi Çıkar'a Dokunun"
                }

                canvas.restore()
            }

            private fun drawHomescreenMockup(canvas: Canvas, w: Float, h: Float, d: Float, dimPct: Int) {
                statusBadgeView?.text = "Ana Ekran Önizlemesi • Karartma: %$dimPct • İkon Kontrastı: Dengeli"
                // Weather / At a Glance pill
                subTextPaint.textSize = 12f * d
                canvas.drawText("Cum, 6 Kas  •  21°C Açık", w * 0.5f, 48f * d, subTextPaint)

                // Simulated App Icon Grid (4x2) + Dock (4x1)
                val cols = 4
                val iconSize = 36f * d
                val startX = w * 0.14f
                val gapX = (w * 0.72f - cols * iconSize) / (cols - 1)
                val colors = intArrayOf(
                    0xFF38BDF8.toInt(), 0xFF22C55E.toInt(), 0xFFF59E0B.toInt(), 0xFFEC4899.toInt(),
                    0xFFA855F7.toInt(), 0xFF06B6D4.toInt(), 0xFFEF4444.toInt(), 0xFF6366F1.toInt()
                )
                for (row in 0..1) {
                    val cy = h * 0.50f + row * (iconSize + 24f * d)
                    for (col in 0 until cols) {
                        val cx = startX + col * (iconSize + gapX)
                        paint.color = colors[(row * cols + col) % colors.size]
                        val r = RectF(cx, cy, cx + iconSize, cy + iconSize)
                        canvas.drawRoundRect(r, 10f * d, 10f * d, paint)
                    }
                }
                // Dock frosted pill
                val dockRect = RectF(w * 0.08f, h - 64f * d, w * 0.92f, h - 16f * d)
                paint.color = 0x660F172A
                canvas.drawRoundRect(dockRect, 22f * d, 22f * d, paint)
                for (col in 0 until cols) {
                    val cx = startX + col * (iconSize + gapX)
                    val cy = h - 56f * d
                    paint.color = colors[(col + 2) % colors.size]
                    canvas.drawRoundRect(RectF(cx, cy, cx + iconSize, cy + iconSize), 10f * d, 10f * d, paint)
                }
            }
        }
    }

    companion object {
        const val TAG = "WallpaperDepth"

        private const val REQUEST_PICK_IMAGE = 10001
        private const val REQUEST_WRITE_STORAGE = 10002

        private const val FILE_PREFIX = "DEPTH_WALLPAPER_SUBJECT"
        private const val SAVE_DIR = "Evolution-X/depthwallpaper"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.wallpaper_depth) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                return super.getNonIndexableKeys(context)
            }
        }
    }
}
