package com.siju.acexplorer.settings;


import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.siju.acexplorer.R;
import com.siju.acexplorer.filesystem.FileConstants;
import com.siju.acexplorer.filesystem.utils.ThemeUtils;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    private int mIsTheme; // Default is Dark

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pref_holder);
        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);
        setupActionBar();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsPreferenceFragment())
                .commit();

    }

    private void checkTheme() {
        int theme = ThemeUtils.getTheme(this);

        if (theme == FileConstants.THEME_DARK) {
            setTheme(R.style.Settings_BlackTheme);
            mIsTheme = FileConstants.THEME_DARK;
        } else {
            setTheme(R.style.Settings_LightTheme);
            mIsTheme = FileConstants.THEME_LIGHT;
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        AppBarLayout bar;
        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        bar = (AppBarLayout) LayoutInflater.from(this).inflate(R.layout.toolbar, root,
                false);
        root.addView(bar, 0); // insert at top
        Toolbar mToolbar = (Toolbar) bar.getChildAt(0);

        if (mIsTheme == FileConstants.THEME_DARK) {
            mToolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_colorPrimary));

            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_colorPrimaryDark));
            }
        }

        else {
            mToolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));

            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            }
        }

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);


        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
}
