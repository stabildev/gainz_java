package com.mycompany.gainz.Adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycompany.gainz.Database.Helper.DBHelper;
import com.mycompany.gainz.R;

public class RoutineListAdapter extends MSCursorRecyclerAdapter<RoutineListAdapter.ViewHolder> {

    public RoutineListAdapter(Context context, Cursor cursor){
        super(context,cursor);
    }

    public static class ViewHolder extends MSCursorRecyclerAdapter.ViewHolder {
        TextView name;

        public ViewHolder(View v) {
            super(v);
            name = (TextView) v.findViewById(R.id.item_routineList_name);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_routine_list, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position, Cursor cursor) {

        super.onBindViewHolder(viewHolder, position, cursor);

        // set name if exist
        if (!cursor.isNull(cursor.getColumnIndex(DBHelper.NAME)))
            viewHolder.name.setText(cursor.getString(cursor.getColumnIndex(DBHelper.NAME)));
        else
            viewHolder.name.setText(viewHolder.itemView.getContext().getString(R.string.unnamed_routine));
    }
}
