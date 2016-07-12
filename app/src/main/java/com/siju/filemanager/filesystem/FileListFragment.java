package com.siju.filemanager.filesystem;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.siju.filemanager.BaseActivity;
import com.siju.filemanager.R;
import com.siju.filemanager.common.Logger;
import com.siju.filemanager.common.SharedPreferenceWrapper;
import com.siju.filemanager.filesystem.model.FileInfo;
import com.siju.filemanager.filesystem.ui.DialogBrowseFragment;
import com.siju.filemanager.filesystem.ui.DividerItemDecoration;
import com.siju.filemanager.filesystem.ui.TextDrawable;
import com.siju.filemanager.filesystem.utils.ExtractManager;
import com.siju.filemanager.filesystem.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import static android.R.attr.action;
import static android.R.attr.data;
import static android.R.attr.width;
import static android.R.attr.x;
import static android.R.attr.y;
import static android.R.id.list;
import static android.content.ClipData.newPlainText;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static android.media.CamcorderProfile.get;
import static com.siju.filemanager.BaseActivity.ACTION_VIEW_MODE;
import static com.siju.filemanager.R.id.buttonCount;
import static com.siju.filemanager.R.id.buttonExtract;
import static com.siju.filemanager.R.id.radioGroupPath;
import static com.siju.filemanager.R.id.textEmpty;
import static com.siju.filemanager.R.id.textPathSelect;
import static java.lang.System.currentTimeMillis;


/**
 * Created by Siju on 13-06-2016.
 */

