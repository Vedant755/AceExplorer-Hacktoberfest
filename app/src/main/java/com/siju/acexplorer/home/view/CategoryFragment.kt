package com.siju.acexplorer.home.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.siju.acexplorer.R
import com.siju.acexplorer.base.view.BaseActivity
import com.siju.acexplorer.extensions.findCurrentFragment
import com.siju.acexplorer.extensions.inflateLayout
import com.siju.acexplorer.home.types.CategoryData
import com.siju.acexplorer.main.model.groups.Category
import com.siju.acexplorer.main.model.groups.CategoryHelper
import com.siju.acexplorer.main.viewmodel.MainViewModel
import com.siju.acexplorer.search.helper.SearchUtils
import com.siju.acexplorer.storage.view.FileListFragment
import com.siju.acexplorer.theme.Theme
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*


class CategoryFragment : Fragment(), CategoryMenuHelper, Toolbar.OnMenuItemClickListener {

    private val mainViewModel: MainViewModel by activityViewModels()
    private val menuItemWrapper = MenuItemWrapper()

    private lateinit var pagerAdapter: CategoryPagerAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var category = Category.GENERIC_IMAGES

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflateLayout(R.layout.category_pager, container)
    }

    override fun getCategoryView() = view

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainViewModel.setCategoryMenuHelper(this)
        setupUI(view)
    }

    private fun setupUI(view: View) {
        setupToolbar()
        viewPager = view.findViewById(R.id.categoryPager)
        viewPager.registerOnPageChangeCallback(pageChangeListener)
        //TODO 22 Feb 2020 AE-183 - Temporary solution to layout wrong if adjoining tab has no files
        // on tab click only. Works fine on swipe. Verify with new versions of viewpager > 1.0
        viewPager.offscreenPageLimit = 2
        tabLayout = view.findViewById(R.id.categoryTabs)
        setTabColor(tabLayout)
        pagerAdapter = CategoryPagerAdapter(this)
        setupAdapter()
        setupTabWithPager()
        checkIfFilePicker()
    }

    private fun checkIfFilePicker() {
        if (mainViewModel.isFilePicker()) {
            toolbar.menu.findItem(R.id.action_search).isVisible = false
        }
    }

    private fun setupToolbar() {
        toolbar.inflateMenu(R.menu.filelist_base)
        toolbar.setOnMenuItemClickListener(this)
    }

    private fun setTabColor(tabLayout: TabLayout) {
        val activity = activity
        val context = context
        context?.let {
            if (activity is BaseActivity) {
                val darkColoredTheme = Theme.isDarkColoredTheme(resources, activity.currentTheme)
                if (darkColoredTheme) {
                    tabLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.tab_bg_color))
                } else {
                    tabLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    tabLayout.setTabTextColors(ContextCompat.getColor(context, R.color.tab_text_color),
                            ContextCompat.getColor(context, R.color.tab_selected_text_color))
                    tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(context, R.color.colorAccent))
                }
            }
        }
    }

    override fun setToolbarTitle() {
        toolbar.title = CategoryHelper.getCategoryName(context, category).toUpperCase(Locale.getDefault())
    }

    private fun setupAdapter() {
        val args = arguments
        args?.let {
            val bundle = CategoryFragmentArgs.fromBundle(args)
            val path = bundle.path
            category = bundle.category
            createCategoryData(path, category)
            viewPager.adapter = pagerAdapter
        }
    }

    private fun setupTabWithPager() {
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = pagerAdapter.getTitle(pos)
        }.attach()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        menuItemWrapper.setMenuItem(item)
        mainViewModel.onMenuItemClicked(menuItemWrapper)
        return false
    }

    override fun disableTab() {
        viewPager.isUserInputEnabled = false
        val tabStrip: LinearLayout = tabLayout.getChildAt(0) as LinearLayout
        for (pos in 0 until pagerAdapter.itemCount) {
            tabStrip.getChildAt(pos).setOnTouchListener { view, _ ->
                view.performClick()
                true
            }
        }
        tabLayout.alpha = 0.5f
    }

    override fun enableTab() {
        viewPager.isUserInputEnabled = true
        val tabStrip: LinearLayout = tabLayout.getChildAt(0) as LinearLayout
        for (pos in 0 until pagerAdapter.itemCount) {
            tabStrip.getChildAt(pos).setOnTouchListener { view, _ ->
                view.performClick()
                false
            }
        }
        tabLayout.alpha = 1f
    }


    private val pageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (position == -1) {
                return
            }
            mainViewModel.refreshData()
        }
    }

    private fun createCategoryData(path: String?, category: Category) {
        Log.d(this.javaClass.simpleName, "category:$category")
        setToolbarTitle()
        if (category == Category.WHATSAPP || category == Category.TELEGRAM) {
            createFolderCategoryData(path, category)
        } else if (category == Category.DOCS) {
            createDocCategoryData(path, category)
        } else {
            createGenericCategoryData(path, category)
        }
    }

    private fun createGenericCategoryData(path: String?, category: Category) {
        val allCategory = when (category) {
            Category.GENERIC_IMAGES -> Category.IMAGES_ALL
            Category.GENERIC_VIDEOS -> Category.VIDEO_ALL
            Category.GENERIC_MUSIC -> Category.ALL_TRACKS
            Category.RECENT -> Category.RECENT_ALL
            Category.LARGE_FILES -> Category.LARGE_FILES_ALL
            Category.CAMERA_GENERIC -> Category.CAMERA
            else -> null
        }
        context?.let { context ->
            if (allCategory == null) {
                pagerAdapter.addData(CategoryData( path, category, context.getString(R.string.category_all), this))
            }
            else {
                pagerAdapter.addData(CategoryData( path, allCategory, context.getString(R.string.category_all), this))
            }
            pagerAdapter.addData(CategoryData( path, category, getTitle(context, category), this))
        }
    }

    private fun getTitle(context: Context, category: Category): String {
        return when (category) {
            Category.GENERIC_IMAGES -> context.getString(R.string.categories_folder)
            Category.GENERIC_VIDEOS -> context.getString(R.string.categories_folder)
            Category.GENERIC_MUSIC, Category.RECENT, Category.LARGE_FILES, Category.CAMERA_GENERIC -> context.getString(R.string.categories)
            Category.SEARCH_FOLDER_IMAGES -> context.getString(R.string.image)
            Category.SEARCH_FOLDER_VIDEOS -> context.getString(R.string.nav_menu_video)
            Category.SEARCH_FOLDER_AUDIO -> context.getString(R.string.audio)
            Category.SEARCH_FOLDER_DOCS -> context.getString(R.string.home_docs)
            Category.PDF -> context.getString(R.string.pdf)
            Category.DOCS_OTHER -> context.getString(R.string.other)
            else -> "null"
        }
    }

    private fun createFolderCategoryData(path: String?, category: Category) {
        context?.let { context ->
            pagerAdapter.addData(CategoryData( path, category, context.getString(R.string.category_all), this))
            pagerAdapter.addData(CategoryData( getSubDirImagePath(path, Category.SEARCH_FOLDER_IMAGES),
                    Category.SEARCH_FOLDER_IMAGES, getTitle(context, Category.SEARCH_FOLDER_IMAGES), this))
            pagerAdapter.addData(CategoryData( getSubDirImagePath(path, Category.SEARCH_FOLDER_VIDEOS),
                    Category.SEARCH_FOLDER_VIDEOS, getTitle(context, Category.SEARCH_FOLDER_VIDEOS), this))
            pagerAdapter.addData(CategoryData( getSubDirImagePath(path, Category.SEARCH_FOLDER_AUDIO),
                    Category.SEARCH_FOLDER_AUDIO, getTitle(context, Category.SEARCH_FOLDER_AUDIO), this))
            pagerAdapter.addData(CategoryData( getSubDirImagePath(path, Category.SEARCH_FOLDER_DOCS),
                    Category.SEARCH_FOLDER_DOCS, getTitle(context, Category.SEARCH_FOLDER_DOCS), this))
        }
    }

    private fun createDocCategoryData(path: String?, category: Category) {
        context?.let { context ->
            pagerAdapter.addData(CategoryData( path, category, context.getString(R.string.category_all), this))
            pagerAdapter.addData(CategoryData( path,
                    Category.PDF, getTitle(context, Category.PDF), this))
            pagerAdapter.addData(CategoryData( path,
                    Category.DOCS_OTHER, getTitle(context, Category.DOCS_OTHER), this))
        }
    }

    private fun getSubDirImagePath(path: String?, category: Category): String? {
        if (path == null) {
            return null
        }
        when (path) {
            SearchUtils.getWhatsappDirectory() -> {
                return when (category) {
                    Category.SEARCH_FOLDER_IMAGES -> SearchUtils.getWhatsappImagesDirectory()
                    Category.SEARCH_FOLDER_VIDEOS -> SearchUtils.getWhatsappVideosDirectory()
                    Category.SEARCH_FOLDER_AUDIO -> SearchUtils.getWhatsappAudioDirectory()
                    Category.SEARCH_FOLDER_DOCS -> SearchUtils.getWhatsappDocDirectory()
                    else -> path
                }
            }
            SearchUtils.getTelegramDirectory() -> {
                return when (category) {
                    Category.SEARCH_FOLDER_IMAGES -> SearchUtils.getTelegramImagesDirectory()
                    Category.SEARCH_FOLDER_VIDEOS -> SearchUtils.getTelegramVideosDirectory()
                    Category.SEARCH_FOLDER_AUDIO -> SearchUtils.getTelegramAudioDirectory()
                    Category.SEARCH_FOLDER_DOCS -> SearchUtils.getTelegramDocsDirectory()
                    else -> path
                }
            }
        }
        return path
    }

    fun onBackPressed(): Boolean {
        val fragment = viewPager.findCurrentFragment(childFragmentManager)
        if (fragment is FileListFragment) {
            return fragment.onBackPressed()
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        viewPager.unregisterOnPageChangeCallback(pageChangeListener)
        pagerAdapter.clear()
        mainViewModel.setCategoryMenuHelper(null)
    }
}