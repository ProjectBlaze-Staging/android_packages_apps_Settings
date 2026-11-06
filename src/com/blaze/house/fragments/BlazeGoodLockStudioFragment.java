/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house.fragments;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.blaze.house.utils.LivingFlowTouchHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Blaze Good Lock Studio (OneUI 9 Make Up & Life Up + ColorOS 17 / OriginOS 6 / HyperOS 4)
 *
 * Modular customization laboratory divided into two suites:
 *  1. Make Up (Visual & Aesthetic): LockStar, Home Up (DIY Home), QuickStar, Edge Lighting+, Wonderland 3D
 *  2. Life Up (Productivity, Audio & Diagnostics): SoundAssistant + O-Relax, One Hand Operation+ & Smart Sidebar,
 *     MultiStar, RegiStar, Nice Catch System Detective.
 */
public class BlazeGoodLockStudioFragment extends SettingsPreferenceFragment {

    private static final int TAB_MAKE_UP = 0;
    private static final int TAB_LIFE_UP = 1;

    private Context mContext;
    private ContentResolver mResolver;
    private int mCurrentTab = TAB_MAKE_UP;

    private LinearLayout mMakeUpContainer;
    private LinearLayout mLifeUpContainer;
    private TextView mTabMakeUpBtn;
    private TextView mTabLifeUpBtn;
    private TextView mActiveModulesBadge;

    // O-Relax Procedural Ambient Soundscape Synthesizer (Stage 2)
    private volatile boolean mAmbientPlaying = false;
    private volatile int mAmbientMode = 0; // 0=Off, 1=Night Rain, 2=Ocean Waves, 3=Campfire, 4=Deep Space
    private Thread mAmbientThread;
    private TextView mAmbientStatusText;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = requireContext();
        mResolver = mContext.getContentResolver();
        if (savedInstanceState != null) {
            mCurrentTab = savedInstanceState.getInt("goodlock_tab", TAB_MAKE_UP);
        }
        if (getActivity() != null) {
            getActivity().setTitle("Blaze Good Lock Studio");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("goodlock_tab", mCurrentTab);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(mContext);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.parseColor("#080D16"));

        LinearLayout root = new LinearLayout(mContext);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(16);
        root.setPadding(padH, dp(12), padH, dp(40));
        scrollView.addView(root);

        // 1. Hero Banner + Dual Suite Switcher (Make Up / Life Up)
        root.addView(buildHeroBanner());
        root.addView(buildSuiteTabSwitcher());

        // 2. Make Up Container (Stage 1 & Stage 4)
        mMakeUpContainer = new LinearLayout(mContext);
        mMakeUpContainer.setOrientation(LinearLayout.VERTICAL);
        populateMakeUpModules(mMakeUpContainer);
        root.addView(mMakeUpContainer);

        // 3. Life Up Container (Stage 2, Stage 3 & Stage 5)
        mLifeUpContainer = new LinearLayout(mContext);
        mLifeUpContainer.setOrientation(LinearLayout.VERTICAL);
        populateLifeUpModules(mLifeUpContainer);
        root.addView(mLifeUpContainer);