public class FileListFragment extends Fragment implements LoaderManager
        .LoaderCallbacks<ArrayList<FileInfo>>,
        SearchView.OnQueryTextListener {

    //    private ListView fileList;
    private RecyclerView recyclerViewFileList;
    private View root;
    private final int LOADER_ID = 1000;
    private FileListAdapter fileListAdapter;
    private ArrayList<FileInfo> fileInfoList;
    private boolean mIsDualMode;
    private String mFilePath;
    private int mCategory;
    private int mViewMode = FileConstants.KEY_LISTVIEW;
    private String mPath;
    private boolean mIsZip;
    private SharedPreferenceWrapper preference;
    private TextView mTextEmpty;
    private boolean mIsDualActionModeActive;
    private boolean mIsLandscapeMode;
    private boolean mIsDualModeEnabledSettings;
    private Toolbar mToolbar;
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private boolean mStartDrag;
    private GestureDetectorCompat gestureDetector;
    private long mLongPressedTime;
    private boolean mIsDragInProgress;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.file_list, container, false);
        return root;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        initializeViews();
        mIsLandscapeMode = getResources().getConfiguration().orientation == Configuration
                .ORIENTATION_LANDSCAPE;
        mIsDualModeEnabledSettings = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean(FileConstants.PREFS_DUAL_PANE, false);

        Bundle args = new Bundle();
        final String fileName;

        if (getArguments() != null) {
            if (getArguments().getString(FileConstants.KEY_PATH) != null) {
                mFilePath = getArguments().getString(FileConstants.KEY_PATH);
            }
            mCategory = getArguments().getInt(FileConstants.KEY_CATEGORY, FileConstants.CATEGORY.FILES.getValue());

            mIsZip = getArguments().getBoolean(FileConstants.KEY_ZIP, false);
            mIsDualMode = getArguments().getBoolean(FileConstants.KEY_DUAL_MODE, false);

        }
        mViewMode = preference.getViewMode(getActivity());

        Log.d("TAG", "on onActivityCreated--Fragment" + mFilePath);
        Log.d("TAG", "View mode=" + mViewMode);

        fileListAdapter = new FileListAdapter(getContext(), fileInfoList, mCategory, mViewMode);
        recyclerViewFileList.setAdapter(fileListAdapter);

        args.putString(FileConstants.KEY_PATH, mFilePath);

        getLoaderManager().initLoader(LOADER_ID, args, this);

        fileListAdapter.setOnItemClickListener(new FileListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                if (((BaseActivity) getActivity()).getActionMode() != null) {
                    if (mIsDualActionModeActive) {
                        if (FileListFragment.this instanceof FileListDualFragment) {
                            itemClickActionMode(position);
                        } else {
                            handleCategoryItemClick(position);
                        }
                    } else {
                        if (FileListFragment.this instanceof FileListDualFragment) {
                            handleCategoryItemClick(position);
                        } else {
                            itemClickActionMode(position);
                        }
                    }
                } else {
                    handleCategoryItemClick(position);
                }
            }
        });
        fileListAdapter.setOnItemLongClickListener(new FileListAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int position) {
                Logger.log("TAG", "On long click" + mStartDrag);
                itemClickActionMode(position);
                mLongPressedTime = System.currentTimeMillis();

                if (((BaseActivity) getActivity()).getActionMode() != null && fileListAdapter
                        .getSelectedCount() >= 1) {
                    mStartDrag = true;

//                    Logger.log("TAG", "On long click drag");
     /*               Intent intent = new Intent();

                    intent.putExtra(FileConstants.KEY_PATH, fileInfoList.get(position).getFilePath());
                    ClipData data = ClipData.newIntent("", intent);
                    int count = fileListAdapter
                            .getSelectedCount();
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View view1 = inflater.inflate(R.layout.drag_shadow, null);
                        Button buttonCount = (Button) view1.findViewById(R.id.buttonCount);
    //                    textView.setLayoutParams(new LinearLayout.LayoutParams(100,100));
                        buttonCount.setText("" + count);

                    View.DragShadowBuilder shadowBuilder = new MyDragShadowBuilder(view, count);
                    view.startDrag(data, shadowBuilder, view, 0);*/

//                    view.setVisibility(View.INVISIBLE);
                }


            }
        });

 /*      fileListAdapter.setOnItemTouchListener(new FileListAdapter.OnItemTouchListener() {
            @Override
            public boolean onItemTouch(View view, int position, MotionEvent event) {
                Logger.log("TAG", "On item touch");
                if (mStartDrag && event.getAction() == MotionEvent.ACTION_DOWN) {
                    Logger.log("TAG", "On item touch inside");
                    Intent intent = new Intent();

                    intent.putExtra(FileConstants.KEY_PATH, fileInfoList.get(position).getFilePath());
                    ClipData data = ClipData.newIntent("", intent);
                    int count = fileListAdapter
                            .getSelectedCount();
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View view1 = inflater.inflate(R.layout.drag_shadow, null);
                    Button buttonCount = (Button) view1.findViewById(R.id.buttonCount);
                    buttonCount.setText("" + count);

                    View.DragShadowBuilder shadowBuilder = new MyDragShadowBuilder(view, count);
                    view.startDrag(data, shadowBuilder, view, 0);
                    return true;
                }
                return true;
            }
        });*/

        recyclerViewFileList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int event = motionEvent.getActionMasked();

                long timeElapsed = System.currentTimeMillis() - mLongPressedTime;
                Logger.log("TAG", "On item touch time Elapsed" + timeElapsed);
                if (mStartDrag && event == MotionEvent.ACTION_MOVE && mLongPressedTime != 0) {

                    if (timeElapsed > 1000) {
                        mLongPressedTime = 0;
                        mStartDrag = false;

                        View top = recyclerViewFileList.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
                        int position = recyclerViewFileList.getChildAdapterPosition(top);

                        Intent intent = new Intent();

                        intent.putExtra(FileConstants.KEY_PATH, fileInfoList.get(position).getFilePath());
                        ClipData data = ClipData.newIntent("", intent);
                        int count = fileListAdapter
                                .getSelectedCount();
                        View.DragShadowBuilder shadowBuilder = new MyDragShadowBuilder(view, count);
                        view.startDrag(data, shadowBuilder, view, 0);
//                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                    View view1 = inflater.inflate(R.layout.drag_shadow, null);
//                    Button buttonCount = (Button) view1.findViewById(R.id.buttonCount);
                        //                    textView.setLayoutParams(new LinearLayout.LayoutParams(100,100));
//                    buttonCount.setText("" + count);


                /*    if (gestureDetector.onTouchEvent(motionEvent)) {
                        Logger.log("TAG", "On item touch inside");
                        return false;
                    }*/
                    }
                }
                return false;
