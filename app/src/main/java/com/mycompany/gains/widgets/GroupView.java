package com.mycompany.gains.widgets;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Data.Model.Superset;
import com.mycompany.gains.R;

/**
 * Created by Klee on 10.09.2015.
 */
public class GroupView extends LinearLayout {
    boolean editable = false;
    boolean showNote = true;
    boolean useBottomPadding = true;

    GroupListener listener;
    LinearLayout rows;

    Superset mSuperset;

    public GroupView(Context context) {
        super(context, null);
        initializeViews();
    }

    public GroupView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        initializeViews();
    }

    public GroupView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void initializeViews() {
        this.setOrientation(VERTICAL);
        rows = this;
        this.setClickable(false);
        this.setFocusable(false);
    }

    public void setSuperset(Superset superset) {
        mSuperset = superset;
        rows.removeAllViews();

        for (Row r : mSuperset.getRows()) {
            RowView rowView = addRow(r);
            rowView.showNote(showNote);

            // add extra padding to bottom view
            if (useBottomPadding && r.getPosition() == r.getSuperset().getRowCount()-1)
                rowView.setPadding(
                        rowView.getPaddingLeft(),
                        rowView.getPaddingTop(),
                        rowView.getPaddingRight(),
                        getResources().getDimensionPixelSize(R.dimen.edge_margin)
                );
        }
    }

    public void useBottomPadding(boolean use) {
        useBottomPadding = use;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void showNote(boolean show) {
        this.showNote = show;

        for (int i = 0; i < rows.getChildCount(); i++) {
            ((RowView) rows.getChildAt(i)).showNote(show);
        }
    }

    public RowView addRow(final Row row) {
        final RowView rowView = (RowView) View.inflate(getContext(), R.layout.row_view_concrete, null);
        rowView.setRow(row);
        /*rowView.setRowListener(new RowView.RowListener() {
            @Override
            public void onSetClicked(Set set, boolean longClick) {
                if (listener != null) listener.onSetClicked(set, longClick);
            }

            @Override
            public void onAddSetClicked() {
                if (listener != null)
                    listener.onAddSetClicked(row.getPosition());
            }
        });*/
        addView(rowView);

            updateHeight();
        return rowView;
    }

    // set minimum height for single-row groups
    public void updateHeight() {
        View firstRow = getChildAt(0);
        boolean isSingleViewHolder = getChildCount() == 1;

        int minHeight = getResources().getDimensionPixelSize(R.dimen.row_list_single_height);

        firstRow.setMinimumHeight(isSingleViewHolder ? minHeight : 0);
        firstRow.requestLayout();
    }

    public void setGroupListener(GroupListener listener) {
        this.listener = listener;
    }

    public interface GroupListener {
        void onSetClicked(Set set, boolean longClick);
        void onAddSetClicked(int row);
    }
}
