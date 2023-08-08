package com.mycompany.gainz.Adapters;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycompany.gainz.Database.Helper.DBHelper;
import com.mycompany.gainz.R;

public class HistoryListAdapter extends MSCursorRecyclerAdapter<HistoryListAdapter.ViewHolder> {
    private DBHelper db;

    public HistoryListAdapter(Context context, Cursor cursor){
        super(context,cursor);
    }

    public static class ViewHolder extends MSCursorRecyclerAdapter.ViewHolder {
        TextView name, date, note;

        public ViewHolder(View v) {
            super(v);
            name = (TextView) v.findViewById(R.id.item_historyList_name);
            date = (TextView) v.findViewById(R.id.item_historyList_date);
            note = (TextView) v.findViewById(R.id.item_historyList_note);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_list, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position, Cursor cursor) {

        super.onBindViewHolder(viewHolder, position, cursor);

        // set name if exist
        if (!cursor.isNull(cursor.getColumnIndex(DBHelper.NAME)))
            viewHolder.name.setText(cursor.getString(cursor.getColumnIndex(DBHelper.NAME)));
        else
            viewHolder.name.setText(viewHolder.itemView.getContext().getString(R.string.unnamed_workout));

        // set date if exist
        if (!cursor.isNull(cursor.getColumnIndex(DBHelper.DATE)))
            viewHolder.date.setText(DateUtils.getRelativeTimeSpanString(
                    DBHelper.stringToCalendar(cursor.getString(cursor.getColumnIndex(DBHelper.DATE))).getTimeInMillis(),
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS));

        // set note if exist
        if (!cursor.isNull(cursor.getColumnIndex(DBHelper.NOTE))) {
            viewHolder.note.setText(cursor.getString(cursor.getColumnIndex(DBHelper.NOTE)));
            viewHolder.note.setVisibility(View.VISIBLE);
        } else
            viewHolder.note.setVisibility(View.GONE);
    }
}