//                return false;
            }
        });


    }


    private void initializeViews() {
        recyclerViewFileList = (RecyclerView) root.findViewById(R.id.recyclerViewFileList);
        mTextEmpty = (TextView) root.findViewById(textEmpty);
        preference = new SharedPreferenceWrapper();
        recyclerViewFileList.setOnDragListener(new myDragEventListener());
        gestureDetector = new GestureDetectorCompat(getActivity(),
                new GestureListener());

    }

    private void handleCategoryItemClick(int position) {
        switch (mCategory) {
            case 0:
                // For file, open external apps based on Mime Type
                if (!fileInfoList.get(position).isDirectory()) {
                    String extension = fileInfoList.get(position).getExtension().toLowerCase();
                    if (extension.equalsIgnoreCase("zip")) {
//                        showZipFileOptions(fileInfoList.get(position).getFilePath(),mFilePath);
                        String path = fileInfoList.get(position).getFilePath();
                        Bundle bundle = new Bundle();
                        bundle.putString(FileConstants.KEY_PATH, path);
                        bundle.putInt(BaseActivity.ACTION_VIEW_MODE, mViewMode);
                        bundle.putBoolean(FileConstants.KEY_ZIP, true);
                        Intent intent = new Intent(getActivity(), BaseActivity.class);
                        if (FileListFragment.this instanceof FileListDualFragment) {
                            intent.setAction(BaseActivity.ACTION_DUAL_VIEW_FOLDER_LIST);
                            intent.putExtra(BaseActivity.ACTION_DUAL_PANEL, true);
                        } else {
                            intent.setAction(BaseActivity.ACTION_VIEW_FOLDER_LIST);
                            intent.putExtra(BaseActivity.ACTION_DUAL_PANEL, false);
                        }

                        intent.putExtras(bundle);
                        startActivity(intent);


                    } else {
                        FileUtils.viewFile(getActivity(), fileInfoList.get(position).getFilePath(), fileInfoList.get
                                (position).getExtension());
                    }

                } else {
                    Bundle bundle = new Bundle();
                    String path = fileInfoList.get(position).getFilePath();
                    bundle.putString(FileConstants.KEY_PATH, path);
                    bundle.putInt(BaseActivity.ACTION_VIEW_MODE, mViewMode);
                    Intent intent = new Intent(getActivity(), BaseActivity.class);
                    if (FileListFragment.this instanceof FileListDualFragment) {
                        intent.setAction(BaseActivity.ACTION_DUAL_VIEW_FOLDER_LIST);
                        intent.putExtra(BaseActivity.ACTION_DUAL_PANEL, true);
                    } else {
                        intent.setAction(BaseActivity.ACTION_VIEW_FOLDER_LIST);
                        intent.putExtra(BaseActivity.ACTION_DUAL_PANEL, false);
                    }

                    intent.putExtras(bundle);
                    startActivity(intent);
                }
                break;
            case 1:
            case 2:
            case 3:
            case 4:
                FileUtils.viewFile(getActivity(), fileInfoList.get(position).getFilePath(), fileInfoList.get(position)
                        .getExtension());

                break;

        }
    }


    private void itemClickActionMode(int position) {
        fileListAdapter.toggleSelection(position);
        boolean hasCheckedItems = fileListAdapter.getSelectedCount() > 0;
        ActionMode actionMode = ((BaseActivity) getActivity()).getActionMode();
        if (hasCheckedItems && actionMode == null) {
            // there are some selected items, start the actionMode
            ((BaseActivity) getActivity()).startActionMode();
            if (FileListFragment.this instanceof FileListDualFragment) {
                mIsDualActionModeActive = true;
            } else {
                mIsDualActionModeActive = false;
            }
            ((BaseActivity) getActivity()).setFileList(fileInfoList);
        } else if (!hasCheckedItems && actionMode != null) {
            // there no selected items, finish the actionMode
            actionMode.finish();
        }
        if (((BaseActivity) getActivity()).getActionMode() != null) {
            SparseBooleanArray checkedItemPos = fileListAdapter.getSelectedItemPositions();
            ((BaseActivity) getActivity()).setSelectedItemPos(checkedItemPos);
            ((BaseActivity) getActivity()).getActionMode().setTitle(String.valueOf(fileListAdapter.getSelectedCount()
            ) + " selected");
        }
    }

    public void toggleSelectAll(boolean selectAll) {
        fileListAdapter.clearSelection();
        for (int i = 0; i < fileListAdapter.getItemCount(); i++) {
            fileListAdapter.toggleSelectAll(i, selectAll);
        }
        SparseBooleanArray checkedItemPos = fileListAdapter.getSelectedItemPositions();
        ((BaseActivity) getActivity()).setSelectedItemPos(checkedItemPos);

        ((BaseActivity) getActivity()).getActionMode().setTitle(String.valueOf(fileListAdapter.getSelectedCount()
        ) + " selected");
        fileListAdapter.notifyDataSetChanged();

    }

    public void clearSelection() {
        fileListAdapter.removeSelection();

    }

    public void refreshList() {
        Bundle args = new Bundle();
        args.putString(FileConstants.KEY_PATH, mFilePath);
        getLoaderManager().restartLoader(LOADER_ID, args, this);
    }


    @Override
    public Loader<ArrayList<FileInfo>> onCreateLoader(int id, Bundle args) {
        fileInfoList = new ArrayList<>();
        String path = args.getString(FileConstants.KEY_PATH);
        return new FileListLoader(getContext(), path, mCategory);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<FileInfo>> loader, ArrayList<FileInfo> data) {
//        Log.d("TAG", "on onLoadFinished--" + data.size());
        if (data != null) {
            Log.d("TAG", "on onLoadFinished--" + data.size());
            if (!data.isEmpty()) {
                fileInfoList = data;
                fileListAdapter.updateAdapter(fileInfoList);
                recyclerViewFileList.setHasFixedSize(true);
                RecyclerView.LayoutManager llm;
                if (mViewMode == FileConstants.KEY_LISTVIEW) {
                    llm = new LinearLayoutManager(getActivity());
                } else {
                    llm = new GridLayoutManager(getActivity(), getResources().getInteger(R
                            .integer.grid_columns));

                }
                llm.setAutoMeasureEnabled(false);
                recyclerViewFileList.setLayoutManager(llm);
                recyclerViewFileList.setItemAnimator(new DefaultItemAnimator());
                recyclerViewFileList.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager
                        .VERTICAL));
         /*       ItemTouchHelper.Callback callback = new SimpleItemTouchHelper();
                ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
                mItemTouchHelper.attachToRecyclerView(recyclerViewFileList);*/

                ((BaseActivity) getActivity()).setFileListAdapter(fileListAdapter);

                if (mTextEmpty.getVisibility() == View.VISIBLE) {
                    mTextEmpty.setVisibility(View.GONE);
                }
            } else {
                mTextEmpty.setVisibility(View.VISIBLE);
            }
        }

    }

    @Override
    public void onLoaderReset(Loader<ArrayList<FileInfo>> loader) {

    }


    public BitmapDrawable writeOnDrawable(String text) {

//        Bitmap bm = BitmapFactory.decodeResource(getResources(), drawableId).copy(Bitmap.Config.ARGB_8888, true);
        Bitmap bm = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        bm.eraseColor(ContextCompat.getColor(getActivity(), R.color.colorPrimary));

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        int countFont = getResources()
                .getDimensionPixelSize(R.dimen.drag_shadow_font);
        paint.setTextSize(countFont);

        Canvas canvas = new Canvas(bm);
        int strLength = (int) paint.measureText(text);
        int x = bm.getWidth()/2 - strLength;

        // int y = s.titleOffset;
        int y = (bm.getHeight() - countFont) / 2;
//        drawText(canvas, x, y, title, labelWidth - s.leftMargin - x
//                - s.titleRightMargin, mTitlePaint);

        canvas.drawText(text, x, y - paint.getFontMetricsInt().ascent, paint);
//        canvas.drawText(text, bm.getWidth() / 2, bm.getHeight() / 2, paint);

        return new BitmapDrawable(getActivity().getResources(), bm);
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_MIN_DISTANCE = 100;
        private static final int SWIPE_THRESHOLD_VELOCITY = 100;
        protected MotionEvent mLastOnDownEvent = null;

        @Override
        public boolean onFling(
                MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            Logger.log("TAG", "Gesture --e1=" + e1 + "e2==" + e2);
       /*     if (e1==null)
                e1 = mLastOnDownEvent;
            if (e1==null || e2==null)
                return false;*/
           /* if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {*/


            Intent intent = new Intent();

            intent.putExtra(FileConstants.KEY_PATH, fileInfoList.get(0).getFilePath());
            ClipData data = ClipData.newIntent("", intent);
            int count = fileListAdapter
                    .getSelectedCount();
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view1 = inflater.inflate(R.layout.drag_shadow, null);
//            Button buttonCount = (Button) view1.findViewById(buttonCount);
            //                    textView.setLayoutParams(new LinearLayout.LayoutParams(100,100));
//            buttonCount.setText("" + count);

//                View.DragShadowBuilder shadowBuilder = new MyDragShadowBuilder(view, count);
//                view.startDrag(data, shadowBuilder, view, 0);

            return true;
//            }
//            return false;

        }

        @Override
        public boolean onDown(MotionEvent e) {
            // always return true since all gestures always begin with onDown
            // and<br>
            // if this returns false, the framework won't try to pick up onFling
            // for example.
            mLastOnDownEvent = e;
            Logger.log("TAG", "Gesture ondown");
            return true;
        }

    }

    private class MyDragShadowBuilder extends View.DragShadowBuilder {

        // The drag shadow image, defined as a drawable thing
        private Drawable shadow;
        private Point mScaleFactor;

        // Defines the constructor for myDragShadowBuilder
        public MyDragShadowBuilder(View v, int count) {

            // Stores the View parameter passed to myDragShadowBuilder.
            super(v);

            // Creates a draggable image that will fill the Canvas provided by the system.
//            shadow = v
//            shadow = new TextDrawable(getActivity(),"ABCDDDDDDDDDDDDDDDDDD");

//            shadow = new ColorDrawable(Color.LTGRAY);
            shadow = writeOnDrawable("" + count);
            //ColorDrawable(Color.RED);

        }

        // Defines a callback that sends the drag shadow dimensions and touch point back to the
        // system.
        @Override
        public void onProvideShadowMetrics(Point size, Point touch) {
            // Defines local variables
            int width, height;

            // Sets the width of the shadow to half the width of the original View
            width = getView().getWidth() / 6;
//            width = 100;
            Log.d("TAG", "width=" + width);

            // Sets the height of the shadow to half the height of the original View
            height = getView().getHeight() / 6;
//            height = 100;

            Log.d("TAG", "height=" + height);


            // The drag shadow is a ColorDrawable. This sets its dimensions to be the same as the
            // Canvas that the system will provide. As a result, the drag shadow will fill the
            // Canvas.
            shadow.setBounds(0, 0, width, height);

            // Sets the size parameter's width and height values. These get back to the system
            // through the size parameter.
            size.set(width, height);
            // Sets size parameter to member that will be used for scaling shadow image.
            mScaleFactor = size;

            // Sets the touch point's position to be in the middle of the drag shadow
//            touch.set(width / 4, height / 4);
        }

        // Defines a callback that draws the drag shadow in a Canvas that the system constructs
        // from the dimensions passed in onProvideShadowMetrics().
        @Override
        public void onDrawShadow(Canvas canvas) {

            // Draws the ColorDrawable in the Canvas passed in from the system.
            shadow.draw(canvas);
//            canvas.scale(mScaleFactor.x/(float)getView().getWidth(), mScaleFactor.y/(float)getView().getHeight());
/*            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPaint(paint);

            paint.setColor(Color.BLACK);
            paint.setTextSize(20);
            canvas.drawText("10", 10, 25, paint);*/
//            getView().draw(canvas);
        }
    }

    private void showDragDialog(final String sourcePath, final String destinationDir) {
        final Dialog dialog = new Dialog(
                getActivity());
        dialog.setContentView(R.layout.dialog_drag);
        dialog.setCancelable(true);


        final RadioButton radioButtonCopy = (RadioButton) dialog.findViewById(R.id
                .radioButtonCopy);
        final RadioButton radioButtonMove = (RadioButton) dialog.findViewById(R.id
                .radioButtonMove);
        RadioGroup radioGroupDrag = (RadioGroup) dialog.findViewById(R.id.radioGroupAction);
        Button buttonOk = (Button) dialog.findViewById(R.id.buttonOk);
        Button buttonCancel = (Button) dialog.findViewById(R.id.buttonCancel);


        dialog.show();
        final boolean isMoveOperation = radioButtonMove.isChecked();

        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileUtils.copyToDirectory(getActivity(), sourcePath, destinationDir,
                        isMoveOperation, action, null);
                dialog.dismiss();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    protected class myDragEventListener implements View.OnDragListener {

        // This is the method that the system calls when it dispatches a drag event to the
        // listener.
        public boolean onDrag(View v, DragEvent event) {

            // Defines a variable to store the action type for the incoming event
            final int action = event.getAction();
            int pos = 0;

            // Handles each of the expected events
            switch (action) {

                case DragEvent.ACTION_DRAG_STARTED:

                    Log.d("TAG", "DRag started");
                    mIsDragInProgress = true;

                    // Determines if this View can accept the dragged data
                    if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_INTENT)) {

                        // As an example of what your application might do,
                        // applies a blue color tint to the View to indicate that it can accept
                        // data.
//                        v.setColorFilter(Color.BLUE);

                        // Invalidate the view to force a redraw in the new tint
//                        v.invalidate();

                        // returns true to indicate that the View can accept the dragged data.
                        return true;

                    }

                    // Returns false. During the current drag and drop operation, this View will
                    // not receive events again until ACTION_DRAG_ENDED is sent.
                    return false;

                case DragEvent.ACTION_DRAG_ENTERED:
                    Log.d("TAG", "DRag entered");


                    // Applies a green tint to the View. Return true; the return value is ignored.

//                    v.setColorFilter(Color.GREEN);

                    // Invalidate the view to force a redraw in the new tint
                    v.invalidate();

                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:
//                    View onTopOf = recyclerViewFileList.findChildViewUnder(event.getX(), event.getY());
//                     pos = recyclerViewFileList.getChildAdapterPosition(onTopOf);
//                    Logger.log("TAG","drag pos="+pos);

//                    list.add(i, list.remove(prevPos));
                    // Ignore the event
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    Log.d("TAG", "DRag exit");
                    mIsDragInProgress = false;

                    // Re-sets the color tint to blue. Returns true; the return value is ignored.
//                    v.setColorFilter(Color.BLUE);

                    // Invalidate the view to force a redraw in the new tint
                    v.invalidate();

                    return true;

                case DragEvent.ACTION_DROP:
//                    Log.d("TAG","DRag drop"+pos);

                    View top = recyclerViewFileList.findChildViewUnder(event.getX(), event.getY());
                    int position = recyclerViewFileList.getChildAdapterPosition(top);
                    Logger.log("TAG", "DROP new pos=" + position);

                    // Gets the item containing the dragged data
                    ClipData.Item item = event.getClipData().getItemAt(0);

                    // Gets the text data from the item.
                    Intent dragData = item.getIntent();
                    String path = dragData.getStringExtra(FileConstants.KEY_PATH);
                    String destinationDir = fileInfoList.get(position).getFilePath();
                    Logger.log("TAG", "Source=" + path + "Dest=" + destinationDir);
                    if (!destinationDir.equals(path)) {

                        showDragDialog(path, destinationDir);
                    }
                    // Displays a message containing the dragged data.
                    Toast.makeText(getActivity(), "Dragged data is " + path, Toast
                            .LENGTH_LONG);


                    // Turns off any color tints
//                    v.clearColorFilter();

                    // Invalidates the view to force a redraw
//                    v.invalidate();

                    // Returns true. DragEvent.getResult() will return true.
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:

                    Log.d("TAG", "DRag end");
                    mIsDragInProgress = false;


                    // Turns off any color tinting
//                    v.clearColorFilter();

                    // Invalidates the view to force a redraw
                    v.invalidate();

                    // Does a getResult(), and displays what happened.
                    if (event.getResult()) {
                        Toast.makeText(getActivity(), "The drop was handled.", Toast.LENGTH_LONG);

                    } else {
                        Toast.makeText(getActivity(), "The drop didn't work.", Toast.LENGTH_LONG);

                    }

                    // returns true; the value is ignored.
                    return true;

                // An unknown action type was received.
                default:
                    Log.e("DragDrop Example", "Unknown action type received by OnDragListener.");
                    break;
            }

            return false;
        }
    }

    ;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(FileListFragment.this.getClass().getSimpleName(), "On Create options " +
                "Fragment=");

        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_base, menu);
        mSearchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        setupSearchView();

    }

    private void setupSearchView() {
//        mSearchView.setIconifiedByDefault(true);
        // Disable full screen keyboard in landscape
        mSearchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mSearchView.setOnQueryTextListener(this);
//        mSearchView.setSubmitButtonEnabled(true);
//        mSearchView.setQueryHint("Search Here");
    }

    @Override
    public boolean onQueryTextChange(String query) {
        fileListAdapter.filter(query);
        // Here is where we are going to implement our filter logic
/*        final List<FileInfo> filteredModelList = filter(fileInfoList, query);
        fileListAdapter.animateTo(filteredModelList);
        recyclerViewFileList.scrollToPosition(0);*/
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
//        fileListAdapter.filter(query);
/*        mSearchView.clearFocus();
        MenuItemCompat.collapseActionView(mSearchItem);*/
        return false;
    }

