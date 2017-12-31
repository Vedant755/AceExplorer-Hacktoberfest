/*
 * Copyright (C) 2017 Ace Explorer owned by Siju Sakaria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.siju.acexplorer.storage.model.backstack;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.siju.acexplorer.R;
import com.siju.acexplorer.analytics.Analytics;
import com.siju.acexplorer.logging.Logger;
import com.siju.acexplorer.model.groups.Category;
import com.siju.acexplorer.model.groups.StoragesGroup;
import com.siju.acexplorer.storage.view.StoragesUiView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import static com.siju.acexplorer.model.StorageUtils.getInternalStorage;


public class NavigationInfo {
    private static final String TAG = "NavigationInfo";
    private static final String SEPARATOR = "/";
    private Context              context;
    private LinearLayout         navDirectory;
    private HorizontalScrollView scrollNavigation;
    private NavigationCallback   navigationCallback;
    private ArrayList<String> externalSDPaths = new ArrayList<>();
    private String currentDir;
    private String initialDir = getInternalStorage();
    private boolean isCurrentDirRoot;
    private String  STORAGE_INTERNAL, STORAGE_ROOT, STORAGE_EXTERNAL;


    public NavigationInfo(StoragesUiView storagesUiView, NavigationCallback navigationCallback) {
        this.context = storagesUiView.getContext();
        navDirectory = storagesUiView.findViewById(R.id.navButtons);
        scrollNavigation = storagesUiView.findViewById(R.id.scrollNavigation);
        scrollNavigation.setBackgroundColor(ContextCompat.getColor(context, R.color
                .colorPrimary));
        this.navigationCallback = navigationCallback;
        STORAGE_ROOT = context.getResources().getString(R.string.nav_menu_root);
        STORAGE_INTERNAL = context.getResources().getString(R.string.nav_menu_internal_storage);
        STORAGE_EXTERNAL = context.getResources().getString(R.string.nav_menu_ext_storage);
        externalSDPaths = StoragesGroup.getInstance().getExternalSDList();
    }

    public void setInitialDir(String currentDir) {
        if (currentDir.contains(getInternalStorage())) {
            initialDir = getInternalStorage();
            isCurrentDirRoot = false;
        } else if (externalSDPaths.size() > 0) {
            for (String path : externalSDPaths) {
                if (currentDir.contains(path)) {
                    initialDir = path;
                    isCurrentDirRoot = false;
                    return;
                }
            }
            initialDir = File.separator;
        } else {
            initialDir = File.separator;
        }
        Logger.log(TAG, "initializeStartingDirectory--startingdir=" + initialDir);

    }

//    private void checkIfFavIsRootDir() {
//
//        if (!currentDir.contains(getInternalStorage()) && !externalSDPaths.contains
//                (currentDir)) {
//            isCurrentDirRoot = true;
//            initialDir = File.separator;
//        }
//    }

    public void addHomeNavButton(boolean isHomeScreenEnabled, Category category) {

        clearNavigation();
        if (isHomeScreenEnabled) {
            ImageButton imageButton = new ImageButton(context);
            imageButton.setImageResource(R.drawable.ic_home_white);
            imageButton.setBackgroundColor(Color.parseColor("#00ffffff"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER_VERTICAL;

            imageButton.setLayoutParams(params);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Analytics.getLogger().navBarClicked(true);
                    navigationCallback.onHomeClicked();
                }
            });
            addViewToNavigation(imageButton);

            ImageView navArrow = new ImageView(context);
            params.leftMargin = 15;
            params.rightMargin = 20;
            navArrow.setImageResource(R.drawable.ic_arrow_nav);
            navArrow.setLayoutParams(params);
            addViewToNavigation(navArrow);
            addTitleText(category);
        } else {
            addTitleText(category);
        }

    }

    private void addTitleText(Category category) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;

        if (!category.equals(Category.FILES) && !category.equals(Category.DOWNLOADS)) {
            String title = getTitleForCategory(category).toUpperCase(Locale.getDefault());
            final TextView textView = new TextView(context);
            textView.setText(title);
            textView.setTextColor(ContextCompat.getColor(context, R.color.navButtons));
            textView.setTextSize(19);
            int paddingLeft = context.getResources().getDimensionPixelSize(R.dimen.padding_10);

            int paddingRight = context.getResources().getDimensionPixelSize(R.dimen.padding_60);
            textView.setPadding(paddingLeft, 0, paddingRight, 0);
            textView.setLayoutParams(params);
            addViewToNavigation(textView);
        }
    }

    private String getTitleForCategory(Category category) {
        switch (category) {
            case AUDIO:
                return context.getString(R.string.audio);
            case VIDEO:
                return context.getString(R.string.nav_menu_video);
            case IMAGE:
                return context.getString(R.string.nav_menu_image);
            case DOCS:
                return context.getString(R.string.nav_menu_docs);
            case DOWNLOADS:
                return context.getString(R.string.downloads);
            case COMPRESSED:
                return context.getString(R.string.compressed);
            case FAVORITES:
                return context.getString(R.string.nav_header_favourites);
            case PDF:
                return context.getString(R.string.pdf);
            case APPS:
                return context.getString(R.string.apk);
            case LARGE_FILES:
                return context.getString(R.string.library_large);

        }
        return context.getString(R.string.app_name);
    }

    public void setNavDirectory(String path, boolean isHomeScreenEnabled, Category category) {
        String[] parts;
        parts = path.split(SEPARATOR);

        clearNavigation();
        currentDir = path;
        String dir = "";
        addHomeNavButton(isHomeScreenEnabled, category);
        // If root dir , parts will be 0
        if (parts.length == 0) {

            isCurrentDirRoot = true;
            initialDir = File.separator;
            setNavDir(File.separator, File.separator); // Add Root button
        } else {
            int count = 0;
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                dir = stringBuilder.append(File.separator).append(parts[i]).toString();

                if (!dir.contains(initialDir)) {
                    continue;
                }
                /*Count check so that ROOT is added only once in Navigation
                  Handles the scenario :
                  1. When Fav item is a root child and if we click on any folder in that fav item
                     multiple ROOT blocks are not added to Navigation view*/
                if (isCurrentDirRoot && count == 0) {
                    setNavDir(File.separator, File.separator);
                }

                count++;
                setNavDir(dir, parts[i]);
            }
        }

    }

    private void setNavDir(String dir, String parts) {

        int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;
        if (dir.equals(getInternalStorage())) {
            isCurrentDirRoot = false;
            createNavButton(STORAGE_INTERNAL, dir);
        } else if (dir.equals(File.separator)) {
            createNavButton(STORAGE_ROOT, dir);
        } else if (externalSDPaths.contains(dir)) {
            isCurrentDirRoot = false;
            createNavButton(STORAGE_EXTERNAL, dir);
        } else {
            ImageView navArrow = new ImageView(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT,
                                                                                   WRAP_CONTENT);
            layoutParams.leftMargin = 20;
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            navArrow.setImageResource(R.drawable.ic_arrow_nav);
            navArrow.setLayoutParams(layoutParams);

            addViewToNavigation(navArrow);
            createNavButton(parts, dir);
            scrollNavigation();

        }
    }

    private void createNavButton(String text, final String dir) {

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;

        if (text.equals(STORAGE_INTERNAL) || text.equals(STORAGE_EXTERNAL) ||
                text.equals(STORAGE_ROOT)) {
            ImageButton imageButton = new ImageButton(context);
            if (text.equals(STORAGE_INTERNAL)) {
                imageButton.setImageResource(R.drawable.ic_storage_white_nav);
            } else if (text.equals(STORAGE_EXTERNAL)) {
                imageButton.setImageResource(R.drawable.ic_ext_nav);
            } else {
                imageButton.setImageResource(R.drawable.ic_root_white_nav);
            }
            imageButton.setBackgroundColor(Color.parseColor("#00ffffff"));
            imageButton.setLayoutParams(params);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (dir != null) {
                        navButtonOnClick(dir);
                    }
                }
            });
            addViewToNavigation(imageButton);


        } else {
            final TextView textView = new TextView(context);
            textView.setText(text);
            textView.setAllCaps(true);
            textView.setTextColor(ContextCompat.getColor(context, R.color.navButtons));
            textView.setTextSize(15);
            params.leftMargin = 20;
            textView.setPadding(0, 0, 35, 0);
            textView.setLayoutParams(params);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Logger.log(TAG, "nav button onclick--dir=" + dir);
                    if (dir != null) {
                        navButtonOnClick(dir);
                    }
                }
            });
            addViewToNavigation(textView);

        }
    }

    private void navButtonOnClick(final String dir) {
        Logger.log(TAG, "Dir=" + dir + " currentDir=" + currentDir);
        if (!currentDir.equals(dir)) {
            Analytics.getLogger().navBarClicked(false);
            navigationCallback.onNavButtonClicked(dir);

        }
    }


    public void removeHomeFromNavPath() {
        Logger.log(TAG, "Nav directory count=" + navDirectory.getChildCount());

        for (int i = 0; i < Math.min(navDirectory.getChildCount(), 2); i++) {
            navDirectory.removeViewAt(0);
        }
    }

    private void addViewToNavigation(View view) {
        navDirectory.addView(view);
    }

    private void clearNavigation() {
        navDirectory.removeAllViews();
    }

    public void showNavigationView() {
        scrollNavigation.setVisibility(View.VISIBLE);
    }

    private void scrollNavigation() {
        scrollNavigation.postDelayed(new Runnable() {
            public void run() {
                scrollNavigation.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        }, 100L);
    }


}
