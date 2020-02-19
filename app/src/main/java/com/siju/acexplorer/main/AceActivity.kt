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

package com.siju.acexplorer.main

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.ActivityResult
import com.kobakei.ratethisapp.RateThisApp
import com.siju.acexplorer.R
import com.siju.acexplorer.base.view.BaseActivity
import com.siju.acexplorer.extensions.isLandscape
import com.siju.acexplorer.helper.ToolbarHelper
import com.siju.acexplorer.home.view.CategoryFragment
import com.siju.acexplorer.home.view.HomeScreenFragment
import com.siju.acexplorer.main.helper.REQUEST_CODE_UPDATE
import com.siju.acexplorer.main.helper.UpdateChecker
import com.siju.acexplorer.main.model.StorageUtils
import com.siju.acexplorer.main.model.groups.Category
import com.siju.acexplorer.main.view.FragmentsFactory
import com.siju.acexplorer.main.viewmodel.MainViewModel
import com.siju.acexplorer.main.viewmodel.Pane
import com.siju.acexplorer.permission.PermissionHelper
import com.siju.acexplorer.premium.PremiumUtils
import com.siju.acexplorer.search.view.SearchFragment
import com.siju.acexplorer.settings.AboutFragment
import com.siju.acexplorer.settings.SettingsFragment
import com.siju.acexplorer.storage.view.BaseFileListFragment
import com.siju.acexplorer.storage.view.DualPaneFragment
import com.siju.acexplorer.storage.view.FileListFragment
import com.siju.acexplorer.theme.Theme
import com.siju.acexplorer.tools.ToolsFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


private const val TAG = "AceActivity"
private const val ACTION_IMAGES = "android.intent.action.SHORTCUT_IMAGES"
private const val ACTION_MUSIC = "android.intent.action.SHORTCUT_MUSIC"
private const val ACTION_VIDEOS = "android.intent.action.SHORTCUT_VIDEOS"
private const val SELECTED_TAB = "selected_tab"
class AceActivity : BaseActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, MainCommunicator {

    private lateinit var mainViewModel: MainViewModel
    private var premiumUtils: PremiumUtils? = null
    private var category: Category? = null
    private var updateChecker: UpdateChecker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        mainViewModel.setPermissionHelper(PermissionHelper(this, applicationContext))

