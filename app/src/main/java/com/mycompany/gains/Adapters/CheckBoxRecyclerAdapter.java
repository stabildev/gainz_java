package com.mycompany.gains.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public abstract class CheckBoxRecyclerAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected Context mContext;
    private LinkedHashMap<Integer, Integer> mSelectedItems; // <Position, id>

    private OnSelectionListener mListener;

    public interface OnSelectionListener {
        void onSelectionChanged(int selectionSize);
        void onLongClick(int id);
    }

    public interface SelectionListener {
        OnSelectionListener getOnSelectionListener(Object T);
    }

    public void setOnSelectionListener(OnSelectionListener listener) {
        mListener = listener;
    }

    public CheckBoxRecyclerAdapter(Context context) {
        super();
        mContext = context;
        mSelectedItems = new LinkedHashMap<>();
    }

    public static class CheckBoxViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {
        
        private ClickListener clickListener;
        int _id;
        protected CheckBox checkBox;

        public CheckBoxViewHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
        }

        public void setId(int id) {
            _id = id;
        }

        // interface for handling clicks, both long and normal
        public interface ClickListener {
            void onClick(View v, int position, int _id);
            void onLongClick(View v, int position, int _id);
        }

        public void setClickListener(ClickListener clickListener) {
            this.clickListener = clickListener;
        }

        @Override
        public void onClick(View v) {
            clickListener.onClick(v, getAdapterPosition(), _id);
        }

        @Override
        public boolean onLongClick(View v) {
            clickListener.onLongClick(v, getAdapterPosition(), _id);
            return true;
        }
    }

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {
        
        if (!(viewHolder instanceof CheckBoxViewHolder))
            return;
        
        CheckBoxViewHolder holder = (CheckBoxViewHolder) viewHolder;
        holder.checkBox.setChecked((mSelectedItems.containsKey(position)));
        holder.checkBox.setOnClickListener(holder);

        // set ClickListener
        holder.setClickListener(new CheckBoxViewHolder.ClickListener() {
            @Override
            public void onClick(View v, int pos, int _id) {
                toggleSelection(pos, _id);
            }

            @Override
            public void onLongClick(View v, int pos, int _id) {
                mListener.onLongClick(_id);
            }
        });
    }

    public void toggleSelection(int pos, int id) {
        // if item is selected
        Log.i("LEL", "toggleSelection(pos = " + pos + ", id = " + id + ", v)");
        if (mSelectedItems.containsKey(pos)) {
            mSelectedItems.remove(pos);
        }
        else {
            mSelectedItems.put(pos, id);
        }
        mListener.onSelectionChanged(mSelectedItems.size());
        notifyItemChanged(pos);
    }

    public void clearSelection() {
        mSelectedItems.clear();
        notifyDataSetChanged();
        mListener.onSelectionChanged(0);
    }

    public void insertItem(int position) {
        // create new map to avoid ConcurrentModificationException
        LinkedHashMap<Integer, Integer> newMap = new LinkedHashMap<>(mSelectedItems.size());

        // move all selected items up by one beginning with position
        for (Map.Entry<Integer, Integer> entry : mSelectedItems.entrySet()) {
            if (entry.getKey() >= position)
                newMap.put(entry.getKey()+1, mSelectedItems.get(entry.getKey()));
            else
                newMap.put(entry.getKey(), mSelectedItems.get(entry.getKey()));
        }

        mSelectedItems = newMap;
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
}
