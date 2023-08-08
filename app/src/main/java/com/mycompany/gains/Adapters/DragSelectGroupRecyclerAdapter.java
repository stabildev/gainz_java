package com.mycompany.gains.Adapters;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.mycompany.gains.Activities.WorkoutEditorOld.adapters.ItemTouchHelper.ItemTouchHelperAdapter;
import com.mycompany.gains.Activities.WorkoutEditorOld.adapters.ItemTouchHelper.ItemTouchHelperViewHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class DragSelectGroupRecyclerAdapter<VH extends DragSelectGroupRecyclerAdapter.BaseViewHolder>
        extends RecyclerView.Adapter<VH> implements ItemTouchHelperAdapter {

    private SparseBooleanArray mSelectedItems;
    private boolean mSelectMode = false;
    private boolean mDragMode = false;
    private int threshold;

    private List<Integer> mSelectedGroups;
    private Map<Integer, List<Integer>> mSelectedSubItems;

    private DragSelectListener mListener;

    public enum SelectionType {
        GROUP_SELECTION,
        ITEM_SELECTION,
        NULL
    }

    private SelectionType selectionType = SelectionType.NULL;

    public interface DragSelectListener {
        void onSelectionChanged(int groupSelectionSize, int itemSelectionSize);
        void onClick(int groupPosition, int subPosition);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
        void onItemMove(int fromPosition, int toPosition);
        void onItemMoved(int fromPosition, int toPosition);
        void onItemDismiss(int position);
    }

    public DragSelectGroupRecyclerAdapter(Context context, DragSelectListener listener) {
        this(context, listener, .01f);
    }

    public DragSelectGroupRecyclerAdapter(Context context, DragSelectListener listener, float threshold) {
        super();
        mSelectedItems = new SparseBooleanArray();
        mSelectedGroups = new ArrayList<>();
        mSelectedSubItems = new HashMap<>();

        try {
            mListener = listener ;
        } catch (ClassCastException e) {
            throw new ClassCastException(listener.toString()
                    + " must implement DragSelectListener");
        }

        //setHasStableIds(true);

        // get screen size. movement threshold is fraction of screen height.
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int height;

        if (Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            height = size.y;
        } else
            height = display.getHeight();

        this.threshold = (int) (threshold * height);
    }

    public abstract static class BaseViewHolder extends RecyclerView.ViewHolder {
        public BaseViewHolder(View v) {
            super(v);
        }

        public abstract boolean isDragDropSelectEnabled();
    }

    public abstract static class ViewHolder extends BaseViewHolder
            implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener,
            ItemTouchHelperViewHolder {
        boolean isLongClicked = false;
        boolean isMoving = false;
        int y0, y1;
        int threshold;
        DragSelectListener listener;

        interface DragSelectListener {
            void onStartDrag(View v);
            void onLongClick(int groupPosition, int subPosition, View v);
            void onClick(int groupPosition, int subPosition, View v);
        }

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            v.setOnTouchListener(this);
        }

        public void setDragSelectListener(DragSelectListener listener, int threshold) {
            this.listener = listener;
            this.threshold = threshold;
        }

        public void onSubItemClick(int subPosition, View v) {
            listener.onClick(getAdapterPosition(), subPosition, v);
        }

        public void onSubItemLongClick(int subPosition, View v) {
            isLongClicked = true;
            listener.onLongClick(getAdapterPosition(), subPosition, v);
        }

        @Override
        public void onClick(View v) {
            listener.onClick(getAdapterPosition(), -1, itemView);
        }

        @Override
        public boolean onLongClick(View v) {
            isLongClicked = true;
            listener.onLongClick(getAdapterPosition(), -1, itemView);
            return true;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            // on ACTION_DOWN initialize variables and don't consume event
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                isLongClicked = false;
                isMoving = false;
                y0 = (int) event.getY();
                y1 = y0;
            }
            // on ACTION_MOVE get new y coordinate
            else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                y1 = (int) event.getY();

                // if item is clicked long enough AND not moving yet AND the movement higher
                // than the treshold, start moving the item
                if (isLongClicked && Math.abs(y1 - y0) > threshold && !isMoving) {
                    isMoving = true;
                    listener.onStartDrag(this.itemView);
                }
            }
            return false;
        }
    }

    public void onBindViewHolder(final VH viewHolder, int position) {

        ViewHolder holder = (ViewHolder) viewHolder;

        // activated after long click, selected while being dragged
        holder.itemView.setActivated(mSelectedItems.get(position));

        // set ViewHolder.DragSelectListener
        holder.setDragSelectListener(new ViewHolder.DragSelectListener() {
            @Override
            public void onClick(int groupPosition, int subPosition, View v) {
                if (mSelectMode && selectionType == SelectionType.GROUP_SELECTION)
                    toggleGroupSelection(groupPosition, v);
                else if (mSelectMode && selectionType == SelectionType.ITEM_SELECTION)
                    toggleItemSelection(groupPosition, subPosition, v);
                else
                    mListener.onClick(groupPosition, subPosition);
            }

            @Override
            public void onLongClick(int groupPosition, int subPosition, View v) {
                if (!mSelectMode && !mDragMode) {
                    mSelectMode = true;
                    if (subPosition != -1) {
                        selectionType = SelectionType.ITEM_SELECTION;
                        toggleItemSelection(groupPosition, subPosition, v);
                    } else {
                        selectionType = SelectionType.GROUP_SELECTION;
                        toggleGroupSelection(groupPosition, v);
                    }
                }
            }

            @Override
            public void onStartDrag(View v) {
                if (mSelectedItems.size() < 2 && v.isActivated()) {
                    mDragMode = true;
                    v.setActivated(false);
                    mListener.onStartDrag(viewHolder);
                }
            }
        }, threshold);
    }

    private void stopSelection() {
        mSelectMode = false;
        selectionType = SelectionType.NULL;
    }

    public void toggleGroupSelection(int groupPosition, View v) {
        // deactivate item if active
        if (mSelectedItems.get(groupPosition, false)) {
            mSelectedItems.delete(groupPosition);
            mListener.onSelectionChanged(mSelectedItems.size(), mSelectedSubItems.size());

            // disable selection mode if all are deselected
            if (mSelectedItems.size() == 0)
                stopSelection();
        }
        // activate item if inactive
        else {
            mSelectedItems.put(groupPosition, true);
            mListener.onSelectionChanged(mSelectedItems.size(), mSelectedSubItems.size());
        }

        // toggle activation status in ViewHolder
        // done elsewhere for dragged item
        if (v != null)
            v.setActivated(!v.isActivated());
    }

    public void toggleItemSelection(int groupPosition, int subPosition, View v) {
        if (mSelectedSubItems.containsKey(groupPosition) && mSelectedSubItems.get(groupPosition).contains(subPosition)) {
            mSelectedSubItems.get(groupPosition).remove(new Integer(subPosition));
            if (mSelectedSubItems.get(groupPosition).isEmpty())
                mSelectedSubItems.remove(groupPosition);
        } else if (mSelectedSubItems.containsKey(groupPosition))
            mSelectedSubItems.get(groupPosition).add(subPosition);
        else
            mSelectedSubItems.put(groupPosition, Collections.singletonList(subPosition));

        v.setActivated(!v.isActivated());
        mListener.onSelectionChanged(mSelectedSubItems.keySet().size(), mSelectedSubItems.values().size());
    }

    public void clearSelection() {
        mSelectedItems.clear();
        mSelectedSubItems.clear();
        mListener.onSelectionChanged(0, 0);
        stopSelection();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    public List<Integer> getSelectedGroups() {
        List<Integer> items = new ArrayList<>(mSelectedItems.size());
        for (int i = 0; i < mSelectedItems.size(); i++)
            items.add(mSelectedItems.keyAt(i));
        return items;
    }

    // ItemTouchHelperAdapter methods

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        // update selection state
        boolean from = mSelectedItems.get(fromPosition, false);
        boolean to = mSelectedItems.get(toPosition, false);

        if (from)
            mSelectedItems.put(toPosition, true);
        else if (to)
            mSelectedItems.delete(toPosition);

        if (to)
            mSelectedItems.put(fromPosition, true);
        else if (from)
            mSelectedItems.delete(fromPosition);

        mListener.onItemMove(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onActionFinished() {
        mDragMode = false;
        if (mSelectMode) {
            mSelectedItems.clear();
            mSelectedSubItems.clear();
            mListener.onSelectionChanged(0, 0);
            stopSelection();
        }
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        mListener.onItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
        clearSelection();
        mListener.onItemDismiss(position);
        notifyItemRemoved(position);
    }

    public SelectionType getSelectionType() {
        return selectionType;
    }
}
