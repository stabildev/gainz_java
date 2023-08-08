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
import java.util.List;


public abstract class DragSelectRecyclerAdapter<VH extends DragSelectRecyclerAdapter.BaseViewHolder>
        extends RecyclerView.Adapter<VH> implements ItemTouchHelperAdapter {

    private SparseBooleanArray mSelectedItems;
    private boolean mSelectMode = false;
    private boolean mDragMode = false;
    private int threshold;

    private DragSelectListener mListener;

    public interface DragSelectListener {
        void onSelectionChanged(int selectionSize);
        void onClick(int position);
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
        void onItemMove(int fromPosition, int toPosition);
        void onItemMoved(int fromPosition, int toPosition);
        void onItemDismiss(int position);
    }

    public DragSelectRecyclerAdapter(Context context, DragSelectListener listener) {
        this(context, listener, .01f);
    }

    public DragSelectRecyclerAdapter(Context context, DragSelectListener listener, float threshold) {
        super();
        mSelectedItems = new SparseBooleanArray();

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
            void onLongClick(int position, View v);
            void onClick(int position, View v);
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

        @Override
        public void onClick(View v) {
            listener.onClick(getAdapterPosition(), itemView);
        }

        @Override
        public boolean onLongClick(View v) {
            isLongClicked = true;
            listener.onLongClick(getAdapterPosition(), itemView);
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

        public void setLongClicked(boolean longClicked) {
            this.isLongClicked = longClicked;
        }
    }

    public void onBindViewHolder(final VH viewHolder, int position) {

        ViewHolder holder = (ViewHolder) viewHolder;

        // activated after long click, selected while being dragged
        holder.itemView.setActivated(mSelectedItems.get(position));

        // set ViewHolder.DragSelectListener
        holder.setDragSelectListener(new ViewHolder.DragSelectListener() {
            @Override
            public void onClick(int position, View v) {
                if (mSelectMode)
                    toggleSelection(position, v);
                else
                    mListener.onClick(position);
            }

            @Override
            public void onLongClick(int position, View v) {
                if (!mSelectMode && !mDragMode) {
                    mSelectMode = true;
                }
                toggleSelection(position, v);
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
    }

    public void toggleSelection(int pos, View v) {
        // deactivate item if active
        if (mSelectedItems.get(pos, false)) {
            mSelectedItems.delete(pos);
            mListener.onSelectionChanged(mSelectedItems.size());

            // disable selection mode if all are deselected
            if (mSelectedItems.size() == 0)
                stopSelection();
        }
        // activate item if inactive
        else {
            mSelectedItems.put(pos, true);
            mListener.onSelectionChanged(mSelectedItems.size());
        }

        // toggle activation status in ViewHolder
        // done elsewhere for dragged item
        if (v != null)
            v.setActivated(!v.isActivated());
    }

    public void clearSelection() {
        mSelectedItems.clear();
        mListener.onSelectionChanged(0);
        stopSelection();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    public List<Integer> getSelectedItems() {
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
            mListener.onSelectionChanged(0);
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
}
