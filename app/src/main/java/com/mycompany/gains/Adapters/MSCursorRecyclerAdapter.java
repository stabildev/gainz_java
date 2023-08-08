package com.mycompany.gains.Adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


public abstract class MSCursorRecyclerAdapter<VH extends MSCursorRecyclerAdapter.ViewHolder> extends CursorRecyclerViewAdapter<VH> {
    private LinkedHashMap<Integer, Integer> mSelectedItems; // <Position, id>
    private boolean mSelectMode = false;
    private RecyclerView mRecyclerView;

    private OnSelectionListener mListener;

    public interface OnSelectionListener {
        void onSelectionChanged(int selectionSize);
        void onClick(int id);
    }

    public interface SelectionListener {
        OnSelectionListener getOnSelectionListener(Object T);
    }

    public void setOnSelectionListener(OnSelectionListener listener) {
        mListener = listener;
    }

    public MSCursorRecyclerAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        mSelectedItems = new LinkedHashMap<>();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {
        private ClickListener clickListener;
        protected int _id;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
        }

        // interface for handling clicks, both long and normal
        public interface ClickListener {
            void onClick(View v, int position, int _id, boolean isLongClick);
        }

        public void setClickListener(ClickListener clickListener) {
            this.clickListener = clickListener;
        }

        @Override
        public void onClick(View v) {
            clickListener.onClick(v, getAdapterPosition(), _id, false);
        }

        @Override
        public boolean onLongClick(View v) {
            clickListener.onClick(v, getAdapterPosition(), _id, true);
            return true;
        }
    }

    @Override
    public void onBindViewHolder(VH viewHolder, int position, Cursor cursor) {

        viewHolder._id = cursor.getInt(cursor.getColumnIndex("_id"));
        viewHolder.itemView.setActivated(mSelectedItems.containsKey(position));

        // set ClickListener
        viewHolder.setClickListener(new ViewHolder.ClickListener() {
            @Override
            public void onClick(View v, int pos, int _id, boolean isLongClick) {
                if (mSelectMode) {
                    toggleSelection(pos, _id, v);
                } else if (isLongClick) {
                    // View v at position pos is long-clicked
                    mSelectMode = true;
                    toggleSelection(pos, _id, v);
                } else {
                    mListener.onClick(_id);
                }
            }
        });
    }

    public void toggleSelection(int pos, int id, View v) {
        // if item is selected
        if (mSelectedItems.containsKey(pos)) {
            mSelectedItems.remove(pos);

            // disable selection mode if all are deselected
            if (mSelectedItems.size() == 0) {
                mSelectMode = false;
            }
        }
        else {
            mSelectedItems.put(pos, id);
        }
        v.setActivated(!v.isActivated());
        mListener.onSelectionChanged(mSelectedItems.size());
    }

    public void clearSelection() {
        mSelectedItems.clear();
        mSelectMode = false;
        notifyDataSetChanged();
        mListener.onSelectionChanged(0);
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    public List<Integer> getSelectedPositions() {
        return new ArrayList<>(mSelectedItems.keySet());
    }

    public List<Integer> getSelectedIds() {
        return new ArrayList<>(mSelectedItems.values());
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }
    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mRecyclerView = null;
    }
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }
}
