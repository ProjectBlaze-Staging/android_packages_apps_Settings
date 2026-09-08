/*
 * Copyright (C) 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.blaze.house;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class BlazeHouse extends SettingsPreferenceFragment {

    private static final String TAG = "BlazeHouse";
    private static final int MENU_INFO = Menu.FIRST + 1;

    public static final int TAB_THEMES = 0;
    public static final int TAB_QS = 1;
    public static final int TAB_STATUSBAR = 2;
    public static final int TAB_LOCKSCREEN = 3;
    public static final int TAB_SYSTEM = 4;

    private int mCurrentTab = TAB_SYSTEM;

    private LinearLayout[] mTabViews = new LinearLayout[5];
    private ImageView[] mTabIcons = new ImageView[5];
    private TextView[] mTabTexts = new TextView[5];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        loadTabPreferences(mCurrentTab);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupFloatingTabs();
    }

    private void setupFloatingTabs() {
        View headerView = setPinnedHeaderView(R.layout.blaze_floating_tabs);
        if (headerView == null) return;

        View pinnedContainer = getActivity().findViewById(R.id.pinned_header);
        if (pinnedContainer != null) {
            pinnedContainer.setBackgroundColor(Color.TRANSPARENT);
        }

        mTabViews[TAB_THEMES] = headerView.findViewById(R.id.tab_themes);
        mTabViews[TAB_QS] = headerView.findViewById(R.id.tab_qs);
        mTabViews[TAB_STATUSBAR] = headerView.findViewById(R.id.tab_statusbar);
        mTabViews[TAB_LOCKSCREEN] = headerView.findViewById(R.id.tab_lockscreen);
        mTabViews[TAB_SYSTEM] = headerView.findViewById(R.id.tab_system);

        mTabIcons[TAB_THEMES] = headerView.findViewById(R.id.icon_themes);
        mTabIcons[TAB_QS] = headerView.findViewById(R.id.icon_qs);
        mTabIcons[TAB_STATUSBAR] = headerView.findViewById(R.id.icon_statusbar);
        mTabIcons[TAB_LOCKSCREEN] = headerView.findViewById(R.id.icon_lockscreen);
        mTabIcons[TAB_SYSTEM] = headerView.findViewById(R.id.icon_system);

        mTabTexts[TAB_THEMES] = headerView.findViewById(R.id.text_themes);
        mTabTexts[TAB_QS] = headerView.findViewById(R.id.text_qs);
        mTabTexts[TAB_STATUSBAR] = headerView.findViewById(R.id.text_statusbar);
        mTabTexts[TAB_LOCKSCREEN] = headerView.findViewById(R.id.text_lockscreen);
        mTabTexts[TAB_SYSTEM] = headerView.findViewById(R.id.text_system);

        for (int i = 0; i < 5; i++) {
            final int tabIndex = i;
            if (mTabViews[i] != null) {
                mTabViews[i].setOnClickListener(v -> selectTab(tabIndex));
            }
        }

        updateTabUI(mCurrentTab);
    }

    private void selectTab(int tabIndex) {
        if (mCurrentTab == tabIndex) return;
        mCurrentTab = tabIndex;
        updateTabUI(tabIndex);
        loadTabPreferences(tabIndex);
    }

    private void updateTabUI(int selectedTab) {
        final Context context = getContext();
        if (context == null) return;

        final int selectedBg = R.drawable.blaze_tab_item_selected_bg;
        final int unselectedColor = context.getColor(R.color.blaze_tab_unselected_icon_color);
        final int selectedColor = context.getColor(R.color.blaze_tab_selected_text_color);

        for (int i = 0; i < 5; i++) {
            if (mTabViews[i] == null) continue;

            if (i == selectedTab) {
                mTabViews[i].setBackgroundResource(selectedBg);
                if (mTabIcons[i] != null) {
                    mTabIcons[i].setColorFilter(selectedColor);
                }
                if (mTabTexts[i] != null) {
                    mTabTexts[i].setVisibility(View.VISIBLE);
                }
            } else {
                mTabViews[i].setBackground(null);
                if (mTabIcons[i] != null) {
                    mTabIcons[i].setColorFilter(unselectedColor);
                }
                if (mTabTexts[i] != null) {
                    mTabTexts[i].setVisibility(View.GONE);
                }
            }
        }
    }

    private void loadTabPreferences(int tabIndex) {
        PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            screen.removeAll();
        }

        switch (tabIndex) {
            case TAB_THEMES:
                addPreferencesFromResource(R.xml.blazehouse_themes);
                break;
            case TAB_QS:
                addPreferencesFromResource(R.xml.blazehouse_quick_settings);
                break;
            case TAB_STATUSBAR:
                addPreferencesFromResource(R.xml.blazehouse_status_bar);
                break;
            case TAB_LOCKSCREEN:
                addPreferencesFromResource(R.xml.blazehouse_lock_screen);
                break;
            case TAB_SYSTEM:
            default:
                addPreferencesFromResource(R.xml.blazehouse_miscellaneous);
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.add(Menu.NONE, MENU_INFO, Menu.NONE, R.string.blazehouse_info_title);
        item.setIcon(R.drawable.ic_blazehouse_info);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_INFO) {
            showBlazeHouseInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showBlazeHouseInfoDialog() {
        if (getActivity() == null) return;
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.blazehouse_title)
                .setMessage(R.string.blazehouse_info_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(R.drawable.ic_homepage_blaze)
                .show();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.VIEW_UNKNOWN;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blazehouse_miscellaneous);
}
