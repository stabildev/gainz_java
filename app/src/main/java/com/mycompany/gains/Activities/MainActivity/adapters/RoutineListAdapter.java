package com.mycompany.gains.Activities.MainActivity.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.mycompany.gains.Adapters.MSCursorRecyclerAdapter;
import com.mycompany.gains.R;

import static com.mycompany.gains.Data.Database.DatabaseConstants.*;

public class RoutineListAdapter extends MSCursorRecyclerAdapter<RoutineListAdapter.ViewHolder> {
    private RoutineAdapterListener mListener;

    public RoutineListAdapter(Context context, Cursor cursor){
        super(context,cursor);
    }

    public interface RoutineAdapterListener extends MSCursorRecyclerAdapter.OnSelectionListener {
        void onRoutineStarred(int id, boolean starred);
    }

    public static class ViewHolder extends MSCursorRecyclerAdapter.ViewHolder {
        TextView name;
        TextView note;
        CheckBox star;

        public ViewHolder(View v) {
            super(v);
            name = (TextView) v.findViewById(R.id.name);
            note = (TextView) v.findViewById(R.id.note);
            star = (CheckBox) v.findViewById(R.id.star);
        }

        public int getId() {
            return _id;
        }
    }

    public void setRoutineAdapterListener(RoutineAdapterListener listener) {
        super.setOnSelectionListener(listener);
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_routine_list, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position, Cursor cursor) {

        super.onBindViewHolder(viewHolder, position, cursor);

        // set name if exist
        String name = "";
        if (!cursor.isNull(cursor.getColumnIndex(NAME)))
            name = cursor.getString(cursor.getColumnIndex(NAME));

        if (name.length() == 0)
            name = viewHolder.itemView.getContext().getString(R.string.unnamed_routine);

        viewHolder.name.setText(name);

        // set note if exist
        String note = "";
        if (!cursor.isNull(cursor.getColumnIndex(NOTE)))
            note = cursor.getString(cursor.getColumnIndex(NOTE));

        viewHolder.note.setVisibility(note.length() > 0 ? View.VISIBLE : View.GONE);
        viewHolder.note.setText(note);

        // update starred state
        boolean starred = false;
        if (!cursor.isNull(cursor.getColumnIndex(IS_STARRED)))
            starred = cursor.getInt(cursor.getColumnIndex(IS_STARRED)) == 1;
        viewHolder.star.setChecked(starred);

        viewHolder.star.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mListener.onRoutineStarred(viewHolder.getId(), isChecked);
            }
        });
    }
}
