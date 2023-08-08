package com.mycompany.gains.Activities.MainActivity.adapters;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycompany.gains.Adapters.MSCursorRecyclerAdapter;
import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.R;

import static com.mycompany.gains.Data.Database.DatabaseConstants.*;

public class HistoryListAdapter extends MSCursorRecyclerAdapter<HistoryListAdapter.ViewHolder> {

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
        String name = "";
        if (!cursor.isNull(cursor.getColumnIndex(NAME)))
            name = cursor.getString(cursor.getColumnIndex(NAME));

        if (name.length() == 0)
            name = viewHolder.itemView.getContext().getString(R.string.unnamed_workout);

        viewHolder.name.setText(name);

        // set note if exist
        String note = "";
        if (!cursor.isNull(cursor.getColumnIndex(NOTE)))
            note = cursor.getString(cursor.getColumnIndex(NOTE));

        viewHolder.note.setVisibility(note.length() > 0 ? View.VISIBLE : View.GONE);
        viewHolder.note.setText(note);

        // set date if exist
        if (!cursor.isNull(cursor.getColumnIndex(DATE)))
            viewHolder.date.setText(DateUtils.getRelativeTimeSpanString(
                    DatabaseHelper.stringToCalendar(cursor.getString(cursor.getColumnIndex(DATE))).getTimeInMillis(),
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS));
    }
}