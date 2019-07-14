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

package com.siju.acexplorer.storage.view

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.siju.acexplorer.AceApplication
import com.siju.acexplorer.R
import com.siju.acexplorer.ads.AdsView
import com.siju.acexplorer.analytics.Analytics
import com.siju.acexplorer.common.types.FileInfo
import com.siju.acexplorer.main.model.groups.Category
import com.siju.acexplorer.main.model.helper.FileUtils
import com.siju.acexplorer.main.model.helper.ShareHelper
import com.siju.acexplorer.main.model.helper.UriHelper
import com.siju.acexplorer.main.model.helper.ViewHelper
import com.siju.acexplorer.main.view.dialog.DialogHelper
import com.siju.acexplorer.main.viewmodel.MainViewModel
import com.siju.acexplorer.permission.PermissionHelper
import com.siju.acexplorer.storage.model.SortMode
import com.siju.acexplorer.storage.model.StorageModelImpl
import com.siju.acexplorer.storage.model.ViewMode
import com.siju.acexplorer.storage.model.operations.OperationAction
import com.siju.acexplorer.storage.model.operations.OperationResultCode
import com.siju.acexplorer.storage.model.operations.Operations
import com.siju.acexplorer.storage.viewmodel.FileListViewModel
import com.siju.acexplorer.storage.viewmodel.FileListViewModelFactory
import com.siju.acexplorer.utils.InstallHelper
import kotlinx.android.synthetic.main.main_list.*
import kotlinx.android.synthetic.main.toolbar.*

const val KEY_PATH = "path"
const val KEY_CATEGORY = "category"
private const val TAG = "BaseFileListFragment"
private const val SAF_REQUEST = 2000

open class BaseFileListFragment : Fragment() {

