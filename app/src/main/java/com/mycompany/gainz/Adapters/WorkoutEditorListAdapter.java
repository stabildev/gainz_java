package com.mycompany.gainz.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.collect.SortedSetMultimap;
import com.mycompany.gainz.Database.Model.Set;
import com.mycompany.gainz.R;


public class WorkoutEditorListAdapter extends RecyclerView.Adapter<WorkoutEditorListAdapter.ViewHolder> {
    SortedSetMultimap<Integer, Set> mSetsInRows;

    public WorkoutEditorListAdapter(SortedSetMultimap<Integer, Set> sets){
        super();
        mSetsInRows = sets;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        View divider;

        public ViewHolder(View v) {
            super(v);
            name = (TextView) v.findViewById(R.id.item_workoutList_name);
            divider = v.findViewById(R.id.item_workoutList_divider);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout_list, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.name.setText(
                mSetsInRows.get(position+1).first().getExercise().getName());

        // deactivate divider if following item is part of same superset
        if (position < getItemCount()-1 && mSetsInRows.get(position+1).first().getSuperset()
                == mSetsInRows.get(position+2).first().getSuperset())
            viewHolder.divider.setVisibility(View.GONE);
        else
            viewHolder.divider.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return mSetsInRows.keySet().size();
    }
}