        setTabColor()
        initObservers()
        initListeners()
        checkIfInAppShortcut(intent)
        setupSelectedTabPosition(savedInstanceState)
        updateChecker = UpdateChecker(baseContext, this, updateCallback)
        setupPremiumUtils()
    }

    private fun setupSelectedTabPosition(savedInstanceState: Bundle?) {
        when {
            savedInstanceState == null -> {
                setSelectedTab(R.id.navigation_home)
            }
            savedInstanceState.getInt(SELECTED_TAB, -1) != -1 -> {
                setSelectedTab(savedInstanceState.getInt(SELECTED_TAB))
            }
            else -> {
                setSelectedTab(R.id.navigation_home)
            }
        }
    }

    private fun setSelectedTab(tab : Int) {
        bottom_navigation.selectedItemId = tab
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putInt(SELECTED_TAB, bottom_navigation.selectedItemId)
    }

    override fun getUpdateChecker(): UpdateChecker? {
        return updateChecker
    }

    override fun isPremiumVersion(): Boolean {
        if (::mainViewModel.isInitialized) {
            return mainViewModel.isPremiumVersion()
        }
        return false
    }

    fun getViewModel() = mainViewModel

    private fun initListeners() {
        bottom_navigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener)
        bottom_navigation.setOnNavigationItemReselectedListener(navigationItemReselectedListener)
    }

    private fun setTabColor() {
        val darkColoredTheme = Theme.isDarkColoredTheme(resources, currentTheme)
        if (darkColoredTheme) {
            bottom_navigation.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.tab_bg_color))
        } else {
            bottom_navigation.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.colorPrimary))
        }
    }

    private fun initObservers() {
        mainViewModel.permissionStatus.observe(this, Observer { permissionStatus ->
            when (permissionStatus) {
                is PermissionHelper.PermissionState.Required -> mainViewModel.requestPermissions()
                is PermissionHelper.PermissionState.Rationale -> mainViewModel.showPermissionRationale()
                else -> {
                }
            }
        })

        mainViewModel.dualMode.observe(this, Observer {
            Log.d(TAG, "Dual mode value:$it")
            it?.apply {
                if (it) {
                    onDualModeEnabled(resources.configuration)
                } else {
                    disableDualPane()
                }
            }
        })

        mainViewModel.storageScreenReady.observe(this, Observer {
            it?.apply {
                if (it) {
                    onDualModeEnabled(resources.configuration)
                }
            }
        })

        mainViewModel.homeClicked.observe(this, Observer {
            it?.apply {
                if (it) {
                    disableDualPane()
                    mainViewModel.setHomeClickedFalse()
                }
            }
        })

        mainViewModel.navigateToSearch.observe(this, Observer {
            it?.apply {
                if (it) {
                    disableDualPane()
                    mainViewModel.setNavigatedToSearch()
                    openFragment(SearchFragment.newInstance(), true)
                }
            }
        })
    }

    private fun setupPremiumUtils() {
        premiumUtils = PremiumUtils()
        premiumUtils?.onStart(this)
        premiumUtils?.showPremiumDialogIfNeeded(this, mainViewModel)
    }

    private fun checkIfInAppShortcut(intent: Intent?) {
        if (intent == null || intent.action == null) {
            setCategory(null)
            return
        }
        var category: Category? = null
        when (intent.action) {
            ACTION_IMAGES -> category = Category.GENERIC_IMAGES
            ACTION_MUSIC -> category = Category.GENERIC_MUSIC
            ACTION_VIDEOS -> category = Category.GENERIC_VIDEOS
        }
        if (mainViewModel.permissionStatus.value is PermissionHelper.PermissionState.Granted) {
            setCategory(category)
        } else {
            setCategory(null)
        }
    }

    private fun setCategory(value: Category?) {
        this.category = value
    }

    private fun onDualModeEnabled(configuration: Configuration?) {
        if (canEnableDualPane(configuration)) {
            enableDualPane()
            mainViewModel.refreshLayout(Pane.SINGLE)
        }
        mainViewModel.setStorageNotReady()
    }

    private fun enableDualPane() {
        Log.d(TAG, "enableDualPane")
        frame_container_dual.visibility = View.VISIBLE
        viewSeparator.visibility = View.VISIBLE
        createDualFragment()
    }

    private fun canEnableDualPane(configuration: Configuration?) = isCurrentScreenStorage() && configuration.isLandscape()

    private fun disableDualPane() {
        Log.d(TAG, "disableDualPane")
        frame_container_dual.visibility = View.GONE
        viewSeparator.visibility = View.GONE
        if (isCurrentScreenStorage()) {
            mainViewModel.refreshLayout(Pane.SINGLE)
        }
    }

    private fun isCurrentScreenStorage(): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.main_container)
        if (fragment is BaseFileListFragment) {
            return true
        }
        return false
    }

    private val navigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
        clearBackStack()
        disableDualPane()
        val fragment = FragmentsFactory.createFragment(menuItem.itemId)
        openFragment(fragment)
        true
    }

    private val navigationItemReselectedListener = BottomNavigationView.OnNavigationItemReselectedListener { menuItem ->
        clearBackStack()
        disableDualPane()
        val fragment = if (category != null) {
            CategoryFragment.newInstance(null, category!!)
        } else {
            FragmentsFactory.createFragment(menuItem.itemId)
        }
        setCategory(null)
        openFragment(fragment)
    }

    private fun clearBackStack() {
        for (i in 0 until supportFragmentManager.backStackEntryCount) {
            supportFragmentManager.popBackStack()
        }
    }


    private fun openFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun createDualFragment() {
        val fragment = DualPaneFragment.newInstance(StorageUtils.internalStorage, Category.FILES)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_container_dual, fragment)
        transaction.commit()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            val fragment = supportFragmentManager.findFragmentById(R.id
                    .main_container)
            if (fragment is SearchFragment) {
                fragment.performVoiceSearch(query)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        mainViewModel.onPermissionResult(requestCode, permissions, grantResults)
    }


    override fun onStart() {
        super.onStart()
        // Monitor launch times and interval from installation
        RateThisApp.onCreate(this)
        // If the criteria is satisfied, "Rate this app" dialog will be shown
        RateThisApp.showRateDialogIfNeeded(this)
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.onResume()
        updateChecker?.onResume()
    }

    override fun onBackPressed() {
        when (val fragment = supportFragmentManager.findFragmentById(R.id.main_container)) {
            is BaseFileListFragment -> when (val focusedFragment = getCurrentFocusFragment(fragment)) {
                is DualPaneFragment -> {
                    onDualPaneBackPress(focusedFragment)
                }
                is FileListFragment -> {
                    onSinglePaneBackPress(focusedFragment)
                }
            }
            is CategoryFragment -> {
                val backPressed = fragment.onBackPressed()
                if (backPressed) {
                    super.onBackPressed()
                }
            }
            is SearchFragment -> onSearchBackPress(fragment)
            is ToolsFragment -> {
                clearBackStack()
                switchToHomeScreen()
            }
            is SettingsFragment -> {
                if (supportFragmentManager.backStackEntryCount == 0) {
                    clearBackStack()
                    switchToHomeScreen()
                } else {
                    super.onBackPressed()
                }
            }
            else -> super.onBackPressed()
        }
    }

    private fun switchToHomeScreen() {
        bottom_navigation.selectedItemId = R.id.navigation_home
    }

    private fun onDualPaneBackPress(focusedFragment: DualPaneFragment) {
        val backPressNotHandled = focusedFragment.onBackPressed()
        if (backPressNotHandled) {
            super.onBackPressed()
            disableDualPane()
        }
    }

    private fun onSinglePaneBackPress(focusedFragment: FileListFragment) {
        val backPressNotHandled = focusedFragment.onBackPressed()
        if (backPressNotHandled) {
            super.onBackPressed()
            disableDualPane()
        }
    }

    private fun onSearchBackPress(fragment: SearchFragment) {
        val backPressNotHandled = fragment.onBackPressed()
        if (backPressNotHandled) {
            super.onBackPressed()
        }
    }

    private fun getCurrentFocusFragment(fragment: BaseFileListFragment): Fragment? {
        return if (mainViewModel.isDualPaneInFocus) {
            supportFragmentManager.findFragmentById(R.id.frame_container_dual)
        } else {
            fragment
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat,
                                           pref: Preference): Boolean {
        val args = pref.extras
        val fragment = AboutFragment()
        fragment.arguments = args
        fragment.setTargetFragment(caller, 0)
        replaceFragment(supportFragmentManager, fragment)
        ToolbarHelper.setToolbarTitle(this, pref.title.toString())
        ToolbarHelper.showToolbarAsUp(this)
        return true
    }

    private fun replaceFragment(fragmentManager: FragmentManager,
                                fragment: Fragment) {
        fragmentManager.beginTransaction()
                .replace(R.id.frameSettings, fragment)
                .addToBackStack(null)
                .commit()
    }

    private val updateCallback = object : UpdateChecker.UpdateCallback {
        override fun onUpdateDownloaded(appUpdateManager: AppUpdateManager) {
            showUpdateDownloadedSnackbar()
        }

        override fun onUpdateInstalled() {
            removeUpdateBadge()
            mainViewModel.onUpdateInstalled()
        }

        override fun onUpdateCancelledByUser() {
            showUpdateBadge()
        }

        override fun onUpdateSnackbarDismissed() {
            showUpdateBadge()
        }

        override fun onUpdateDownloading() {
            Toast.makeText(this@AceActivity, getString(R.string.update_downloading), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UPDATE) {
            when (resultCode) {
                Activity.RESULT_CANCELED -> {
                    mainViewModel.userCancelledUpdate()
                    showUpdateBadge()
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    showUpdateBadge()
                }
            }
        }
    }

    private fun showUpdateDownloadedSnackbar() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main_container)
        if (fragment is HomeScreenFragment) {
            fragment.showUpdateSnackbar(updateChecker)
        }
        else if (fragment is BaseFileListFragment) {
            fragment.showUpdateSnackbar(updateChecker)
        }
    }

    private fun showUpdateBadge() {
        bottom_navigation.getOrCreateBadge(R.id.navigation_settings)
    }

    private fun removeUpdateBadge() {
        bottom_navigation.removeBadge(R.id.navigation_settings)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged:${newConfig.isLandscape()}, dualMode:${mainViewModel.dualMode.value}")
        if (mainViewModel.dualMode.value == true && newConfig.isLandscape()) {
            onDualModeEnabled(newConfig)
        } else {
            disableDualPane()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateChecker?.onDestroy()
    }

}
