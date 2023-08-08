package com.mycompany.gains.Adapters;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import com.mycompany.gains.Stuff.Misc;
import com.mycompany.gains.Activities.WorkoutEditorOld.adapters.ItemTouchHelper.ItemTouchHelperAdapter;
import com.mycompany.gains.Activities.WorkoutEditorOld.adapters.ItemTouchHelper.ItemTouchHelperViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class DragSelectRecyclerAdapter2<VH extends DragSelectRecyclerAdapter2.BaseViewHolder>
        extends RecyclerView.Adapter<VH> implements ItemTouchHelperAdapter {

    private enum SelectionMode {
        ITEM_SELECTION,
        SUB_ITEM_SELECTION,
        NONE
    }

    protected Context mContext;

    private List<Integer> mSelectedItems;
    private Map<Integer, View> mSelectedViews;
    private int mSelectedSubItem = -1;

    private SelectionMode mSelectMode = SelectionMode.NONE;
    private boolean mDragMode = false;
    private int threshold;

    private DragSelectListener mListener;

    public interface DragSelectListener {
        void onSelectionChanged(int selectionSize, int subSelectionSize);
        void onClick(int position, int subPosition);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
        void onItemMove(int fromPosition, int toPosition);
        void onItemMoved(int fromPosition, int toPosition);
        void onItemDismiss(int position);
    }

    public DragSelectRecyclerAdapter2(Context context) {
        this(context, .01f);
    }

    public DragSelectRecyclerAdapter2(Context context, float threshold) {
        super();
        mContext = context;
        mSelectedItems = new ArrayList<>();
        mSelectedViews = new HashMap<>();

        //setHasStableIds(true);

        // get screen size. movement threshold is fraction of screen height.
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int height = Misc.getDisplayHeight(mContext);

        this.threshold = (int) (threshold * height);
    }

    public void setDragSelectListener(DragSelectListener listener) {
        try {
            mListener = listener ;
        } catch (ClassCastException e) {
            throw new ClassCastException(listener.toString()
                    + " must implement DragSelectListener");
        }
    }

    public abstract static class BaseViewHolder extends RecyclerView.ViewHolder {
        public BaseViewHolder(View v) {
            super(v);
        }

        public abstract boolean isDragDropSelectEnabled();
    }

    public abstract static class ItemViewHolder extends BaseViewHolder
            implements View.OnTouchListener, View.OnClickListener, View.OnLongClickListener,
            ItemTouchHelperViewHolder {
        boolean isLongClicked = false;
        boolean isLongerClicked = false;
        boolean isMoving = false;
        int y0, y1;
        int threshold;
        DragSelectListener listener;

        final Handler handler = new Handler();
        LongPressTimeOut mLongPressTimeOut = new LongPressTimeOut(false);

        private class LongPressTimeOut implements Runnable {
            int position;
            int subPosition;
            boolean longerPress;
            View v;
            
            LongPressTimeOut(boolean longerPress) {
                this.longerPress = longerPress;
            }

            public void init(int pos, int subPos, View view) {
                position = pos;
                subPosition = subPos;
                v = view;
            }

            public void run() {
                if (!isMoving) {
                    if (!isLongClicked) {
                        isLongClicked = true;
                        listener.onLongClick(position, subPosition, v);
                        if (subPosition != -1)
                            handler.postDelayed(this, (long) (ViewConfiguration.getLongPressTimeout()/2));
                    } else if (!isLongerClicked) {
                        isLongerClicked = true;
                        listener.onLongClick(position, -1, itemView);
                    }
                }
            }
        }

        interface DragSelectListener {
            void onStartDrag(View v);
            void onLongClick(int position, int subPosition, View v);
            void onClick(int position, int subPosition, View v);
        }

        public ItemViewHolder(View v) {
            super(v);
        }

        public void setDragSelectListener(DragSelectListener listener, int threshold) {
            this.listener = listener;
            this.threshold = threshold;
        }

        // enable drag and drop, activate DragSelectListener in onBindViewHolder
        @Override
        public boolean isDragDropSelectEnabled() {
            return true;
        }
        
        // should return -1 for single items  and subPosition for multi items
        public abstract int getSubPosition(View v);

        @Override
        public void onClick(View v) {
            listener.onClick(getAdapterPosition(), getSubPosition(v), v);
        }

        @Override
        public boolean onLongClick(View v) {
            if (!isMoving && !isLongClicked) {
                listener.onLongClick(getAdapterPosition(), getSubPosition(v), v);
                isLongClicked = true;

                // initiate timeout for longer click if not a single item
                if (getSubPosition(v) != -1) {
                    mLongPressTimeOut.init(getAdapterPosition(), -1, v);
                    handler.postDelayed(mLongPressTimeOut, (long) (1.2 * ViewConfiguration.getLongPressTimeout()));
                }
                return true;
            } else
                return false;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // initialize variables
                    isLongClicked = false;
                    isLongerClicked = false;
                    isMoving = false;
                    y0 = (int) event.getY();
                    y1 = y0;

                    // start long click countdown
                    // the listener will receive a long click event after the system's default timeout.
                    // if subPosition is not -1, there will be another long click event after the same
                    // delay, this time performing a long click (i.e. selecting) the whole itemview
                    /*mLongPressTimeOut.init(getAdapterPosition(), getSubPosition(v), v);
                    handler.postDelayed(mLongPressTimeOut,
                            ViewConfiguration.getLongPressTimeout());*/
                    break;

                case MotionEvent.ACTION_MOVE:
                    // update position (movement distance)
                    y1 = (int) event.getY();

                    // if item is pressed long enough, not moving yet and dragged beyond the threshold,
                    // initiate item drag
                    if (isLongClicked && !isMoving && Math.abs(y1 - y0) > threshold) {
                        handler.removeCallbacks(mLongPressTimeOut);
                        isMoving = true;
                        listener.onStartDrag(v);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    // stop long press timer
                    handler.removeCallbacks(mLongPressTimeOut);

                    // if not long pressed and not moving, a click was performed
                    /*if (!isLongClicked && !isMoving) {
                        listener.onClick(getAdapterPosition(), getSubPosition(v), v);
                    }*/
                    break;
            }
            return false;
        }
    }

    public void onBindViewHolder(final VH viewHolder, int position) {

        // this method only sets the DragSelectListener and handles activation/selection
        if (!viewHolder.isDragDropSelectEnabled())
            return;

        final ItemViewHolder holder = (ItemViewHolder) viewHolder;

        // activated after long click, selected while being dragged
        holder.itemView.setActivated(mSelectedItems.contains(position));

        // set ViewHolder.DragSelectListener
        holder.setDragSelectListener(new ItemViewHolder.DragSelectListener() {

            // called after click on item or sub item
            // subPosition is -1 for single items
            // View v can be itemView or child
            @Override
            public void onClick(int position, int subPosition, View v) {
                if (mSelectMode != SelectionMode.NONE)
                    toggleSelection(position, subPosition, v);
                else
                    mListener.onClick(position, subPosition);
            }

            // called after long click, can be followed by onRequestDrag
            // long click on sub item can be followed by long click on whole item
            @Override
            public void onLongClick(int position, int subPosition, View v) {
                if (!mDragMode) {
                    // activate selection mode if not active yet and not dragging
                    if (mSelectMode == SelectionMode.NONE)
                        // if subPosition is -1, activate group selection
                        if (subPosition == -1)
                            mSelectMode = SelectionMode.ITEM_SELECTION;
                            // else, activate sub item selection
                        else
                            mSelectMode = SelectionMode.SUB_ITEM_SELECTION;

                    // now that selection mode is activated, toggle selection for (sub / group) item
                    toggleSelection(position, subPosition, v);
                }
            }

            // called after long click if moved beyond threshold. View v is NOT always the itemView
            @Override
            public void onStartDrag(View v) {
                // start dragging only if only one item (or a sub item) is selected

                if (mSelectedItems.size() < 2 /*&& v.isActivated()*/) {
                    mDragMode = true;

                    if (mSelectMode == SelectionMode.ITEM_SELECTION && ((View) v.getParent()).isActivated())
                        ((View) v.getParent()).setActivated(false);
                    else
                        v.setActivated(false);

                    // start dragging the viewHolder
                    mListener.onStartDrag(viewHolder);
                }
            }
        }, threshold);
    }

    public void toggleSelection(int position, int subPosition, View v) {
        View parentView = v == null ? null : (View) v.getParent();
        switch (mSelectMode) {
            case ITEM_SELECTION:
                if (mSelectedItems.contains(position)) {
                    mSelectedItems.remove(new Integer(position));
                    mSelectedViews.remove(position);
                    Log.i("LEL", "remove view");
                }
                else {
                    mSelectedItems.add(position);
                }

                // toggle activation status of whole item
                if (v != null)
                    if (subPosition == -1) {
                        if (!v.isActivated()) {
                            mSelectedViews.put(position, v);
                            Log.i("LEL", "add view");
                        }
                        v.setActivated(!v.isActivated());
                    }
                    else {
                        if (!parentView.isActivated()) {
                            mSelectedViews.put(position, parentView);
                            Log.i("LEL", "add parent view");
                        }
                        parentView.setActivated(!parentView.isActivated());
                    }
                break;
            
            case SUB_ITEM_SELECTION:
                if (mSelectedItems.size() == 0 && mSelectedSubItem == -1) {
                    // no sub item is selected yet
                    mSelectedItems.add(position);
                    mSelectedSubItem = subPosition;
                    mSelectedViews.put(-1, v);

                    if (v != null)
                        v.setActivated(!v.isActivated());
                }
                else if (mSelectedItems.size() == 1 && mSelectedItems.get(0) == position && (subPosition == -1 || subPosition != mSelectedSubItem)) {
                    // the "longer press" happened or another item of the same group is selected
                    // NOW the whole group is being selected!
                    mSelectMode = SelectionMode.ITEM_SELECTION;

                    mSelectedViews.get(-1).setActivated(false);

                    mSelectedViews.put(position, (View) mSelectedViews.get(-1).getParent());
                    mSelectedViews.get(position).setActivated(true);

                    mSelectedViews.remove(-1);
                    mSelectedSubItem = -1;
                } else {
                    // the same item is being deselected or a (sub) item is already selected
                    // and now a new one is being selected. In either case stop selection
                    mSelectedItems.clear();
                    mSelectedSubItem = -1;

                    for (View view : mSelectedViews.values())
                        view.setActivated(false);
                }
        }

        if (mSelectedItems.size() == 0 && mSelectedSubItem == -1)
            mSelectMode = SelectionMode.NONE;

        mListener.onSelectionChanged(mSelectedItems.size(), mSelectedSubItem != -1 ? 1 : 0);
    }

    public void clearSelection() {
        clearSelection(true);
    }

    public void clearSelection(boolean clearViews) {
        mSelectedItems.clear();
        mSelectedSubItem = -1;
        mListener.onSelectionChanged(0, 0);
        mSelectMode = SelectionMode.NONE;
        if (clearViews) {
            for (View v : mSelectedViews.values()) {
                v.setActivated(false);
                Log.i("LEL", "deactivate view");
            }
            mSelectedViews.clear();
        }
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    public boolean isSubSelection() {
        return mSelectedSubItem != -1;
    }

    public List<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public int getSelectedSubItem() {
        return mSelectedSubItem;
    }

    // ItemTouchHelperAdapter methods

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        // update selection state
        boolean from = mSelectedItems.contains(fromPosition);
        boolean to = mSelectedItems.contains(toPosition);

        if (from)
            mSelectedItems.add(toPosition);
        else if (to)
            mSelectedItems.remove(new Integer(toPosition));

        if (to)
            mSelectedItems.add(fromPosition);
        else if (from)
            mSelectedItems.remove(new Integer(fromPosition));

        mListener.onItemMove(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onActionFinished() {
        mDragMode = false;
        if (mSelectMode != SelectionMode.NONE) {
            clearSelection(false);
        }
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        mListener.onItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
        clearSelection(false);
        mListener.onItemDismiss(position);
        notifyItemRemoved(position);
    }
}
