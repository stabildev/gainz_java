package com.mycompany.gains.widgets;

import android.content.ClipData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mycompany.gains.Activities.WorkoutEditor.RowDragListener;
import com.mycompany.gains.Data.Model.Exercise;
import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Data.Model.Superset;
import com.mycompany.gains.Data.Model.Workout;
import com.mycompany.gains.R;


/**
 * Created by Klee on 10.09.2015.
 */
public class WorkoutView extends LinearLayout {
    boolean editMode = false;
    boolean showNotes = true;
    boolean useBottomPadding = true;
    boolean isDragging = false;

    private SetView mActiveSetView;

    // tracking variables for drag n drop
    int ssBefore = -1;
    int rBefore = -1;
    int ssNow = -1;
    boolean ssCreate = false;
    boolean ssIncrease = false;

    private WorkoutListener mListener;

    Workout mWorkout;


    // keep references to avoid deletion
    View bottomDivider;

    public WorkoutView(Context context) {
        super(context);
        init();
    }

    public WorkoutView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WorkoutView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init() {
        this.setOrientation(VERTICAL);
    }

    public void setWorkout(Workout workout) {
        mWorkout = workout;
        this.removeAllViews();

        bottomDivider = createDivider();
        this.addView(bottomDivider);

        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;

        // add "Empty workout" text
        boolean isWorkoutEmpty = mWorkout.getAllSets().size() == 0;
        TextView noExercise = new TextView(getContext());
        noExercise.setText(R.string.no_exercise);
        int edgeMargin = getResources().getDimensionPixelSize(R.dimen.edge_margin);
        noExercise.setPadding(0, 2 * edgeMargin, 0, 2 * edgeMargin);
        this.addView(noExercise, params);
        noExercise.setVisibility(!editMode && isWorkoutEmpty ? VISIBLE : GONE);

        bottomDivider.setVisibility(editMode | isWorkoutEmpty ? VISIBLE : GONE);

        // add "Add exercise" button and adjust visibility
        Button addExerciseButton = (Button) View.inflate(getContext(), R.layout.workout_view_add_exercise, null);
        this.addView(addExerciseButton, params);
        addExerciseButton.setVisibility(editMode ? VISIBLE : GONE);
        addExerciseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onAddExerciseClicked();
            }
        });

        // add rows
        for (Superset s : mWorkout.getSupersets())
            addSuperset(s);
    }

    private void updatePadding(RowView rowView, boolean forceExtraPadding) {
        boolean extraPadding = useBottomPadding && (forceExtraPadding || hasDividerBottom(rowView));
        Log.i("LEL", "extraPadding = " + extraPadding);
            rowView.setPadding(
                    rowView.getPaddingLeft(),
                    rowView.getPaddingTop(),
                    rowView.getPaddingRight(),
                    getResources().getDimensionPixelSize(
                            extraPadding ? R.dimen.edge_margin : R.dimen.content_margin));
    }

    public void addSuperset(Superset superset) {
        // add divider with .75dp height
        this.addView(createDivider(), indexOfChild(bottomDivider));

        for (Row r : superset.getRows()) {
            // inflate and add row view
            final RowView rowView = (RowView) View.inflate(getContext(), R.layout.row_view_concrete, null);
            rowView.setRow(r);
            rowView.showNote(showNotes);
            this.addView(rowView, indexOfChild(bottomDivider));

            if (editMode) {
                rowView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // toggle selection and expansion state
                        rowView.setSelected(!rowView.isSelected());
                        rowView.setExpanded(!rowView.isExpanded());

                        // deselect and collapse other rows
                        if (rowView.isExpanded()) {
                            for (int i = 0; i < getChildCount(); i++) {
                                if (i == indexOfChild(rowView))
                                    continue;
                                View vi = WorkoutView.this.getChildAt(i);
                                if (vi instanceof RowView) {
                                    vi.setSelected(false);
                                    ((RowView) vi).setExpanded(false);
                                }
                            }
                        }
                    }
                });
                rowView.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (mListener != null)
                            mListener.onRowLongClicked(rowView.getRow());
                        return false;
                    }
                });
                rowView.setRowListener(new RowView.RowListener() {
                    @Override
                    public void onSetClicked(Set set, boolean longClick) {
                        if (mListener != null) mListener.onSetClicked(set, longClick);
                    }

                    @Override
                    public void onAddSetClicked() {
                        if (mListener != null) mListener.onAddSetClicked(rowView.getRow());
                    }

                    @Override
                    public void onNoteChanged(String note) {
                        if (mListener != null) mListener.onNoteChanged(rowView.getRow(), note);
                    }

                    @Override
                    public void onRestClicked() {
                        if (mListener != null) mListener.onRestClicked(rowView.getRow());
                    }

                    @Override
                    public void onEditGoals() {
                        if (mListener != null) mListener.onEditGoals(rowView.getRow());
                    }

                    @Override
                    public void onDelete() {
                        if (mListener != null) mListener.onDeleteRow(rowView.getRow());
                    }

                    @Override
                    public void onEditExercise() {
                        if (mListener != null) mListener.onEditExercise(rowView.getRow().getExercise());
                    }
                });
                rowView.setRowDragListener(new RowDragListener() {

                    @Override
                    public void onRequestDrag(View view) {
                        ClipData data = ClipData.newPlainText("", "");
                        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                        view.startDrag(data, shadowBuilder, view, 0);
                    }

                    @Override
                    public void onDragStarted(View dragged) {
                        // only call once
                        if (isDragging) return;
                        isDragging = true;

                        // hide dragged view
                        dragged.setAlpha(0f);

                        // initialize tracking variables
                        Log.i("DRAG", "init");
                        Row row = ((RowView) dragged).getRow();
                        ssBefore = row.getSuperset().getPosition();
                        rBefore = row.getPosition();
                        ssNow = ssBefore;
                        ssCreate = false;
                        ssIncrease = false;
                    }

                    @Override
                    public boolean canDropOver(View dragged, View target) {
                        // return true if both views are rows
                        return (dragged instanceof RowView) && (target instanceof RowView);
                    }

                    @Override
                    public void onDragEnded(View d) {
                        // only call once
                        if (!isDragging) return;
                        isDragging = false;

                        // make dragged view visible again
                        d.setAlpha(1f);
                        updatePadding((RowView) d, false);

                        // get new row
                        int rNow = getRowPos((RowView) d);

                        // inform listener
                        if (mListener != null)
                            mListener.onMoveRow(ssBefore, rBefore, ssNow, rNow, ssCreate);

                        Log.i("DRAG", "Moved row from " + ssBefore + "." + rBefore + " to " + (ssCreate ? "newly created " : "") + ssNow + "." + rNow);
                    }

                    @Override
                    public void onHover(View d, View t, float x, float y) {
                        RowView dragged = (RowView) d;
                        RowView target = (RowView) t;

                        // area codes prevent code from being run multiple times

                        // upward drag
                        if (y < .75 * target.getHeight() && indexOfChild(target) < indexOfChild(dragged)) {
                            // remove double divider
                            if (hasDividerTop(dragged) && hasDividerBottom(dragged)) {
                                removeViewAt(indexOfChild(dragged) + 1);
                            }
                            // swap d and t
                            removeView(dragged);
                            addView(dragged, indexOfChild(target));
                            if (mListener != null)
                                mListener.onRowsSwapped();
                            // update padding
                            updatePadding(dragged, false);
                            updatePadding(target, false);

                            ssNow = target.getRow().getSuperset().getPosition();
                            ssCreate = false;
                            ssIncrease = false;
                            Log.i(" DRAG", "SUPERSET = " + ssNow + ", ROW = " + getRowPos(dragged) + ", CREATE = " + ssCreate);
                        }
                        // downward drag
                        else if (y >= .25 * target.getHeight() && indexOfChild(target) > indexOfChild(dragged)) {
                            // remove double divider
                            if (hasDividerTop(dragged) && hasDividerBottom(dragged)) {
                                removeViewAt(indexOfChild(dragged) + 1);
                            }
                            // swap d and t
                            removeView(dragged);
                            addView(dragged, indexOfChild(target) + 1);
                            if (mListener != null)
                                mListener.onRowsSwapped();
                            // update padding
                            updatePadding(dragged, false);
                            updatePadding(target, false);

                            ssNow = target.getRow().getSuperset().getPosition();
                            ssCreate = false;
                            ssIncrease = false;
                            Log.i(" DRAG", "SUPERSET = " + ssNow + ", ROW = " + getRowPos(dragged) + ", CREATE = " + ssCreate);
                        }
                        // add superset above
                        else if (y < .25 * target.getHeight() && target.equals(dragged) && hasDividerTop(target) && !hasDividerBottom(target)) {
                            addView(createDivider(), indexOfChild(target) + 1);
                            ssCreate = true;
                            Log.i(" DRAG", "SUPERSET = " + ssNow + ", ROW = " + getRowPos(dragged) + ", CREATE = " + ssCreate);
                        }
                        // add superset below
                        else if (y > .75 * target.getHeight() && target.equals(dragged) && hasDividerBottom(target) && !hasDividerTop(target)) {
                            addView(createDivider(), indexOfChild(target));
                            if (!ssIncrease)
                                ssNow++;
                            ssIncrease = true;
                            ssCreate = true;
                            Log.i(" DRAG", "SUPERSET = " + ssNow + ", ROW = " + getRowPos(dragged) + ", CREATE = " + ssCreate);
                        }
                        // TODO edge scrolling
                    }

                    @Override
                    public void onExit(View dragged, View target) {
                    }

                    @Override
                    public void onDrop(View dragged, View target) {
                    }
                });
            }

            // set minimum height for single-row supersets
            if (superset.getRowCount() == 1) {
                int minHeight = getResources().getDimensionPixelSize(R.dimen.row_list_single_height);

                rowView.setMinimumHeight(minHeight);
                rowView.requestLayout();
            }

            // add extra padding to bottom view
            if (r.getPosition() == r.getSuperset().getRowCount()-1)
                updatePadding(rowView, true);
        }
    }

    private boolean hasDivider(View child, boolean above) {
        return indexOfChild(child) == (above ? 0 : getChildCount()-1) ||
                !(getChildAt(indexOfChild(child) + (above ? -1 : 1)) instanceof RowView);
    }

    private boolean hasDividerTop(View child) {
        return hasDivider(child, true);
    }

    private boolean hasDividerBottom(View child) {
        return hasDivider(child, false);
    }

    private RowView getPreviousRow(View view) {
        for (int i = indexOfChild(view)-1; i > 0; i--)
            if (getChildAt(i) instanceof RowView)
                return (RowView) getChildAt(i);
        return null;
    }

    private RowView getNextRow(View view) {
        for (int i = indexOfChild(view)+1; i < getChildCount(); i++)
            if (getChildAt(i) instanceof RowView)
                return (RowView) getChildAt(i);
        return null;
    }

    private int getRowPos(RowView r) {
        int pos0 = indexOfChild(r);
        for (int i = 0; ; i++) {
            View v = getChildAt(pos0 - i);
            if (v instanceof RowView && hasDividerTop(v))
                return i;
        }
        /*
        if (hasDividerTop(r))
            return 0;
        else
            return getPreviousRow(r).getRow().getPosition() + 1;*/
    }

    public View createDivider() {
        // create divider with .75dp height and 16dp margin left and right
        View divider = new SupersetDivider(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, .75f, getResources().getDisplayMetrics()));
        params.leftMargin = params.rightMargin = getResources().getDimensionPixelSize(R.dimen.edge_margin);
        divider.setLayoutParams(params);
        return divider;
    }

    public void useBottomPadding(boolean use) {
        useBottomPadding = use;
    }

    // has to be called before setWorkout
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public void showNotes(boolean show) {
        this.showNotes = show;

        for (int i = 0; i < this.getChildCount(); i++) {
            View v = this.getChildAt(i);

            if (v instanceof RowView)
                ((RowView) v).showNote(show);
        }
    }

    public void setWorkoutListener(WorkoutListener listener) {
        mListener = listener;
    }

    // Convenience methods
    public void updateSet(Set set) {
        findRowView(set.getRow()).findSetView(set).update();
    }

    public void addSet(Set set) {
        findRowView(set.getRow()).addSet(set);
    }

    public void removeSet(Set set) {
        findRowView(set.getRow()).removeSet(set.getPosition());
    }

    private SetView findSetView(Set set) {
        if (set == null)
            return null;

        RowView rv = findRowView(set.getRow());
        if (rv == null)
            return null;
        else
            return rv.findSetView(set);
    }

    public void setActiveSet(Set set, boolean animate) {
        // deactivate previous set
        if (mActiveSetView != null)
            mActiveSetView.setSelected(false, animate);

        // activate new set
        mActiveSetView = findSetView(set);
        if (mActiveSetView != null)
            mActiveSetView.setSelected(true, animate);
    }

    public void removeRow(Row row) {
        RowView v = findRowView(row);
        // if single-row superset, remove divider above
        if (hasDividerBottom(v) && hasDividerTop(v))
            removeViewAt(indexOfChild(v) -1);
        removeView(v);
    }

    public RowView findRowView(Row row) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v instanceof RowView && ((RowView) v).getRow().equals(row))
                return (RowView) v;
        }
        return null;
    }

    public interface WorkoutListener {
        void onRowLongClicked(Row row);
        void onRowsSwapped();
        void onMoveRow(int fromSuperset, int fromRow, int toSuperset, int toRow, boolean newSuperset);
        void onAddExerciseClicked();
        void onSetClicked(Set set, boolean longClick);
        void onAddSetClicked(Row row);
        void onNoteChanged(Row row, String note);
        void onRestClicked(Row row);
        void onEditGoals(Row row);
        void onDeleteRow(Row row);
        void onEditExercise(Exercise exercise);
    }
}