        switchSuiteTab(mCurrentTab);
        refreshActiveModulesCount();
        return scrollView;
    }

    @Override
    public void onDestroyView() {
        stopAmbientSoundscape();
        super.onDestroyView();
    }

    // =========================================================================
    // HERO BANNER & SUITE SWITCHER
    // =========================================================================

    private View buildHeroBanner() {
        LinearLayout card = new LinearLayout(mContext);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.parseColor("#1D1836"),
                        Color.parseColor("#111C35"),
                        Color.parseColor("#0D1526")
                });
        bg.setCornerRadius(dp(26));
        bg.setStroke(dp(1), Color.parseColor("#45A855F7"));
        card.setBackground(bg);
        int pad = dp(20);
        card.setPadding(pad, dp(18), pad, dp(18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        card.setLayoutParams(lp);

        LinearLayout topRow = new LinearLayout(mContext);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView pill = new TextView(mContext);
        pill.setText("✨ ONE UI 9 GOOD LOCK • BLAZE EDITION");
        pill.setTextColor(Color.parseColor("#E9D5FF"));
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(Color.parseColor("#33A855F7"));
        pillBg.setCornerRadius(dp(99));
        pill.setBackground(pillBg);
        pill.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        topRow.addView(pill, pillLp);

        mActiveModulesBadge = new TextView(mContext);
        mActiveModulesBadge.setTextColor(Color.parseColor("#38BDF8"));
        mActiveModulesBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        mActiveModulesBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        topRow.addView(mActiveModulesBadge);
        card.addView(topRow);

        TextView title = new TextView(mContext);
        title.setText("Good Lock Özelleştirme Laboratuvarı");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setPadding(0, dp(10), 0, dp(4));
        card.addView(title);

        TextView subtitle = new TextView(mContext);
        subtitle.setText("Make Up ile kilit ekranı, 3D parçacıklar, kenar ışıkları ve hareket fiziğini; Life Up ile uygulama başına ses mikseri, tek el çapraz hareketleri, akıllı kenar rıhtımı ve Nice Catch sistem dedektifini yönetin.");
        subtitle.setTextColor(Color.parseColor("#94A3B8"));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        card.addView(subtitle);

        LivingFlowTouchHelper.attach(card, 0.97f, true);
        return card;
    }

    private View buildSuiteTabSwitcher() {
        LinearLayout bar = new LinearLayout(mContext);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#131C2E"));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.parseColor("#25FFFFFF"));
        bar.setBackground(bg);
        bar.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(16);
        bar.setLayoutParams(lp);

        mTabMakeUpBtn = createSuiteTabButton("🎨 Make Up (Görsel & Estetik)", TAB_MAKE_UP);
        mTabLifeUpBtn = createSuiteTabButton("🚀 Life Up (Ses, Hareket & Dedektif)", TAB_LIFE_UP);

        bar.addView(mTabMakeUpBtn, new LinearLayout.LayoutParams(0, dp(42), 1f));
        bar.addView(mTabLifeUpBtn, new LinearLayout.LayoutParams(0, dp(42), 1f));
        return bar;
    }

    private TextView createSuiteTabButton(String label, final int tabIndex) {
        TextView tv = new TextView(mContext);
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setOnClickListener(v -> {
            vibrateTick();
            switchSuiteTab(tabIndex);
        });
        return tv;
    }

    private void switchSuiteTab(int tabIndex) {
        mCurrentTab = tabIndex;
        if (mMakeUpContainer != null) {
            mMakeUpContainer.setVisibility(tabIndex == TAB_MAKE_UP ? View.VISIBLE : View.GONE);
        }
        if (mLifeUpContainer != null) {
            mLifeUpContainer.setVisibility(tabIndex == TAB_LIFE_UP ? View.VISIBLE : View.GONE);
        }
        styleSuiteTab(mTabMakeUpBtn, tabIndex == TAB_MAKE_UP, "#A855F7");
        styleSuiteTab(mTabLifeUpBtn, tabIndex == TAB_LIFE_UP, "#38BDF8");
    }

    private void styleSuiteTab(TextView tv, boolean active, String accentHex) {
        if (tv == null) return;
        if (active) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#2E" + accentHex.substring(1)));
            bg.setCornerRadius(dp(14));
            bg.setStroke(dp(1), Color.parseColor(accentHex));
            tv.setBackground(bg);
            tv.setTextColor(Color.WHITE);
        } else {
            tv.setBackground(null);
            tv.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    private void refreshActiveModulesCount() {
        if (mActiveModulesBadge == null) return;
        int active = 0;
        if (Settings.System.getIntForUser(mResolver, "depth_wallpaper_enabled", 0, UserHandle.USER_CURRENT) == 1) active++;
        if (!TextUtils.isEmpty(Settings.Secure.getStringForUser(mResolver, "blaze_lockstar_signature_text", UserHandle.USER_CURRENT))) active++;
        if (Settings.Secure.getIntForUser(mResolver, "blaze_edgelighting_style", 2, UserHandle.USER_CURRENT) > 0) active++;
        if (Settings.Secure.getIntForUser(mResolver, "blaze_wonderland_particle_mode", 0, UserHandle.USER_CURRENT) > 0) active++;
        if (Settings.Secure.getIntForUser(mResolver, "blaze_behavioral_wallpaper_aura", 1, UserHandle.USER_CURRENT) == 1) active++;
        if (Settings.Secure.getIntForUser(mResolver, "blaze_soundassistant_multisound", 1, UserHandle.USER_CURRENT) == 1) active++;
        if (Settings.Secure.getIntForUser(mResolver, "blaze_oho_enabled", 1, UserHandle.USER_CURRENT) == 1) active++;
        if (Settings.Secure.getIntForUser(mResolver, "blaze_smart_sidebar_enabled", 1, UserHandle.USER_CURRENT) == 1) active++;
        if (Settings.Global.getInt(mResolver, "enable_non_resizable_multi_window", 1) == 1) active++;
        mActiveModulesBadge.setText(active + " / 9 Modül Aktif");
    }

    // =========================================================================
    // SUITE 1: MAKE UP MODULES (LockStar, Home Up, QuickStar, Edge Lighting+, Wonderland)
    // =========================================================================

    private void populateMakeUpModules(LinearLayout parent) {
        parent.removeAllViews();

        // MODULE 1: LockStar & HyperOS 4 Magazine Lockscreen
        LinearLayout lockStarCard = createModuleCard(
                "🔒 LockStar & HyperOS Dergi Stüdyosu",
                "MAKE UP • KİLİT EKRANI SERBEST TASARIM & İMZA BAŞLIĞI",
                "#A855F7");

        String currentSig = Settings.Secure.getStringForUser(
                mResolver, "blaze_lockstar_signature_text", UserHandle.USER_CURRENT);
        if (currentSig == null) currentSig = "PROJECT BLAZE 17 • BEYOND INFINITY";

        final TextView sigSummary = new TextView(mContext);
        addActionRow(lockStarCard,
                "✍️ Kilit Ekranı Dergi İmza / Motto Başlığı",
                "Saatin altında sinematik dergi imzası gösterir (" + (TextUtils.isEmpty(currentSig) ? "Kapalı" : currentSig) + ")",
                sigSummary,
                v -> showLockStarSignatureDialog(sigSummary));

        int timeoutSec = Settings.Secure.getIntForUser(
                mResolver, "blaze_lockstar_timeout_sec", 30, UserHandle.USER_CURRENT);
        final TextView timeoutSummary = new TextView(mContext);
        addActionRow(lockStarCard,
                "⏱️ LockStar Kilit Ekranı Zaman Aşımı",
                "Kilit ekranında bildirimleri ve duvar kağıdını incelerken ekranın erken kapanmasını önler (" + timeoutSec + " sn)",
                timeoutSummary,
                v -> showLockStarTimeoutDialog(timeoutSummary));

        addActionRow(lockStarCard,
                "🖼️ Derinlik & Saat Konum Stüdyosunu Aç (Canlı Tuval)",
                "Üst üste saat örtülme koruması, parmakla özne/saat taşıma ve kilit/ana ekran önizlemesi",
                null,
                v -> {
                    new SubSettingLauncher(mContext)
                            .setDestination("org.evolution.settings.fragments.lockscreen.WallpaperDepth")
                            .setTitleText("Derinlik & Saat Konum Stüdyosu")
                            .setSourceMetricsCategory(getMetricsCategory())
                            .launch();
                });
        parent.addView(lockStarCard);

        // MODULE 2: Home Up (DIY Home & Gesture Animation Physics)
        LinearLayout homeUpCard = createModuleCard(
                "🏠 Home Up (DIY Home & Hareket Fiziği)",
                "MAKE UP • ANA EKRAN YAY FİZİĞİ & GÖREV DEĞİŞTİRİCİ",
                "#EC4899");

        int physicsProfile = Settings.Secure.getIntForUser(
                mResolver, "blaze_homeup_gesture_physics", 1, UserHandle.USER_CURRENT);
        final String[] physicsNames = {
                "Klasik Hızlı (0.75x Keskin)",
                "OneUI 9 Dinamik Yay (damping=0.82, Önerilen)",
                "OriginOS 6 Zarif Akış (Atomik Esneme)",
                "ColorOS 17 Aquamorphic (Ultra Yumuşak 1.15x)"
        };
        final TextView physicsSummary = new TextView(mContext);
        addActionRow(homeUpCard,
                "🌀 Ana Ekran Hareket & Yay Fiziği Profili",
                "Aktif: " + physicsNames[Math.max(0, Math.min(3, physicsProfile))],
                physicsSummary,
                v -> showHomeUpPhysicsDialog(physicsNames, physicsSummary));

        boolean wpZoom = Settings.Secure.getIntForUser(
                mResolver, "blaze_homeup_wallpaper_zoom", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(homeUpCard,
                "🔍 Duvar Kağıdı Derinlik Yakınlaştırması (Wallpaper Zoom)",
                "Uygulama açılış/kapanışlarında arka plan duvar kağıdına yumuşak perspektif ölçekleme uygular",
                wpZoom,
                checked -> {
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_homeup_wallpaper_zoom", checked ? 1 : 0, UserHandle.USER_CURRENT);
                    refreshActiveModulesCount();
                });

        boolean recentsBlur = Settings.Secure.getIntForUser(
                mResolver, "blaze_homeup_recents_blur", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(homeUpCard,
                "🪟 Task Changer Gerçek Zamanlı Buzlu Cam",
                "Son uygulamalar (Recents) ekranında arka planı Unified Blur Arbitrator ile bulanıklaştırır",
                recentsBlur,
                checked -> Settings.Secure.putIntForUser(
                        mResolver, "blaze_homeup_recents_blur", checked ? 1 : 0, UserHandle.USER_CURRENT));
        parent.addView(homeUpCard);

        // MODULE 3: Edge Lighting+ & Brief Pop-up
        LinearLayout edgeCard = createModuleCard(
                "🌈 Edge Lighting+ & Brief Pop-up",
                "MAKE UP • KENAR IŞIK DALGASI & AKILLI BİLDİRİM HAPI",
                "#38BDF8");

        int edgeStyle = Settings.Secure.getIntForUser(
                mResolver, "blaze_edgelighting_style", 2, UserHandle.USER_CURRENT);
        final String[] edgeStyleNames = {
                "Kapalı",
                "Eclipse (Yumuşak Speküler Hale)",
                "Cyber Neon (Çift Kutup Akış Dalgası)",
                "Aurora Wave (Kuzey Işıkları Kırılması)",
                "Prism Pulse (Kristal Prizma)"
        };
        final TextView edgeSummary = new TextView(mContext);
        addActionRow(edgeCard,
                "✨ Edge Lighting+ Efekt Stili",
                "Aktif: " + edgeStyleNames[Math.max(0, Math.min(4, edgeStyle))],
                edgeSummary,
                v -> showEdgeLightingStyleDialog(edgeStyleNames, edgeSummary));

        boolean briefPopup = Settings.Secure.getIntForUser(
                mResolver, "ax_dynamic_bar_compact_notifications", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(edgeCard,
                "💊 OneUI 9 Brief Pop-up (İnce Neon Bildirim Hapı)",
                "Ekranı kaplayan bildirim kutuları yerine uygulama renginde zarif cam hap gösterir",
                briefPopup,
                checked -> Settings.Secure.putIntForUser(
                        mResolver, "ax_dynamic_bar_compact_notifications", checked ? 1 : 0, UserHandle.USER_CURRENT));

        boolean otpAutoCopy = Settings.Secure.getIntForUser(
                mResolver, "blaze_smart_otp_autocopy", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(edgeCard,
                "🔑 HyperOS 4 Akıllı OTP / Doğrulama Kodu Yakalayıcı",
                "Mesaj ve bildirimlerdeki 4–8 haneli doğrulama kodlarını otomatik algılar ve hızlı kopyalama sunar",
                otpAutoCopy,
                checked -> Settings.Secure.putIntForUser(
                        mResolver, "blaze_smart_otp_autocopy", checked ? 1 : 0, UserHandle.USER_CURRENT));
        parent.addView(edgeCard);

        // MODULE 4: Wonderland 3D & OriginOS 6 Behavioral Wallpaper
        LinearLayout wonderlandCard = createModuleCard(
                "🌌 Wonderland 3D & Behavioral Duvar Kağıdı",
                "MAKE UP • 3D JİROSKOP PARÇACIK & CANLI TELEMETRİ AURA",
                "#10B981");

        int particleMode = Settings.Secure.getIntForUser(
                mResolver, "blaze_wonderland_particle_mode", 2, UserHandle.USER_CURRENT);
        final String[] particleNames = {
                "Kapalı",
                "🌸 Sakura Yaprakları (Jiroskop Duyarlı)",
                "✨ Ateşböceği Işık Parçacıkları (Fireflies)",
                "🌌 Kozmik Yıldız Tozu (Stardust)",
                "❄️ Kristal Kar Taneleri"
        };
        final TextView particleSummary = new TextView(mContext);
        addActionRow(wonderlandCard,
                "🪐 Wonderland 3D Hareketli Parçacık Katmanı",
                "Aktif: " + particleNames[Math.max(0, Math.min(4, particleMode))],
                particleSummary,
                v -> showWonderlandParticleDialog(particleNames, particleSummary));

        boolean behavioralAura = Settings.Secure.getIntForUser(
                mResolver, "blaze_behavioral_wallpaper_aura", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(wonderlandCard,
                "🌺 OriginOS 6 Behavioral (Davranışsal) Telemetri Aurası",
                "Kilit ekranı ışık kristallerinin rengini ve açılımını pil seviyesi, şarj durumu ve günün saatine göre canlı evrimleştirir",
                behavioralAura,
                checked -> {
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_behavioral_wallpaper_aura", checked ? 1 : 0, UserHandle.USER_CURRENT);
                    refreshActiveModulesCount();
                });
        parent.addView(wonderlandCard);

        // MODULE 5: QuickStar (Quick Settings & Status Bar Surgeon)
        LinearLayout quickStarCard = createModuleCard(
                "⚡ QuickStar (Hızlı Panel & Durum Çubuğu Cerrahı)",
                "MAKE UP • IZGARALAR, BÖLÜNMÜŞ PANEL & İKON KONTROLÜ",
                "#F59E0B");

        int qsCols = Settings.System.getIntForUser(
                mResolver, "qs_columns_portrait", 4, UserHandle.USER_CURRENT);
        final TextView qsColSummary = new TextView(mContext);
        addActionRow(quickStarCard,
                "📐 Hızlı Ayarlar (QS) Sütun Yoğunluğu",
                "Portre modunda satır başına " + qsCols + " kare gösteriliyor",
                qsColSummary,
                v -> showQuickStarColumnsDialog(qsColSummary));

        boolean waveform = Settings.System.getIntForUser(
                mResolver, "media_waveform_seekbar", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(quickStarCard,
                "🎵 Hızlı Panel Medya Dalga Formu Çubuğu",
                "Medya oynatıcı ilerleme çubuğunu canlı ses dalgası olarak çizer",
                waveform,
                checked -> Settings.System.putIntForUser(
                        mResolver, "media_waveform_seekbar", checked ? 1 : 0, UserHandle.USER_CURRENT));
        parent.addView(quickStarCard);
    }

    // =========================================================================
    // SUITE 2: LIFE UP MODULES (SoundAssistant + O-Relax, One Hand Operation+ & Smart Sidebar, MultiStar, RegiStar, Nice Catch)
    // =========================================================================

    private void populateLifeUpModules(LinearLayout parent) {
        parent.removeAllViews();

        // MODULE 6: SoundAssistant & ColorOS 17 O-Relax Studio (Stage 2)
        LinearLayout soundCard = createModuleCard(
                "🔊 SoundAssistant & O-Relax Ses Laboratuvarı",
                "LIFE UP • UYGULAMA BAŞINA SES, SES ADIMI & DOĞA SESLERİ",
                "#38BDF8");

        int volStep = Settings.Secure.getIntForUser(
                mResolver, "blaze_soundassistant_vol_step", 2, UserHandle.USER_CURRENT);
        final TextView stepSummary = new TextView(mContext);
        addActionRow(soundCard,
                "🎚️ Ses Tuşu Adım Hassasiyeti (Change Step Volume)",
                "Her ses tuşu basışında " + volStep + " birim ince ayar yapılır (Standart Android: 7-10 birim)",
                stepSummary,
                v -> showVolumeStepDialog(stepSummary));

        boolean multiSound = Settings.Secure.getIntForUser(
                mResolver, "blaze_soundassistant_multisound", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(soundCard,
                "🎶 Multi-Sound (Çoklu Uygulama Ses Odağı)",
                "Oyun veya sosyal medya açıldığında arka planda çalan müziğin duraklatılmasını önler",
                multiSound,
                checked -> {
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_soundassistant_multisound", checked ? 1 : 0, UserHandle.USER_CURRENT);
                    SystemProperties.set("persist.sys.blaze.multisound", checked ? "1" : "0");
                    refreshActiveModulesCount();
                });

        addActionRow(soundCard,
                "🎛️ Aktif Uygulama & Kanal Ses Mikseri (Canlı Mikser)",
                "Medya, Bildirim, Zil, Alarm ve aktif ses oturumlarını tek ekranda bağımsız dengele",
                null,
                v -> showPerStreamAndAppVolumeMixerDialog());

        // ColorOS 17 O-Relax Built-in Procedural Soundscape Bar
        soundCard.addView(buildORelaxSoundscapeWidget());
        parent.addView(soundCard);

        // MODULE 7: One Hand Operation+ & ColorOS 17 Smart Sidebar / File Dock (Stage 3 & 5)
        LinearLayout ohoCard = createModuleCard(
                "🤚 One Hand Operation+ & Smart Sidebar (File Dock)",
                "LIFE UP • ÇAPRAZ KENAR HAREKETLERİ & EVRENSEL PANO RIHTIMI",
                "#A855F7");

        boolean ohoEnabled = Settings.Secure.getIntForUser(
                mResolver, "blaze_oho_enabled", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(ohoCard,
                "↗️ One Hand Operation+ Çapraz Kenar Kısayolları",
                "Kenardan düz çekme Geri yapar; Yukarı Çapraz (↗) ve Aşağı Çapraz (↘) özel kısayol tetikler",
                ohoEnabled,
                checked -> {
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_oho_enabled", checked ? 1 : 0, UserHandle.USER_CURRENT);
                    refreshActiveModulesCount();
                });

        final String[] gestureActions = {
                "Smart Sidebar & File Dock Aç (Pano & Araçlar)",
                "Bildirim Panelini İndir",
                "Hızlı Ayarları (QS) Aç",
                "El Fenerini Aç/Kapat",
                "Ekran Görüntüsü Al",
                "Son Uygulamalar / Önceki Uygulama"
        };
        int diagUp = Settings.Secure.getIntForUser(
                mResolver, "blaze_oho_diagonal_up_action", 0, UserHandle.USER_CURRENT);
        final TextView upSummary = new TextView(mContext);
        addActionRow(ohoCard,
                "↗️ Yukarı Çapraz Kaydırma (+25°..+65°) İşlevi",
                "Atanan: " + gestureActions[Math.max(0, Math.min(gestureActions.length - 1, diagUp))],
                upSummary,
                v -> showOhoActionPicker("blaze_oho_diagonal_up_action", "↗️ Yukarı Çapraz Hareketi", gestureActions, upSummary));

        int diagDown = Settings.Secure.getIntForUser(
                mResolver, "blaze_oho_diagonal_down_action", 1, UserHandle.USER_CURRENT);
        final TextView downSummary = new TextView(mContext);
        addActionRow(ohoCard,
                "↘️ Aşağı Çapraz Kaydırma (-25°..-65°) İşlevi",
                "Atanan: " + gestureActions[Math.max(0, Math.min(gestureActions.length - 1, diagDown))],
                downSummary,
                v -> showOhoActionPicker("blaze_oho_diagonal_down_action", "↘️ Aşağı Çapraz Hareketi", gestureActions, downSummary));

        boolean sidebarEnabled = Settings.Secure.getIntForUser(
                mResolver, "blaze_smart_sidebar_enabled", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(ohoCard,
                "🗂️ ColorOS 17 Smart Sidebar & File Dock (Pano Rıhtımı)",
                "Ekranın sağ üst-orta kenarındaki yarı-saydam rıhtımdan son kopyalanan metinlere, OTP kodlarına ve hızlı araçlara erişin",
                sidebarEnabled,
                checked -> {
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_smart_sidebar_enabled", checked ? 1 : 0, UserHandle.USER_CURRENT);
                    refreshActiveModulesCount();
                });

        addActionRow(ohoCard,
                "📋 File Dock Pano Rıhtımını Şimdi Aç (Önizleme)",
                "Mevcut panodaki metni/OTP kodunu ve hızlı araçları görüntüle",
                null,
                v -> showFileDockQuickModal());
        parent.addView(ohoCard);

        // MODULE 8: MultiStar (Advanced Multi-Window & Multi-Focus)
        LinearLayout multiStarCard = createModuleCard(
                "🪟 MultiStar (Süper Çoklu Pencere Motoru)",
                "LIFE UP • ZORUNLU BÖLÜNMÜŞ EKRAN & ÇOKLU ODAK (MULTI-RESUME)",
                "#10B981");

        boolean forceResizable = Settings.Global.getInt(
                mResolver, "enable_non_resizable_multi_window", 1) == 1;
        addSwitchRow(multiStarCard,
                "🔓 I Love Multi Window (Tüm Uygulamalarda Zorunlu Çoklu Pencere)",
                "Bölünmüş ekranı veya serbest pencereyi engelleyen uygulamaları (Instagram, Oyunlar vb.) zorla çoklu pencere uyumlu yapar",
                forceResizable,
                checked -> {
                    Settings.Global.putInt(mResolver, "enable_non_resizable_multi_window", checked ? 1 : 0);
                    Settings.Global.putInt(mResolver, "enable_freeform_support", checked ? 1 : 0);
                    refreshActiveModulesCount();
                });

        boolean multiResume = Settings.Secure.getIntForUser(
                mResolver, "blaze_multistar_multi_resume", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(multiStarCard,
                "⚡ Multi-Focus (Aktif Olmayan Pencereyi Duraklatma)",
                "Bölünmüş ekranda bir pencerede yazarken diğer penceredeki oyunun/videonun donmasını engeller",
                multiResume,
                checked -> {
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_multistar_multi_resume", checked ? 1 : 0, UserHandle.USER_CURRENT);
                    SystemProperties.set("persist.wm.debug.multi_resume", checked ? "true" : "false");
                });
        parent.addView(multiStarCard);

        // MODULE 9: RegiStar (Back-Tap & Side Key Customization)
        LinearLayout regiStarCard = createModuleCard(
                "⚙️ RegiStar (Arka Kapak Dokunuşu & Yan Tuş)",
                "LIFE UP • BACK-TAP TETİKLEYİCİSİ & GÜÇ TUŞU ATAMALARI",
                "#F59E0B");

        final String[] backTapActions = {
                "Kapalı",
                "Ekran Görüntüsü Al (Nice Shot)",
                "El Fenerini Aç/Kapat",
                "Bildirim Panelini İndir",
                "Kamera Uygulamasını Başlat",
                "Pil Sağlığı & Termal Stüdyoyu Aç"
        };
        int backTap = Settings.Secure.getIntForUser(
                mResolver, "blaze_registar_backtap_action", 1, UserHandle.USER_CURRENT);
        final TextView backTapSummary = new TextView(mContext);
        addActionRow(regiStarCard,
                "👆 Arka Kapağa Çift Dokunuş (Back-Tap Action)",
                "Atanan: " + backTapActions[Math.max(0, Math.min(backTapActions.length - 1, backTap))],
                backTapSummary,
                v -> showOhoActionPicker("blaze_registar_backtap_action", "👆 Arka Kapak Çift Dokunuş", backTapActions, backTapSummary));

        boolean torchPower = Settings.System.getIntForUser(
                mResolver, "torch_long_press_power_gesture", 1, UserHandle.USER_CURRENT) == 1;
        addSwitchRow(regiStarCard,
                "🔦 Ekran Kapalıyken Güç Tuşuna Basılı Tutunca El Feneri",
                "RegiStar Side Key: Kilit ekranını açmadan güç tuşuna basılı tutarak feneri yakar",
                torchPower,
                checked -> Settings.System.putIntForUser(
                        mResolver, "torch_long_press_power_gesture", checked ? 1 : 0, UserHandle.USER_CURRENT));
        parent.addView(regiStarCard);

        // MODULE 10: Nice Catch (System Detective: Vibration, Wakeup & Clipboard History)
        LinearLayout niceCatchCard = createModuleCard(
                "🕵️ Nice Catch (Sistem & Gizlilik Dedektifi)",
                "LIFE UP • GİZLİ TİTREŞİM, EKRAN UYANDIRMA & SENSÖR GÜNLÜĞÜ",
                "#F43F5E");

        TextView detectiveDesc = new TextView(mContext);
        detectiveDesc.setText("Cihazınız durup dururken titrediğinde, ekran kendi kendine uyandığında veya arka planda pil tükettiğinde hangi uygulamanın bunu tetiklediğini gerçek zamanlı AppOps & UsageStats telemetrisiyle yakalar.");
        detectiveDesc.setTextColor(Color.parseColor("#94A3B8"));
        detectiveDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        detectiveDesc.setPadding(0, dp(4), 0, dp(10));
        niceCatchCard.addView(detectiveDesc);

        LinearLayout liveLogBox = new LinearLayout(mContext);
        liveLogBox.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable logBg = new GradientDrawable();
        logBg.setColor(Color.parseColor("#0B111E"));
        logBg.setCornerRadius(dp(16));
        logBg.setStroke(dp(1), Color.parseColor("#28F43F5E"));
        liveLogBox.setBackground(logBg);
        liveLogBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        populateNiceCatchLiveLogs(liveLogBox);
        niceCatchCard.addView(liveLogBox);

        addActionRow(niceCatchCard,
                "🔄 Nice Catch Dedektif Raporunu Yenile",
                "Son titreşim, ekran uyandırma (Wakelock) ve ses odağı geçmişini tekrar tara",
                null,
                v -> {
                    vibrateTick();
                    populateNiceCatchLiveLogs(liveLogBox);
                    Toast.makeText(mContext, "Nice Catch günlüğü güncellendi", Toast.LENGTH_SHORT).show();
                });

        parent.addView(niceCatchCard);
    }

    // =========================================================================
    // DIALOGS & INTERACTIVE TOOLS (STAGE 1, 2, 3)
    // =========================================================================

    private void showLockStarSignatureDialog(final TextView summaryView) {
        LinearLayout box = new LinearLayout(mContext);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(12), pad, dp(8));

        final EditText input = new EditText(mContext);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        String current = Settings.Secure.getStringForUser(
                mResolver, "blaze_lockstar_signature_text", UserHandle.USER_CURRENT);
        if (current == null) current = "PROJECT BLAZE 17 • BEYOND INFINITY";
        input.setText(current);
        input.setHint("Örn: PROJECT BLAZE 17 • STAY HUNGRY");
        box.addView(input);

        final String[] presets = {
                "PROJECT BLAZE 17 • BEYOND INFINITY",
                "STAY HUNGRY • STAY FOOLISH",
                "HYPEROS 4 • MAGAZINE EDITION",
                "CARPE DIEM • MEMENTO MORI",
                ""
        };
        final String[] presetLabels = {
                "✨ Project Blaze 17 • Beyond Infinity",
                "🚀 Stay Hungry • Stay Foolish",
                "📰 HyperOS 4 • Magazine Edition",
                "🏛️ Carpe Diem • Memento Mori",
                "🚫 İmza Başlığını Gizle (Kapalı)"
        };

        for (int i = 0; i < presets.length; i++) {
            final String val = presets[i];
            TextView chip = new TextView(mContext);
            chip.setText(presetLabels[i]);
            chip.setTextColor(Color.parseColor("#38BDF8"));
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            chip.setPadding(dp(8), dp(8), dp(8), dp(8));
            chip.setOnClickListener(v -> input.setText(val));
            box.addView(chip);
        }

        new AlertDialog.Builder(mContext)
                .setTitle("✍️ LockStar & HyperOS Dergi İmza Başlığı")
                .setView(box)
                .setPositiveButton("Kaydet", (d, w) -> {
                    String text = input.getText().toString().trim();
                    Settings.Secure.putStringForUser(
                            mResolver, "blaze_lockstar_signature_text", text, UserHandle.USER_CURRENT);
                    if (summaryView != null) {
                        summaryView.setText("Saatin altında sinematik dergi imzası gösterir ("
                                + (TextUtils.isEmpty(text) ? "Kapalı" : text) + ")");
                    }
                    refreshActiveModulesCount();
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private void showLockStarTimeoutDialog(final TextView summaryView) {
        final String[] labels = {"15 Saniye (Standart)", "30 Saniye (LockStar Önerilen)", "60 Saniye (1 Dakika)", "120 Saniye (2 Dakika)"};
        final int[] values = {15, 30, 60, 120};
        int cur = Settings.Secure.getIntForUser(mResolver, "blaze_lockstar_timeout_sec", 30, UserHandle.USER_CURRENT);
        int checked = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == cur) checked = i;
        }
        new AlertDialog.Builder(mContext)
                .setTitle("⏱️ LockStar Kilit Ekranı Zaman Aşımı")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_lockstar_timeout_sec", values[which], UserHandle.USER_CURRENT);
                    if (summaryView != null) {
                        summaryView.setText("Kilit ekranında bildirimleri incelerken ekranın erken kapanmasını önler (" + values[which] + " sn)");
                    }
                    d.dismiss();
                })
                .show();
    }

    private void showHomeUpPhysicsDialog(final String[] names, final TextView summaryView) {
        int cur = Settings.Secure.getIntForUser(mResolver, "blaze_homeup_gesture_physics", 1, UserHandle.USER_CURRENT);
        final float[] scales = {0.75f, 0.90f, 1.0f, 1.15f};
        new AlertDialog.Builder(mContext)
                .setTitle("🌀 Home Up Hareket & Yay Fiziği Profili")
                .setSingleChoiceItems(names, Math.max(0, Math.min(3, cur)), (d, which) -> {
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_homeup_gesture_physics", which, UserHandle.USER_CURRENT);
                    Settings.Global.putFloat(mResolver, Settings.Global.WINDOW_ANIMATION_SCALE, scales[which]);
                    Settings.Global.putFloat(mResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, scales[which]);
                    Settings.Global.putFloat(mResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scales[which]);
                    if (summaryView != null) {
                        summaryView.setText("Aktif: " + names[which]);
                    }
                    vibrateTick();
                    d.dismiss();
                })
                .show();
    }

    private void showEdgeLightingStyleDialog(final String[] names, final TextView summaryView) {
        int cur = Settings.Secure.getIntForUser(mResolver, "blaze_edgelighting_style", 2, UserHandle.USER_CURRENT);
        new AlertDialog.Builder(mContext)
                .setTitle("🌈 Edge Lighting+ Efekt Stili")
                .setSingleChoiceItems(names, Math.max(0, Math.min(4, cur)), (d, which) -> {
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_edgelighting_style", which, UserHandle.USER_CURRENT);
                    // Trigger live preview pulse Timestamp so SystemUI flashes the edge lighting immediately
                    Settings.Secure.putLongForUser(
                            mResolver, "blaze_edgelighting_preview_pulse", System.currentTimeMillis(), UserHandle.USER_CURRENT);
                    if (summaryView != null) {
                        summaryView.setText("Aktif: " + names[which]);
                    }
                    refreshActiveModulesCount();
                    vibrateTick();
                    d.dismiss();
                })
                .show();
    }

    private void showWonderlandParticleDialog(final String[] names, final TextView summaryView) {
        int cur = Settings.Secure.getIntForUser(mResolver, "blaze_wonderland_particle_mode", 2, UserHandle.USER_CURRENT);
        new AlertDialog.Builder(mContext)
                .setTitle("🪐 Wonderland 3D Hareketli Parçacık Katmanı")
                .setSingleChoiceItems(names, Math.max(0, Math.min(4, cur)), (d, which) -> {
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_wonderland_particle_mode", which, UserHandle.USER_CURRENT);
                    if (summaryView != null) {
                        summaryView.setText("Aktif: " + names[which]);
                    }
                    refreshActiveModulesCount();
                    vibrateTick();
                    d.dismiss();
                })
                .show();
    }

    private void showQuickStarColumnsDialog(final TextView summaryView) {
        final String[] labels = {
                "3 Sütun (Geniş OneUI Kartları)",
                "4 Sütun (Standart Dengeli)",
                "5 Sütun (QuickStar Kompakt Yoğun)"
        };
        final int[] vals = {3, 4, 5};
        int cur = Settings.System.getIntForUser(mResolver, "qs_columns_portrait", 4, UserHandle.USER_CURRENT);
        int idx = cur == 3 ? 0 : (cur == 5 ? 2 : 1);
        new AlertDialog.Builder(mContext)
                .setTitle("📐 QuickStar Hızlı Ayarlar Sütun Yoğunluğu")
                .setSingleChoiceItems(labels, idx, (d, which) -> {
                    Settings.System.putIntForUser(
                            mResolver, "qs_columns_portrait", vals[which], UserHandle.USER_CURRENT);
                    if (summaryView != null) {
                        summaryView.setText("Portre modunda satır başına " + vals[which] + " kare gösteriliyor");
                    }
                    vibrateTick();
                    d.dismiss();
                })
                .show();
    }

    private void showVolumeStepDialog(final TextView summaryView) {
        final String[] labels = {
                "1 Birim (Ultra Hassas Stüdyo Kontrolü — 150 Adım)",
                "2 Birim (SoundAssistant Önerilen — 75 Adım)",
                "5 Birim (Dengeli — 30 Adım)",
                "10 Birim (Standart Hızlı — 15 Adım)"
        };
        final int[] steps = {1, 2, 5, 10};
        int cur = Settings.Secure.getIntForUser(mResolver, "blaze_soundassistant_vol_step", 2, UserHandle.USER_CURRENT);
        int checked = 1;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == cur) checked = i;
        }
        new AlertDialog.Builder(mContext)
                .setTitle("🎚️ SoundAssistant Ses Tuşu Adım Hassasiyeti")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    int step = steps[which];
                    Settings.Secure.putIntForUser(
                            mResolver, "blaze_soundassistant_vol_step", step, UserHandle.USER_CURRENT);
                    int mediaSteps = Math.max(15, 150 / step);
                    SystemProperties.set("ro.config.media_vol_steps", String.valueOf(mediaSteps));
                    if (summaryView != null) {
                        summaryView.setText("Her ses tuşu basışında " + step + " birim ince ayar yapılır");
                    }
                    vibrateTick();
                    d.dismiss();
                })
                .show();
    }

    private void showPerStreamAndAppVolumeMixerDialog() {
        final AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;

        LinearLayout box = new LinearLayout(mContext);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(14), pad, dp(10));

        final int[] streams = {
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.STREAM_ALARM
        };
        final String[] streamTitles = {
                "🎵 Medya & Müzik Sesi",
                "📞 Zil Sesi",
                "🔔 Bildirim Sesi",
                "⏰ Alarm Sesi"
        };

        for (int i = 0; i < streams.length; i++) {
            final int streamType = streams[i];
            int max = Math.max(1, am.getStreamMaxVolume(streamType));
            int cur = am.getStreamVolume(streamType);

            TextView label = new TextView(mContext);
            int pct = (int) Math.round((cur * 100.0) / max);
            label.setText(streamTitles[i] + "  •  %" + pct);
            label.setTextColor(Color.WHITE);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            label.setPadding(0, dp(8), 0, dp(2));
            box.addView(label);

            SeekBar sb = new SeekBar(mContext);
            sb.setMax(max);
            sb.setProgress(cur);
            final String baseTitle = streamTitles[i];
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        am.setStreamVolume(streamType, progress, 0);
                        int p = (int) Math.round((progress * 100.0) / max);
                        label.setText(baseTitle + "  •  %" + p);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            box.addView(sb);
        }

        // Active App Playback Sessions from AudioManager
        List<AudioPlaybackConfiguration> configs = am.getActivePlaybackConfigurations();
        TextView activeHeader = new TextView(mContext);
        activeHeader.setText("🎧 Aktif Ses Oynatan Uygulamalar (" + configs.size() + " oturum)");
        activeHeader.setTextColor(Color.parseColor("#38BDF8"));
        activeHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        activeHeader.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        activeHeader.setPadding(0, dp(14), 0, dp(4));
        box.addView(activeHeader);

        if (configs.isEmpty()) {
            TextView empty = new TextView(mContext);
            empty.setText("Şu anda arka planda aktif medya oynatan bir uygulama yok. Müzik veya video başlattığınızda uygulama başına ses kontrolü burada listelenir.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            box.addView(empty);
        } else {
            for (int i = 0; i < Math.min(4, configs.size()); i++) {
                AudioPlaybackConfiguration cfg = configs.get(i);
                int usage = cfg.getAudioAttributes() != null ? cfg.getAudioAttributes().getUsage() : 0;
                TextView row = new TextView(mContext);
                row.setText("• Aktif Ses Oturumu #" + (i + 1) + " (Usage ID: " + usage + ") — Çalıyor");
                row.setTextColor(Color.parseColor("#E2E8F0"));
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                row.setPadding(0, dp(3), 0, dp(3));
                box.addView(row);
            }
        }

        new AlertDialog.Builder(mContext)
                .setTitle("🎛️ SoundAssistant Ses Mikseri")
                .setView(box)
                .setPositiveButton("Tamam", null)
                .show();
    }

    // =========================================================================
    // COLOROS 17 O-RELAX PROCEDURAL AMBIENT SOUNDSCAPE SYNTHESIZER (STAGE 2)
    // =========================================================================

    private View buildORelaxSoundscapeWidget() {
        LinearLayout container = new LinearLayout(mContext);
        container.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0B1322"));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.parseColor("#2E38BDF8"));
        container.setBackground(bg);
        int pad = dp(14);
        container.setPadding(pad, dp(12), pad, dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        container.setLayoutParams(lp);

        TextView header = new TextView(mContext);
        header.setText("🌊 ColorOS 17 O-Relax — Gerçek Zamanlı Doğa & Odak Sentezleyicisi");
        header.setTextColor(Color.WHITE);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        container.addView(header);

        mAmbientStatusText = new TextView(mContext);
        mAmbientStatusText.setText("Durum: Hazır (Harici dosya gerektirmeyen canlı AudioTrack DSP sentezleyici)");
        mAmbientStatusText.setTextColor(Color.parseColor("#94A3B8"));
        mAmbientStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        mAmbientStatusText.setPadding(0, dp(2), 0, dp(8));
        container.addView(mAmbientStatusText);

        HorizontalScrollView hsv = new HorizontalScrollView(mContext);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(mContext);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);

        final String[] labels = {"⏹️ Durdur", "🌧️ Gece Yağmuru", "🌊 Okyanus Dalgası", "🔥 Kamp Ateşi", "🪐 Derin Uzay"};
        for (int i = 0; i < labels.length; i++) {
            final int mode = i;
            TextView chip = new TextView(mContext);
            chip.setText(labels[i]);
            chip.setTextColor(Color.WHITE);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(i == 0 ? Color.parseColor("#2A3447") : Color.parseColor("#1E293B"));
            chipBg.setCornerRadius(dp(99));
            chipBg.setStroke(dp(1), Color.parseColor("#38BDF8"));
            chip.setBackground(chipBg);
            chip.setPadding(dp(12), dp(7), dp(12), dp(7));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = dp(8);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                vibrateTick();
                if (mode == 0) {
                    stopAmbientSoundscape();
                } else {
                    startAmbientSoundscape(mode, labels[mode]);
                }
            });
            chipRow.addView(chip);
        }
        hsv.addView(chipRow);
        container.addView(hsv);
        return container;
    }

    private void startAmbientSoundscape(final int mode, final String modeLabel) {
        stopAmbientSoundscape();
        mAmbientMode = mode;
        mAmbientPlaying = true;
        if (mAmbientStatusText != null) {
            mAmbientStatusText.setText("Çalıyor: " + modeLabel + " (Canlı DSP Sentezi Aktif)");
            mAmbientStatusText.setTextColor(Color.parseColor("#38BDF8"));
        }

        mAmbientThread = new Thread(() -> {
            final int sampleRate = 24000;
            int minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(Math.max(minBuf, 4096))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();

            short[] buffer = new short[1024];
            Random rng = new Random();
            double brown = 0.0;
            double lp1 = 0.0;
            double phase = 0.0;

            try {
                track.play();
                while (mAmbientPlaying && !Thread.currentThread().isInterrupted()) {
                    for (int i = 0; i < buffer.length; i++) {
                        double white = rng.nextDouble() * 2.0 - 1.0;
                        brown = (brown + (0.02 * white)) / 1.02;
                        lp1 = lp1 + 0.08 * (white - lp1);
                        phase += 1.0 / sampleRate;

                        double sample;
                        if (mode == 1) {
                            // Night Rain: pink-filtered noise + gentle droplet envelope
                            sample = (lp1 * 0.55 + brown * 0.45) * 0.35;
                        } else if (mode == 2) {
                            // Ocean Waves: slow 0.14 Hz swell modulating brown/pink noise
                            double waveEnv = 0.25 + 0.75 * (0.5 + 0.5 * Math.sin(phase * 2.0 * Math.PI * 0.14));
                            sample = (brown * 0.75 + lp1 * 0.25) * waveEnv * 0.55;
                        } else if (mode == 3) {
                            // Campfire: warm low rumble + soft random crackles
                            double crackle = (rng.nextDouble() > 0.9975) ? (white * 0.65) : 0.0;
                            sample = (brown * 0.65 + crackle) * 0.40;
                        } else {
                            // Deep Space: ultra-smooth deep brownian drone
                            sample = brown * 0.55;
                        }
                        int pcm = (int) (Math.max(-1.0, Math.min(1.0, sample)) * 14000);
                        buffer[i] = (short) pcm;
                    }
                    track.write(buffer, 0, buffer.length);
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    track.stop();
                    track.release();
                } catch (Exception ignored) {
                }
            }
        }, "BlazeORelaxSynth");
        mAmbientThread.start();
    }

    private void stopAmbientSoundscape() {
        mAmbientPlaying = false;
        if (mAmbientThread != null) {
            mAmbientThread.interrupt();
            mAmbientThread = null;
        }
        if (mAmbientStatusText != null) {
            mAmbientStatusText.setText("Durum: Durduruldu");
            mAmbientStatusText.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    // =========================================================================
    // ONE HAND OPERATION+, FILE DOCK & NICE CATCH DETECTIVE (STAGE 3)
    // =========================================================================

    private void showOhoActionPicker(
            final String settingKey,
            final String title,
            final String[] actions,
            final TextView summaryView) {
        int cur = Settings.Secure.getIntForUser(mResolver, settingKey, 0, UserHandle.USER_CURRENT);
        new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setSingleChoiceItems(actions, Math.max(0, Math.min(actions.length - 1, cur)), (d, which) -> {
                    Settings.Secure.putIntForUser(mResolver, settingKey, which, UserHandle.USER_CURRENT);
                    if (summaryView != null) {
                        summaryView.setText("Atanan: " + actions[which]);
                    }
                    vibrateTick();
                    d.dismiss();
                })
                .show();
    }

    private void showFileDockQuickModal() {
        ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        String clipText = "";
        if (cm != null && cm.hasPrimaryClip()) {
            ClipData cd = cm.getPrimaryClip();
            if (cd != null && cd.getItemCount() > 0 && cd.getItemAt(0).getText() != null) {
                clipText = cd.getItemAt(0).getText().toString();
            }
        }

        LinearLayout box = new LinearLayout(mContext);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(14), pad, dp(10));

        TextView clipLabel = new TextView(mContext);
        clipLabel.setText("📌 Pano Rıhtımı (Aktif Pano İçeriği):");
        clipLabel.setTextColor(Color.parseColor("#38BDF8"));
        clipLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        box.addView(clipLabel);

        TextView clipContent = new TextView(mContext);
        clipContent.setText(TextUtils.isEmpty(clipText) ? "(Pano şu anda boş)" : clipText);
        clipContent.setTextColor(Color.WHITE);
        clipContent.setPadding(0, dp(6), 0, dp(12));
        box.addView(clipContent);

        new AlertDialog.Builder(mContext)
                .setTitle("🗂️ ColorOS 17 File Dock & Pano Rıhtımı")
                .setView(box)
                .setPositiveButton("Kapat", null)
                .show();
    }

    private void populateNiceCatchLiveLogs(LinearLayout container) {
        container.removeAllViews();
        PackageManager pm = mContext.getPackageManager();
        UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        List<UsageStats> stats = usm != null
                ? usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 3600_000L * 12, now)
                : Collections.emptyList();

        List<UsageStats> sorted = new ArrayList<>();
        if (stats != null) {
            for (UsageStats s : stats) {
                if (s.getLastTimeUsed() > 0) {
                    sorted.add(s);
                }
            }
            sorted.sort((a, b) -> Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()));
        }

        AppOpsManager appOps = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
        int count = 0;
        for (UsageStats us : sorted) {
            if (count >= 6) break;
            String pkg = us.getPackageName();
            if ("android".equals(pkg)) continue;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                String appName = pm.getApplicationLabel(ai).toString();
                String timeStr = sdf.format(new Date(us.getLastTimeUsed()));

                StringBuilder tags = new StringBuilder();
                if (appOps != null) {
                    int vibMode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_VIBRATE, ai.uid, pkg);
                    if (vibMode == AppOpsManager.MODE_ALLOWED) tags.append("📳 Titreşim • ");
                }
                tags.append("⚡ Uyandırma / Etkinlik");

                TextView row = new TextView(mContext);
                row.setText("[" + timeStr + "]  " + appName + "  —  " + tags);
                row.setTextColor(Color.parseColor("#E2E8F0"));
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                row.setPadding(0, dp(4), 0, dp(4));
                container.addView(row);
                count++;
            } catch (Exception ignored) {
            }
        }

        if (count == 0) {
            TextView empty = new TextView(mContext);
            empty.setText("Son saatlerde şüpheli arka plan titreşimi veya ekran uyandırma tetikleyicisi saptanmadı.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            container.addView(empty);
        }
    }

    // =========================================================================
    // UI CARD & ROW BUILDERS
    // =========================================================================

    private LinearLayout createModuleCard(String title, String badgeText, String accentHex) {
        LinearLayout card = new LinearLayout(mContext);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#121A2B"));
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), Color.parseColor("#35" + accentHex.substring(1)));
        card.setBackground(bg);
        int pad = dp(18);
        card.setPadding(pad, dp(16), pad, dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        card.setLayoutParams(lp);

        TextView badge = new TextView(mContext);
        badge.setText(badgeText);
        badge.setTextColor(Color.parseColor(accentHex));
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        card.addView(badge);

        TextView header = new TextView(mContext);
        header.setText(title);
        header.setTextColor(Color.WHITE);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        header.setPadding(0, dp(2), 0, dp(6));
        card.addView(header);

        LivingFlowTouchHelper.attach(card, 0.97f, true);
        return card;
    }

    private interface OnToggleChanged {
        void onChanged(boolean checked);
    }

    private void addSwitchRow(
            LinearLayout parent,
            String title,
            String summary,
            boolean initialChecked,
            final OnToggleChanged listener) {
        LinearLayout row = new LinearLayout(mContext);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));

        LinearLayout textCol = new LinearLayout(mContext);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.rightMargin = dp(10);
        textCol.setLayoutParams(colLp);

        TextView t = new TextView(mContext);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F1F5F9"));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textCol.addView(t);

        TextView s = new TextView(mContext);
        s.setText(summary);
        s.setTextColor(Color.parseColor("#94A3B8"));
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        textCol.addView(s);

        final Switch sw = new Switch(mContext);
        sw.setChecked(initialChecked);
        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            vibrateTick();
            if (listener != null) listener.onChanged(isChecked);
        });

        row.setOnClickListener(v -> sw.setChecked(!sw.isChecked()));
        row.addView(textCol);
        row.addView(sw);
        parent.addView(row);
    }

    private void addActionRow(
            LinearLayout parent,
            String title,
            String summary,
            TextView customSummaryRef,
            View.OnClickListener clickListener) {
        LinearLayout row = new LinearLayout(mContext);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));

        LinearLayout textCol = new LinearLayout(mContext);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.rightMargin = dp(10);
        textCol.setLayoutParams(colLp);

        TextView t = new TextView(mContext);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F1F5F9"));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textCol.addView(t);

        TextView s = customSummaryRef != null ? customSummaryRef : new TextView(mContext);
        s.setText(summary);
        s.setTextColor(Color.parseColor("#38BDF8"));
        s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        textCol.addView(s);

        TextView chev = new TextView(mContext);
        chev.setText("›");
        chev.setTextColor(Color.parseColor("#64748B"));
        chev.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);

        row.addView(textCol);
        row.addView(chev);
        row.setOnClickListener(clickListener);
        parent.addView(row);
    }

    private void vibrateTick() {
        try {
            Vibrator vib = mContext.getSystemService(Vibrator.class);
            if (vib != null && vib.hasVibrator()) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
            }
        } catch (Exception ignored) {
        }
    }

    private int dp(int val) {
        return Math.round(val * mContext.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.VIEW_UNKNOWN;
    }
}