    private var hiddenMenuItem: MenuItem? = null
    private var path: String? = null
    private var category = Category.FILES
    private lateinit var filesList: FilesList
    private lateinit var floatingView: FloatingView
    private lateinit var navigationView: NavigationView
    private lateinit var mainViewModel: MainViewModel
    private lateinit var fileListViewModel: FileListViewModel
    private lateinit var adView: AdsView
    private lateinit var menuControls: MenuControls

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.main_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)

        getArgs()
        adView = AdsView(main_content)
        setupToolbar()
        setupViewModels()

        val view = view
        view?.let {
            filesList = FilesList(this, view, fileListViewModel.getViewMode())
            floatingView = FloatingView(view, this)
            navigationView = NavigationView(view, fileListViewModel.navigationCallback)
            menuControls = MenuControls(this, view, category)
        }
        setupMultiSelection()
        setupNavigationView()
        initObservers()
        Log.e(TAG, "onAct created:$hiddenMenuItem")
        setHiddenCheckedState(fileListViewModel.shouldShowHiddenFiles())
    }

    private fun setupMultiSelection() {
        val multiSelectionHelper = MultiSelectionHelper()
        fileListViewModel.multiSelectionHelper = multiSelectionHelper
        filesList.setMultiSelectionHelper(multiSelectionHelper)
        multiSelectionHelper.setMultiSelectionListener(fileListViewModel.multiSelectionListener)
    }

    private fun setupToolbar() {
        toolbar.title = resources.getString(R.string.app_name)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
    }

    private fun setupNavigationView() {
        fileListViewModel.setNavigationView(navigationView)
        navigationView.showNavigationView()
    }

    private fun getArgs() {
        val args = arguments
        args?.let {
            path = it.getString(KEY_PATH)
            category = it.getSerializable(KEY_CATEGORY) as Category
        }
    }

    private fun setupViewModels() {
        val activity = requireNotNull(activity)
        mainViewModel = ViewModelProviders.of(activity).get(MainViewModel::class.java)
        val viewModelFactory = FileListViewModelFactory(StorageModelImpl(AceApplication.appContext))
        fileListViewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(FileListViewModel::class.java)
    }

    private fun initObservers() {
        mainViewModel.permissionStatus.observe(viewLifecycleOwner, Observer { permissionStatus ->
            Log.e(TAG, "permissionStatus state:$permissionStatus")
            when (permissionStatus) {
                is PermissionHelper.PermissionState.Granted -> fileListViewModel.loadData(path,
                                                                                          category)
                else -> {
                }
            }
        })

        mainViewModel.premiumLiveData.observe(viewLifecycleOwner, Observer {
            Log.e(TAG, "Premium state:$it")
            it?.apply {
                if (it.entitled) {
                    hideAds()
                }
                else {
                    showAds()
                }
            }
        })

        mainViewModel.theme.observe(viewLifecycleOwner, Observer {
            floatingView.setTheme(it)
        })

        fileListViewModel.fileData.observe(viewLifecycleOwner, Observer {
            if (::filesList.isInitialized) {
                filesList.onDataLoaded(it)
            }
        })

        fileListViewModel.showFab.observe(viewLifecycleOwner, Observer { showFab ->
            if (showFab) {
                floatingView.showFab()
            }
            else {
                floatingView.hideFab()
            }
        })

        fileListViewModel.viewFileEvent.observe(viewLifecycleOwner, Observer {
            viewFile(it.first, it.second)
        })

        fileListViewModel.viewMode.observe(viewLifecycleOwner, Observer {
            if (::filesList.isInitialized) {
                filesList.onViewModeChanged(it)
            }
        })

        fileListViewModel.sortEvent.observe(viewLifecycleOwner, Observer { sortMode ->
            DialogHelper.showSortDialog(context, sortMode, sortDialogListener)
        })

        fileListViewModel.installAppEvent.observe(viewLifecycleOwner, Observer {
            val canInstall = it.first
            if (canInstall) {
                openInstallScreen(it.second)
            }
            else {
                InstallHelper.requestUnknownAppsInstallPermission(this)
            }
        })

        fileListViewModel.actionModeState.observe(viewLifecycleOwner, Observer {
            Log.e(TAG, "actionModeState:$it")
            it?.apply {
                when (it) {
                    ActionModeState.STARTED -> {
                        floatingView.hideFab()
                        menuControls.onStartActionMode()
                    }
                    ActionModeState.ENDED -> {
                        if (Category.FILES == category) {
                            floatingView.showFab()
                        }
                        menuControls.onEndActionMode()
                    }
                }
            }
        })

        fileListViewModel.selectedCount.observe(viewLifecycleOwner, Observer {
            it?.apply {
                if (fileListViewModel.actionModeState.value != ActionModeState.ENDED) {
                    menuControls.onSelectedCountChanged(it)
                }
            }
        })

        fileListViewModel.refreshEvent.observe(viewLifecycleOwner, Observer {
            it?.apply {
                if (it) {
                    filesList.refresh()
                }
            }
        })

        fileListViewModel.singleOpData.observe(viewLifecycleOwner, Observer {
            it?.apply {
                handleSingleItemOperation(it)
            }
        })

        fileListViewModel.operationData.observe(viewLifecycleOwner, Observer {
            it?.apply {
                handleOperationResult(it)
            }
        })

        fileListViewModel.noOpData.observe(viewLifecycleOwner, Observer {
            it?.apply {
                when(it.first) {
                    Operations.FOLDER_CREATION -> showCreateFolderDialog(context)
                    Operations.FILE_CREATION -> showCreateFileDialog(context)
                }
            }
        })

        fileListViewModel.multiSelectionOpData.observe(viewLifecycleOwner, Observer {
            it?.apply {
                handleMultiItemOperation(it)
            }
        })
    }

    private fun handleOperationResult(operationResult: Pair<Operations, OperationAction>) {
        Log.e(TAG, "handleOperationResult: $operationResult")
        val action = operationResult.second
        val operation = operationResult.first
        val context = context
        context?.let {
            if (operation == Operations.DELETE) {
                onDeletedResult(action)
            }
            when (action.operationResult.resultCode) {
                OperationResultCode.SUCCESS -> {
                    dismissDialog()
                    fileListViewModel.loadData(path, category)
                }
                OperationResultCode.SAF -> {
                    dismissDialog()
                    showSAFDialog(context, action.operationData.arg1)
                }
                OperationResultCode.INVALID_FILE -> onOperationError(operation, context.getString(
                        R.string.msg_error_invalid_name))
                OperationResultCode.FILE_EXISTS -> onOperationError(operation, context.getString(
                        R.string.msg_file_exists))
                OperationResultCode.FAIL -> {
                    dismissDialog()
                    Toast.makeText(context, R.string.msg_operation_failed, Toast
                            .LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onDeletedResult(operationAction: OperationAction) {
        val count = operationAction.operationResult.count
        if (count > 0) {
            FileUtils.showMessage(context, resources.getQuantityString(R.plurals.number_of_files, count, count) +
            " " + resources.getString(R.string.msg_delete_success))
        }
        else {
            FileUtils.showMessage(context, resources.getString(R.string.msg_delete_failure))
        }
    }

    private fun handleSingleItemOperation(operationData: Pair<Operations, FileInfo>) {
        Log.e(TAG, "handleSingleItemOperation: ")
        when (operationData.first) {
            Operations.RENAME -> {
                context?.let { context -> showRenameDialog(context, operationData.second) }
            }
            Operations.INFO -> {
                context?.let { context ->  DialogHelper.showInfoDialog(context, operationData.second,
                                                                       Category.FILES == category)}
            }
        }
    }

    private fun handleMultiItemOperation(operationData: Pair<Operations, ArrayList<FileInfo?>>) {
       when(operationData.first) {
           Operations.DELETE -> {
               context?.let {
                   context -> showDeleteDialog(context, operationData.second)
               }
           }
           Operations.SHARE -> {
               context?.let { ShareHelper.shareFiles(it, operationData.second, category) }
           }
       }
    }



    fun onCreateDirClicked() {
        fileListViewModel.onFABClicked(Operations.FOLDER_CREATION, path)
    }

    fun onCreateFileClicked() {
        fileListViewModel.onFABClicked(Operations.FILE_CREATION, path)
    }

    private fun showCreateFolderDialog(context: Context?) {
        context?.let {
            val title = context.getString(R.string.new_folder)
            val texts = arrayOf(title, context.getString(R.string.enter_name),
                                context.getString(R.string.create),
                                context.getString(R.string.dialog_cancel))

            DialogHelper.showInputDialog(context, texts, Operations.FOLDER_CREATION, null, alertDialogListener)
        }
    }

    private fun showCreateFileDialog(context: Context?) {
        context?.let {
            val title = context.getString(R.string.new_file)
            val texts = arrayOf(title, context.getString(R.string.enter_name),
                                context.getString(R.string.create),
                                context.getString(R.string.dialog_cancel))

            DialogHelper.showInputDialog(context, texts, Operations.FILE_CREATION, null, alertDialogListener)
        }
    }

    private fun showDeleteDialog(context: Context?, files : ArrayList<FileInfo?>) {
        context?.let {
            DialogHelper.showDeleteDialog(context, files, false, deleteDialogListener)
        }
    }

    private fun onOperationError(operation: Operations,
                                 message: String) {
        when (operation) {
            Operations.FOLDER_CREATION, Operations.FILE_CREATION, Operations.EXTRACT, Operations.RENAME, Operations.COMPRESS -> {
                val editText = dialog?.findViewById<EditText>(R.id.editFileName)
                editText?.requestFocus()
                editText?.error = message
            }
            Operations.HIDE -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            else -> {
            }
        }
    }


    private fun showRenameDialog(context: Context, fileInfo: FileInfo) {
        val title = context.getString(R.string.action_rename)
        val texts = arrayOf(title, context.getString(R.string.enter_name),
                            context.getString(R.string.action_rename),
                            context.getString(R.string.dialog_cancel))
        DialogHelper.showInputDialog(context, texts, Operations.RENAME, fileInfo.filePath,
                                     alertDialogListener)
    }

    private fun showSAFDialog(context: Context, path: String) {
        DialogHelper.showSAFDialog(context, path, safDialogListener)
    }

    private fun openInstallScreen(path: String?) {
        val context = context
        context?.let {
            InstallHelper.openInstallAppScreen(context,
                                               UriHelper.createContentUri(
                                                       context.applicationContext,
                                                       path))
        }
    }

    private fun dismissDialog() {
        dialog?.dismiss()
    }

    private var dialog: Dialog? = null

    private val alertDialogListener = object : DialogHelper.DialogCallback {

        override fun onPositiveButtonClick(dialog: Dialog?, operation: Operations?, name: String?) {
            this@BaseFileListFragment.dialog = dialog
            Log.e(TAG, "onPositiveButtonClick: dialog:$dialog")
            fileListViewModel.onOperation(operation, name)
        }

        override fun onNegativeButtonClick(operations: Operations?) {
        }
    }

    private val deleteDialogListener = DialogHelper.DeleteDialogListener { view, isTrashEnabled, filesToDelete ->
         fileListViewModel.deleteFiles(filesToDelete)
    }

    private fun triggerStorageAccessFramework() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        if (activity?.packageManager?.resolveActivity(intent, 0) != null) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
                                   SAF_REQUEST)
        }
        else {
            Toast.makeText(context, context?.getString(R.string.msg_error_not_supported),
                           Toast.LENGTH_LONG).show()
        }
    }


    private val safDialogListener = object : DialogHelper.AlertDialogListener {

        override fun onPositiveButtonClick(view: View?) {
            triggerStorageAccessFramework()
        }

        override fun onNegativeButtonClick(view: View?) {
            Toast.makeText(context, context?.getString(R.string.error), Toast
                    .LENGTH_SHORT).show()
        }

        override fun onNeutralButtonClick(view: View?) {
        }

    }

    private fun showAds() {
        adView.showAds()
    }

    private fun hideAds() {
        adView.hideAds()
    }

    fun handleItemClick(fileInfo: FileInfo, position: Int) {
        fileListViewModel.handleItemClick(fileInfo, position)
    }

    fun handleLongItemClick(fileInfo: FileInfo, second: Int) {
        fileListViewModel.handleLongClick(fileInfo, second)
    }

    fun onBackPressed() = fileListViewModel.onBackPress()

    private fun viewFile(path: String, extension: String?) {
        Log.e(TAG, "Viewfile:path:$path, extension:$extension")
        val context = context
        context?.let {
            when (extension?.toLowerCase()) {
                null -> {
                    val uri = UriHelper.createContentUri(context, path)
                    uri?.let {
                        DialogHelper.openWith(it, context)
                    }
                }
                ViewHelper.EXT_APK -> ViewHelper.viewApkFile(context, path,
                                                             fileListViewModel.apkDialogListener)
                else -> ViewHelper.viewFile(context, path, extension)
            }
        }
    }


    //    fun onBackPressed(): Boolean {
//        return storagesUi!!.onBackPress()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        storagesUi!!.onResume()
//    }
//
//
//    override fun onPause() {
//        storagesUi!!.onPause()
//        super.onPause()
//    }
//
//
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        when (requestCode) {
            InstallHelper.UNKNOWN_APPS_INSTALL_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    openInstallScreen(fileListViewModel.apkPath)
                    fileListViewModel.apkPath = null
                }
            }

            SAF_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val uri = intent?.data
                    if (uri == null) {
                        Toast.makeText(context, resources.getString(R.string
                                                                            .access_denied_external),
                                       Toast.LENGTH_LONG).show()
                    }
                    else {
                        fileListViewModel.handleSafResult(uri, intent.flags)
                    }

                }
                else {
                    Analytics.getLogger().SAFResult(false)
                    Toast.makeText(context, resources.getString(R.string
                                                                        .access_denied_external),
                                   Toast.LENGTH_LONG).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    //
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.filelist_base, menu)
        Log.e(TAG, "onCreateOptionsMenu")
        hiddenMenuItem = menu.findItem(R.id.action_hidden)
        if (::fileListViewModel.isInitialized) {
            setHiddenCheckedState(fileListViewModel.shouldShowHiddenFiles())
        }
    }

    private fun setHiddenCheckedState(state: Boolean) {
        hiddenMenuItem?.isChecked = state
    }

    //
