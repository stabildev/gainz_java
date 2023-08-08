package com.mycompany.gains.Adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.List;


public abstract class CheckboxCursorRecyclerAdapter<VH extends RecyclerView.ViewHolder> extends CursorRecyclerViewAdapter<VH> {
    private SparseBooleanArray selectedItems;

    private OnSelectionListener mListener;

    public interface OnSelectionListener {
        void onSelectItem(int _id);
        void onDeselectItem(int _id);
    }

    public void setOnSelectionListener(OnSelectionListener listener) {
        mListener = listener;
    }

    public CheckboxCursorRecyclerAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        selectedItems = new SparseBooleanArray();
    }

    public static class CheckboxViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        ClickListener clickListener;
        int _id;
        protected CheckBox checkBox;

        public CheckboxViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
        }

        // interface for handling clicks and checkbox toggles
        public interface ClickListener {
            void onClick(View v, int position, int _id);
        }

        public void setClickListener(ClickListener clickListener) {
            this.clickListener = clickListener;
        }

        @Override
        public void onClick(View v) {
            clickListener.onClick(v, getAdapterPosition(), _id);
        }
    }

    @Override
    public void onBindViewHolder(final VH viewHolder, int position, Cursor cursor) {

        if (!(viewHolder instanceof CheckboxViewHolder))
            return;

        CheckboxViewHolder holder = (CheckboxViewHolder) viewHolder;

        holder._id = cursor.getInt(cursor.getColumnIndex("_id"));
        if (holder.checkBox != null)
            holder.checkBox.setChecked(selectedItems.get(position, false));

        // set ClickListener
        holder.setClickListener(new CheckboxViewHolder.ClickListener() {
            @Override
            public void onClick(View v, int position, int _id) {
                toggleSelection(position, _id);
            }
        });
    }

    public void toggleSelection(int pos, int _id) {
        // if item is selected
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
            mListener.onDeselectItem(_id);
        } else {
            selectedItems.put(pos, true);
            mListener.onSelectItem(_id);
        }
        notifyItemChanged(pos);
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