/*    private List<FileInfo> filter(List<FileInfo> models, String query) {
        query = query.toLowerCase();

        final List<FileInfo> filteredModelList = new ArrayList<>();
        for (FileInfo model : models) {
            final String text = model.getFileName().toLowerCase();
            if (text.contains(query)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }*/


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_paste:
/*                    pasteOperationCleanUp();
                    if (mSelectedItemPositions != null && mSelectedItemPositions.size() > 0) {
                        for (int i = 0; i < mSelectedItemPositions.size(); i++) {
                            checkIfFileExists(mFileList.get(mSelectedItemPositions.keyAt(i)).getFilePath(), new File
                                    (mCurrentDir));
                        }
                        if (!isPasteConflictDialogShown) {
                            callAsyncTask();
                        } else {
                            showDialog(tempSourceFile.get(0));
                            isPasteConflictDialogShown = false;
                        }


                    }*/
                break;


            case R.id.action_view_list:
                if (mViewMode != FileConstants.KEY_LISTVIEW) {
                    mViewMode = FileConstants.KEY_LISTVIEW;
                    preference.savePrefs(getActivity(), mViewMode);
                    switchView();
                }
                break;
            case R.id.action_view_grid:
                if (mViewMode != FileConstants.KEY_GRIDVIEW) {
                    mViewMode = FileConstants.KEY_GRIDVIEW;
                    preference.savePrefs(getActivity(), mViewMode);
                    switchView();
                }
                break;

            case R.id.action_sort_name_asc:
                sortFiles(fileInfoList, FileConstants.KEY_SORT_NAME);
                fileListAdapter.notifyDataSetChanged();
                break;
            case R.id.action_sort_name_desc:
                sortFiles(fileInfoList, FileConstants.KEY_SORT_NAME_DESC);
                fileListAdapter.notifyDataSetChanged();
                break;

            case R.id.action_sort_type_asc:
                sortFiles(fileInfoList, FileConstants.KEY_SORT_TYPE);
                fileListAdapter.notifyDataSetChanged();
                break;

            case R.id.action_sort_type_desc:
                sortFiles(fileInfoList, FileConstants.KEY_SORT_TYPE_DESC);
                fileListAdapter.notifyDataSetChanged();
                break;

            case R.id.action_sort_size_asc:
                sortFiles(fileInfoList, FileConstants.KEY_SORT_SIZE);
                fileListAdapter.notifyDataSetChanged();

                break;

            case R.id.action_sort_size_desc:
                sortFiles(fileInfoList, FileConstants.KEY_SORT_SIZE_DESC);
                fileListAdapter.notifyDataSetChanged();

                break;
            case R.id.action_sort_date_asc:
                sortFiles(fileInfoList, FileConstants.KEY_SORT_DATE);
                fileListAdapter.notifyDataSetChanged();
                break;
            case R.id.action_sort_date_desc:
                sortFiles(fileInfoList, FileConstants.KEY_SORT_DATE_DESC);
                fileListAdapter.notifyDataSetChanged();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void switchView() {
        Bundle bundle = new Bundle();
        bundle.putString(FileConstants.KEY_PATH, mFilePath);
        Intent intent = new Intent(getActivity(), BaseActivity.class);
        if (FileListFragment.this instanceof FileListDualFragment) {
            intent.setAction(BaseActivity.ACTION_DUAL_VIEW_FOLDER_LIST);
            intent.putExtra(BaseActivity.ACTION_DUAL_PANEL, true);

        } else {
            intent.setAction(BaseActivity.ACTION_VIEW_FOLDER_LIST);
            intent.putExtra(BaseActivity.ACTION_DUAL_PANEL, false);
            intent.putExtra(ACTION_VIEW_MODE, mViewMode);
            intent.putExtra(FileConstants.KEY_CATEGORY, mCategory);
        }

        intent.putExtras(bundle);
        startActivity(intent);
    }


    private void sortFiles(ArrayList<FileInfo> files, int sortMode) {

        switch (sortMode) {
            case 0:
                Collections.sort(files, FileUtils.comparatorByName);
                break;
            case 1:
                Collections.sort(files, FileUtils.comparatorByNameDesc);
                break;
            case 2:
                Collections.sort(files, FileUtils.comparatorByType);
                break;
            case 3:
                Collections.sort(files, FileUtils.comparatorByTypeDesc);
                break;
            case 4:
                Collections.sort(files, FileUtils.comparatorBySize);
                break;
            case 5:
                Collections.sort(files, FileUtils.comparatorBySizeDesc);
                break;
            case 6:
                Collections.sort(files, FileUtils.comparatorByDate);
                break;
            case 7:
                Collections.sort(files, FileUtils.comparatorByDateDesc);
                break;

        }
    }

    @Override
    public void onDestroy() {
//        Log.d("TAG", "on onDestroy--Fragment");
        super.onDestroy();

    }
}
