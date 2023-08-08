package com.mycompany.gains.Activities.WorkoutEditorOld.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mycompany.gains.Activities.WorkoutEditor.Stopwatch;
import com.mycompany.gains.Adapters.DragSelectRecyclerAdapter2;
import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Data.Model.Superset;
import com.mycompany.gains.Data.Model.Workout;
import com.mycompany.gains.R;
import com.mycompany.gains.Stuff.Misc;
import com.mycompany.gains.widgets.NoAutoFocusEditText;
import com.mycompany.gains.widgets.EmptyRecyclerView;
import com.mycompany.gains.widgets.SetView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class RowListAdapter extends DragSelectRecyclerAdapter2<DragSelectRecyclerAdapter2.BaseViewHolder>
        implements EmptyRecyclerView.ContainsHeaders {
    private Workout mWorkout;
    private RecyclerView mRecyclerView;
    private Context mContext;

    private Set mActiveSet;

    private static final int ROW_OFFSET = 1; // offset for header etc.


    // positions
    private static final int POSITION_HEADER = 0;

    // viewtypes
    private static final int VIEWTYPE_HEADER = 0;
    private static final int VIEWTYPE_MULTI = 11;

    private RowListListener mListener;
    private View activatedRow;

    public RowListAdapter(Context context, RowListListener listener, float threshold, Workout workout){
        super(context, threshold);
        mWorkout = workout;
        mListener = listener;

        // this rather complicated procedure is necessary to adjust the positions for the row offset
        // due to header and progress indicator
        this.setDragSelectListener(new DragSelectListener() {
            @Override
            public void onSelectionChanged(int selectionSize, int subSelectionSize) {
                mListener.onSelectionChanged(selectionSize, subSelectionSize);
                if (subSelectionSize == 1) {
                    Log.i("LEL", "test");
                    activateRow(getSelectedItems().get(0), getSelectedSubItem());
                } else if (activatedRow != null) {
                    deactivateRow();
                }
            }

            @Override
            public void onClick(int position, int subPosition) {
                mListener.onClick(position - ROW_OFFSET, subPosition);
            }

            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                mListener.onStartDrag(viewHolder);
            }

            @Override
            public void onItemMove(int fromPosition, int toPosition) {
                mListener.onItemMove(fromPosition - ROW_OFFSET, toPosition - ROW_OFFSET);
            }

            @Override
            public void onItemMoved(int fromPosition, int toPosition) {
                mListener.onItemMoved(fromPosition - ROW_OFFSET, toPosition - ROW_OFFSET);
            }

            @Override
            public void onItemDismiss(int position) {
                mListener.onItemDismiss(position - ROW_OFFSET);
            }
        });
    }

    public interface RowListListener extends DragSelectListener {
        void onMetaDataChanged();
        void onMoveRow(int supersetPosition, int rowPosition, boolean up);
        void onSetClicked(Set set, boolean longClick);
        void onAddSetClicked(int supersetPosition, int rowPosition);
    }

    // adjust item selection for row offset
    @Override
    public List<Integer> getSelectedItems() {
        List<Integer> selection = new ArrayList<>(getSelectedItemCount());
        for (int i : super.getSelectedItems())
            selection.add(i - ROW_OFFSET);
        return selection;
    }

    @Override
    public int getRowOffset() {
        return ROW_OFFSET;
    }

    public static class HeaderViewHolder extends BaseViewHolder {
        NoAutoFocusEditText title;
        NoAutoFocusEditText note;
        TextView date;
        Calendar calendar;
        DateFormat df = DateFormat.getDateInstance();
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        boolean showTime = false;
        long duration;

        HeaderListener mListener;

        public HeaderViewHolder(View v) {
            super(v);
            title = (NoAutoFocusEditText) v.findViewById(R.id.workout_title);
            note = (NoAutoFocusEditText) v.findViewById(R.id.workout_note);
            date = (TextView) v.findViewById(R.id.workout_date);

            // disable movement of set editor when keyboard is shown
            title.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    ((Activity) title.getContext()).getWindow().setSoftInputMode(
                            hasFocus ? WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                                    : WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED);
                    if (mListener != null && !hasFocus)
                        mListener.onTitleChanged(title.getText().toString());
                }
            });
            note.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    ((Activity) note.getContext()).getWindow().setSoftInputMode(
                            hasFocus ? WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                                    : WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED);
                    if (mListener != null && !hasFocus)
                        mListener.onNoteChanged(note.getText().toString());
                    if (!hasFocus) {
                        InputMethodManager imm = (InputMethodManager)note.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(note.getWindowToken(), 0);
                    }
                }
            });

            // lose focus when keyboard is hidden
            title.setOnImeBackListener(new NoAutoFocusEditText.OnImeBackListener() {
                @Override
                public void onImeBack(NoAutoFocusEditText ctrl) {
                    title.clearFocus();
                }
            });
            note.setImeOptions(EditorInfo.IME_ACTION_DONE);
            note.setOnImeBackListener(new NoAutoFocusEditText.OnImeBackListener() {
                @Override
                public void onImeBack(NoAutoFocusEditText ctrl) {
                    note.clearFocus();
                }
            });
            note.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE)
                        note.clearFocus();
                    return false;
                }
            });
        }

        public interface HeaderListener {
            void onTitleChanged(String title);
            void onNoteChanged(String note);
        }

        public void setStopwatch(Stopwatch stopwatch) {
            stopwatch.addStopwatchListener(new Stopwatch.StopwatchListener() {
                @Override
                public void onTick(long duration) {
                    Log.i("Stopwatch", "TICK");
                    // update time
                    HeaderViewHolder.this.duration = duration;
                    updateTime();
                }
                @Override
                public void onStop() {
                }
            });
        }

        public void updateTime() {
            String text = df.format(calendar.getTime())
                    + "\n" + tf.format(calendar.getTime());
            if (showTime)
                text += " + " + getElapsedTimeMinutesSecondsString(
                        (int) duration);
            date.setText(text);
        }

        public void setDate(final Calendar calendar) {
            this.calendar = calendar;
            updateTime();
        }

        public void showTime(boolean show) {
            showTime = show;
        }

        public void setHeaderListener(HeaderListener listener) {
            mListener = listener;
        }

        @Override
        public boolean isDragDropSelectEnabled() {
            return false;
        }

        public static String getElapsedTimeMinutesSecondsString(int miliseconds) {
            int elapsedTime = miliseconds;
            String format = String.format("%%0%dd", 2);
            elapsedTime = elapsedTime / 1000;
            String seconds = String.format(format, elapsedTime % 60);
            String minutes = String.format(format, elapsedTime / 60);
            return minutes + ":" + seconds;
        }
    }

    public static class ItemViewHolder extends DragSelectRecyclerAdapter2.ItemViewHolder {
        LinearLayout rows;
        SetView activeSetView;

        RowListener listener;

        boolean activatedBeforeMove;

        public ItemViewHolder(View v) {
            super(v);
            rows = (LinearLayout) v.findViewById(R.id.item_row_list_multi);
        }

        public interface RowListener {
            void onMove(int adapterPosition, int rowPosition, boolean up);
            void onAddSetClicked(int adapterPosition, int rowPosition);
            void onSetClicked(Set set, boolean longClick);
        }

        public void setRowListener(RowListener rowListener) {
            listener = rowListener;
        }

        public void addRow(final Row rowData) {
            final View rowLayout = View.inflate(itemView.getContext(), R.layout.row_view_old, null);

            TextView name = (TextView) rowLayout.findViewById(R.id.name);
            name.setText(rowData.getExercise().getName());

            // disable rest display
            rowLayout.findViewById(R.id.rest).setVisibility(View.GONE);

            rows.addView(rowLayout);
            rowLayout.setOnTouchListener(this);
            rowLayout.setOnClickListener(this);
            rowLayout.setOnLongClickListener(this);

            View addSet = rowLayout.findViewById(R.id.set_add);
            addSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onAddSetClicked(getAdapterPosition(), rowData.getPosition());
                }
            });

            final View upBtn = rowLayout.findViewById(R.id.up_btn);
            final View downBtn = rowLayout.findViewById(R.id.down_btn);

            upBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (upBtn.getAlpha() < 1f)
                        return;

                    int index = rows.indexOfChild(rowLayout);
                    if (listener != null) listener.onMove(getAdapterPosition(),
                            index, true);
                    rows.removeViewAt(index--);

                    rows.addView(rowLayout, index);

                    updateButtonStates(upBtn, downBtn, rowLayout);
                }
            });
            downBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (downBtn.getAlpha() < 1f)
                        return;

                    int index = rows.indexOfChild(rowLayout);
                    if (listener != null) listener.onMove(getAdapterPosition(),
                            rows.indexOfChild(rowLayout), false);
                    rows.removeViewAt(index++);

                    rows.addView(rowLayout, index);

                    updateButtonStates(upBtn, downBtn, rowLayout);
                }
            });
            updateButtonStates(upBtn, downBtn, rowLayout);
        }

        public void updateButtonStates(View upBtn, View downBtn, View rowLayout) {
            int index = rows.indexOfChild(rowLayout);

            if (index == rows.getChildCount() - 1) {
                downBtn.setAlpha(.2f);
            } else {
                downBtn.setAlpha(1f);
            }

            if (index == 0) {
                upBtn.setAlpha(.2f);
            } else {
                upBtn.setAlpha(1f);
            }
        }

        public void setIsSingleViewHolder(boolean isSingleViewHolder) {
            View firstRow = rows.getChildAt(0);
            int minHeight = itemView.getResources().getDimensionPixelSize(R.dimen.row_list_single_height);

            firstRow.setMinimumHeight(isSingleViewHolder ? minHeight : 0);
            firstRow.requestLayout();
        }

        public SetView addSet(final Set set) {
            SetView setView = (SetView) View.inflate(itemView.getContext(), R.layout.set_view_concrete, null);
            setView.setText(SetView.getSetText(set));
            setView.setDone(set.isDone());

            LinearLayout sets = findSetsForRow(set.getRow().getPosition());
            sets.addView(setView, sets.getChildCount()-1); // b/c of add set view
            setView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onSetClicked(set, false);
                }
            });
            setView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    listener.onSetClicked(set, true);
                    return true;
                }
            });

            if (sets.indexOfChild(setView) > 0) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) setView.getLayoutParams();
                params.leftMargin = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.content_margin);
            }
            return setView;
        }

        public void removeSet(final int row, final int pos) {
            LinearLayout sets = findSetsForRow(row);
            if (sets.getChildAt(pos).equals(activeSetView))
                activeSetView = null;

            sets.removeViewAt(pos);
        }

        public void updateSet(Set set) {
            SetView setView = findSetView(set);
            setView.setText(SetView.getSetText(set));
            setView.setDone(set.isDone());
        }

        public void activateSet(Set set, boolean activate, boolean animate) {
            SetView setView = findSetView(set);
            activeSetView = activate ? setView : null;
            setView.setSelected(activate, animate);

            // (de-)activate row
            Misc.setBackground(rows.getChildAt(set.getRow().getPosition()),
                    ContextCompat.getDrawable(itemView.getContext(),
                            activate ? R.drawable.list_item_selector_active
                                    : R.drawable.list_item_selector));
            //rows.getChildAt(set.getRow().getPosition()).setSelected(activate);

            if (activate) {
                // scroll to current set (or following if exists)
                LinearLayout sets = findSetsForRow(set.getRow().getPosition());
                HorizontalScrollView scrollView = (HorizontalScrollView) sets.getParent();
                // next set in row if exists or current one
                View nextView = sets.getChildAt(
                        Math.min(sets.indexOfChild(setView)+1,
                                sets.getChildCount() -1));
                View prevView = sets.indexOfChild(setView) > 0 ? sets.getChildAt(sets.indexOfChild(setView) -1) : setView;

                if (nextView.getRight() - scrollView.getScrollX() > scrollView.getWidth())
                    scrollView.smoothScrollTo(nextView.getRight() - scrollView.getWidth(), 0);
                else if (prevView.getLeft() - scrollView.getScrollX() < 0)
                    scrollView.smoothScrollTo(prevView.getLeft(), 0);
            }
        }

        public LinearLayout findSetsForRow(int row) {
            View rowLayout = rows.getChildAt(row);
            return (LinearLayout) rowLayout.findViewById(R.id.sets);
        }

        public SetView findSetView(Set set) {
            return (SetView) findSetsForRow(set.getRow().getPosition()).getChildAt(set.getPosition());
        }

        @Override
        public void onItemSelected() {
            if (Build.VERSION.SDK_INT >= 21) {
                itemView.animate().translationZ(4);
            } else {
                activatedBeforeMove = itemView.isActivated();
                itemView.setActivated(true);
            }
        }

        @Override
        public void onItemClear() {
            if (Build.VERSION.SDK_INT >= 21) {
                itemView.animate().translationZ(0);
            } else {
                itemView.setActivated(activatedBeforeMove);
            }
        }

        @Override
        public boolean isDragDropSelectEnabled() {
            return true;
        }

        @Override
        public int getSubPosition(View v) {
            if (rows.getChildCount() > 1)
                return rows.indexOfChild(v);
            else
                return -1;
        }
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case VIEWTYPE_HEADER:
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.workout_view_header_edit_old, parent, false);
                return new HeaderViewHolder(itemView);
            default:
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.group_view_concrete, parent, false);
                return new ItemViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder viewHolder, int position) {
        switch (getItemViewType(position)) {
            case VIEWTYPE_HEADER: {
                HeaderViewHolder holder = (HeaderViewHolder) viewHolder;

                // set workout title
                if (mWorkout.getName().length() > 0)
                    holder.title.setText(mWorkout.getName());

                // set workout date
                Calendar date = mWorkout.getDate();
                holder.setDate(date);

                // set workout note
                if (mWorkout.getNote().length() > 0)
                    holder.note.setText(mWorkout.getNote());

                // set listener
                holder.setHeaderListener(new HeaderViewHolder.HeaderListener() {
                    @Override
                    public void onTitleChanged(String title) {
                        if (!title.equals(mWorkout.getName())) {
                            mWorkout.setName(title);
                            mListener.onMetaDataChanged();
                        }
                    }

                    @Override
                    public void onNoteChanged(String note) {
                        if (!note.equals(mWorkout.getNote())) {
                            mWorkout.setNote(note);
                            mListener.onMetaDataChanged();
                        }
                    }
                });

                return;
            } case VIEWTYPE_MULTI: {
                final Superset superset = mWorkout.getSuperset(position - ROW_OFFSET);
                final ItemViewHolder holder = (ItemViewHolder) viewHolder;
                super.onBindViewHolder(holder, position);

                holder.activatedBeforeMove = holder.itemView.isActivated();
                holder.rows.removeAllViews();
                for (final Row rowData : superset.getRows()) {
                    holder.addRow(rowData);
                    holder.setRowListener(new ItemViewHolder.RowListener() {
                        @Override
                        public void onMove(int adapterPosition, int rowPosition, boolean up) {
                            mListener.onMoveRow(adapterPosition - ROW_OFFSET, rowPosition, up);
                        }
                        @Override
                        public void onAddSetClicked(int adapterPosition, int rowPosition) {
                            mListener.onAddSetClicked(adapterPosition - ROW_OFFSET, rowPosition);
                        }
                        @Override
                        public void onSetClicked(Set set, boolean longClick) {
                            mListener.onSetClicked(set, longClick);
                        }
                    });

                    for (final Set setData : rowData.getSets()) {
                        holder.addSet(setData);
                        if (mActiveSet == null || setData.equals(mActiveSet)) {
                            holder.activateSet(setData, true, mActiveSet == null); // don't animate on notifyDatasetChanged
                            mActiveSet = setData;
                        }
                    }
                }
                // add extra padding to bottom view
                View lastRow = holder.rows.getChildAt(holder.rows.getChildCount() - 1);
                lastRow.setPadding(
                        lastRow.getPaddingLeft(),
                        lastRow.getPaddingTop(),
                        lastRow.getPaddingRight(),
                        holder.itemView.getResources().getDimensionPixelSize(R.dimen.edge_margin)
                );

                holder.setIsSingleViewHolder(superset.getRowCount() == 1);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mWorkout.getSupersetCount() + ROW_OFFSET;
    }

    @Override
    public int getItemViewType(int position) {
        switch (position) {
            case POSITION_HEADER:
                return VIEWTYPE_HEADER;
            default:
                return VIEWTYPE_MULTI; /*mWorkout.getSuperset(position - ROW_OFFSET).getRowCount() > 1
                        ? VIEWTYPE_MULTI : VIEWTYPE_SINGLE;*/
        }
    }

    @Override
    public void onAttachedToRecyclerView (RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mRecyclerView.setHasFixedSize(true);
    }

    @Override
    public void onDetachedFromRecyclerView (RecyclerView recyclerView) {
        mRecyclerView = null;
    }

    public void addSet(Set set) {
        if (mRecyclerView == null)
            return;

        int adapterPos = set.getRow().getSuperset().getPosition() + ROW_OFFSET;
        switch (getItemViewType(adapterPos)) {
            case VIEWTYPE_MULTI:
                ((ItemViewHolder) mRecyclerView.findViewHolderForAdapterPosition(adapterPos))
                        .addSet(set);
                //return;
        }
    }

    public void removeSet(Set set) {
        if (mRecyclerView == null)
            return;

        int adapterPos = set.getRow().getSuperset().getPosition() + ROW_OFFSET;
        switch (getItemViewType(adapterPos)) {
            case VIEWTYPE_MULTI:
                ((ItemViewHolder) mRecyclerView.findViewHolderForAdapterPosition(adapterPos))
                        .removeSet(set.getRow().getPosition(), set.getPosition());
                //return;
        }
    }

    public void setActiveSet(Set set) {
        Log.i("LEL", "setActiveSet("+set.getRow().getSuperset().getPosition()+"."+set.getRow().getPosition()+"."+set.getPosition()+")");
        if (mRecyclerView == null)
            return;

        int newAdapterPos = set.getRow().getSuperset().getPosition() + ROW_OFFSET;

        // deactivate old set
        if (mActiveSet != null) {
            int oldAdapterPos = mActiveSet.getRow().getSuperset().getPosition() + ROW_OFFSET;
            ItemViewHolder oldViewHolder = (ItemViewHolder) mRecyclerView.findViewHolderForAdapterPosition(oldAdapterPos);

            if (oldViewHolder != null) {
                oldViewHolder.activateSet(mActiveSet, false, true);
            }
        }

        // activate new set
        ItemViewHolder newViewHolder = (ItemViewHolder) mRecyclerView.findViewHolderForAdapterPosition(newAdapterPos);

        if (newViewHolder != null) {
            newViewHolder.activateSet(set, true, true);
        }

        mActiveSet = set;
    }

    public void updateSet(Set set) {
        if (mRecyclerView == null)
            return;
        int adapterPos = set.getRow().getSuperset().getPosition() + ROW_OFFSET;

        try {
            ((ItemViewHolder) mRecyclerView.findViewHolderForAdapterPosition(adapterPos))
                    .updateSet(set);
        } catch (Exception e) {
            Log.e("LEL", "Can't find viewholder for set");
        }
    }

    public void deactivateRow() {
        if (activatedRow != null)
            activatedRow.setVisibility(View.GONE);
    }

    public void activateRow(final int supersetPos, final int rowPos) {
        if (mRecyclerView == null)
            return;
        int adapterPos = supersetPos + ROW_OFFSET;
        ItemViewHolder holder = (ItemViewHolder) mRecyclerView.findViewHolderForAdapterPosition(
                adapterPos);
        View rowLayout = holder.rows.getChildAt(rowPos);
        View upDownBtns = rowLayout.findViewById(R.id.move_btns);

        for (int i = 0; i < holder.rows.getChildCount(); i++) {
            View row = holder.rows.getChildAt(i);
            holder.updateButtonStates(
                    row.findViewById(R.id.up_btn),
                    row.findViewById(R.id.down_btn),
                    row);
        }

        upDownBtns.setVisibility(View.VISIBLE);
        activatedRow = upDownBtns;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }
}