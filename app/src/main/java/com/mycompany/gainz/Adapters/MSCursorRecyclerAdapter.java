package com.mycompany.gainz.Adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.View;

import java.util.ArrayList;
import java.util.List;


public abstract class MSCursorRecyclerAdapter<VH extends MSCursorRecyclerAdapter.ViewHolder> extends CursorRecyclerViewAdapter<VH> {
    private SparseBooleanArray selectedItems;
    private boolean selectMode = false;

    private OnSelectionListener mListener;

    public interface OnSelectionListener {
        void onStartSelection();
        void onSelectItem(int _id);
        void onDeselectItem(int _id);
        void onDeselectAll();
        void onClick(int _id);
    }

    public void setOnSelectionListener(OnSelectionListener listener) {
        mListener = listener;
    }

    public MSCursorRecyclerAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        selectedItems = new SparseBooleanArray();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {
        private ClickListener clickListener;
        int _id;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
        }

        // interface for handling clicks, both long and normal
        public interface ClickListener {
            public void onClick(View v, int position, int _id, boolean isLongClick);
        }

        public void setClickListener(ClickListener clickListener) {
            this.clickListener = clickListener;
        }

        @Override
        public void onClick(View v) {
            clickListener.onClick(v, getPosition(), _id, false);
        }

        @Override
        public boolean onLongClick(View v) {
            clickListener.onClick(v, getPosition(), _id, true);
            return true;
        }
    }

    @Override
    public void onBindViewHolder(VH viewHolder, int position, Cursor cursor) {

        viewHolder._id = cursor.getInt(cursor.getColumnIndex("_id"));
        viewHolder.itemView.setSelected(selectedItems.get(position, false));
        viewHolder.itemView.setActivated(selectedItems.get(position, false));

        // set ClickListener
        viewHolder.setClickListener(new ViewHolder.ClickListener() {
            @Override
            public void onClick(View v, int pos, int _id, boolean isLongClick) {
                if (selectMode) {
                    toggleSelection(pos, _id);
                } else if (isLongClick) {
                    // View v at position pos is long-clicked
                    selectMode = true;
                    mListener.onStartSelection();
                    toggleSelection(pos, _id);
                } else {
                    mListener.onClick(_id);
                }
            }
        });
    }

    public void toggleSelection(int pos, int workout_id) {
        // if item is selected
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
            mListener.onDeselectItem(workout_id);

            // disable selection mode if all are deselected
            if (selectedItems.size() == 0) {
                selectMode = false;
                mListener.onDeselectAll();
            }
        }
        else {
            selectedItems.put(pos, true);
            mListener.onSelectItem(workout_id);
        }

        notifyItemChanged(pos);
    }

    public void clearSelection() {
        selectedItems.clear();
        selectMode = false;
        mListener.onDeselectAll();

        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }
}