//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        storagesUi!!.onConfigChanged(newConfig)
//    }
//
//    override fun onDestroyView() {
//        storagesUi!!.onViewDestroyed()
//        super.onDestroyView()
//    }
//
//    override fun onDestroy() {
//        storagesUi!!.onExit()
//        super.onDestroy()
//    }
//
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_view_list -> {
                fileListViewModel.switchView(ViewMode.LIST)
                return true
            }
            R.id.action_view_grid -> {
                fileListViewModel.switchView(ViewMode.GRID)
                return true
            }
            R.id.action_hidden -> {
                item.isChecked = !item.isChecked
                fileListViewModel.onHiddenFileSettingChanged(item.isChecked)
                return true
            }

            R.id.action_sort -> {
                fileListViewModel.onSortClicked()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onMenuItemClick(item: MenuItem) {
        fileListViewModel.onMenuItemClick(item.itemId)

    }

    private val sortDialogListener = object : DialogHelper.SortDialogListener {
        override fun onPositiveButtonClick(sortMode: SortMode) {
            fileListViewModel.onSort(sortMode)
        }

        override fun onNegativeButtonClick(view: View?) {
        }

    }
//
//    fun performVoiceSearch(query: String) {
//        storagesUi!!.performVoiceSearch(query)
//    }
//
//    fun collapseFab() {
//        storagesUi!!.collapseFab()
//    }
//
//    fun reloadList(directory: String, category: Category) {
//        storagesUi!!.reloadList(directory, category)
//    }
//
//    fun refreshList() {
//        storagesUi!!.refreshList()
//    }
//
//    fun removeHomeFromNavPath() {
//        storagesUi!!.removeHomeFromNavPath()
//    }
//
//    fun addHomeNavPath() {
//        storagesUi!!.addHomeNavPath()
//    }
//
//    fun refreshSpan() {
//        storagesUi!!.refreshSpan()
//    }
//
//    fun showDualPane() {
//        storagesUi!!.showDualPane()
//    }
//
//    fun hideDualPane() {
//        storagesUi!!.hideDualPane()
//        val fragment = activity!!.supportFragmentManager.findFragmentById(R.id.frame_container_dual)
//        if (fragment != null) {
//            val fragmentTransaction = activity!!.supportFragmentManager.beginTransaction()
//            fragmentTransaction.remove(fragment).commitAllowingStateLoss()
//        }
//    }
//
//
//    fun setHidden(showHidden: Boolean) {
//        storagesUi!!.setHidden(showHidden)
//    }
//
//    fun switchView(viewMode: Int) {
//        storagesUi!!.switchView(viewMode)
//    }
//
//    fun collapseSearchView() {
//        storagesUi!!.collapseSearchView()
//    }
}
