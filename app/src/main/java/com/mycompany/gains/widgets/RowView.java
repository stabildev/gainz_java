package com.mycompany.gains.widgets;

import android.content.Context;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.mycompany.gains.Activities.WorkoutEditor.CustomTextWatcher;
import com.mycompany.gains.Activities.WorkoutEditor.RowDragListener;
import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.R;

/**
 * Created by Klee on 07.09.2015.
 */
public class RowView extends RelativeLayout {

    Row mRow;
    boolean showNote = false;
    boolean expanded = false;

    TextView name;
    EditText noteEdit;
    TextView note;
    ViewFlipper flipper;
    View icMore;
    RestView rest;
    HorizontalScrollView scrollView;
    LinearLayout sets;
    View addSetBtn;

    RowListener listener;

    private boolean dragging;
    private boolean activated;
    private boolean selected;

    static final int[] STATE_DRAGGING = {R.attr.state_dragging};

    public RowView(Context context) {
        super(context);
        initializeViews(context);
    }

    public RowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public RowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    private void initializeViews(Context context) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.row_view, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // initialize views
        name = (TextView) findViewById(R.id.name);
        note = (TextView) findViewById(R.id.note);
        noteEdit = (EditText) findViewById(R.id.note_edit);
        flipper = (ViewFlipper) findViewById(R.id.note_switcher);
        icMore = findViewById(R.id.ic_more);
        rest = (RestView) findViewById(R.id.rest);
        scrollView = (HorizontalScrollView) findViewById(R.id.scroll_view);
        sets = (LinearLayout) findViewById(R.id.sets);
        addSetBtn = findViewById(R.id.set_add);

        icMore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popup = new PopupMenu(getContext(), v) {
                    @Override
                    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_edit_goals:
                                if (listener != null)
                                    listener.onEditGoals();
                                break;
                            case R.id.action_delete_exercise:
                                if (listener != null)
                                    listener.onDelete();
                                break;
                            case R.id.action_edit_exercise:
                                if (listener != null)
                                    listener.onEditExercise();
                                break;
                        }
                        return true;
                    }
                };
                popup.inflate(R.menu.popup_row);
                popup.show();
            }
        });

        addSetBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onAddSetClicked();
            }
        });
        addSetBtn.setVisibility(GONE);

        rest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onRestClicked();
            }
        });
        rest.setClickable(expanded);

        CustomTextWatcher noteListener = new CustomTextWatcher(3000) {
            @Override
            public void onFocusLostOrTimeOut(String text) {
                if (listener != null) listener.onNoteChanged(text);
            }
            @Override
            public void afterTextChanged(Editable s) {
                super.afterTextChanged(s);
                note.setText(s.toString()); // sync TextView
            }
        };
        noteEdit.setOnFocusChangeListener(noteListener);
        noteEdit.addTextChangedListener(noteListener);
    }

    public void addSet(Set set) {
        final SetView setView = (SetView) View.inflate(getContext(), R.layout.set_view_concrete, null);
        setView.setSet(set);
        sets.addView(setView, sets.getChildCount() - 1); // b/c of add set view;
        setView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onSetClicked(setView.getSet(), false);
            }
        });
        setView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (listener != null)
                    listener.onSetClicked(setView.getSet(), true);
                return true;
            }
        });

        if (sets.indexOfChild(setView) > 0) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) setView.getLayoutParams();
            params.leftMargin = getContext().getResources().getDimensionPixelSize(R.dimen.content_margin);
        }
    }

    public void setRow(Row row) {
        mRow = row;

        // update exercise, rest etc.
        update();

        // update sets
        // sets.removeAllViews(); THIS WOULD DELETE THE ADD SET BUTTON, TOO!
        while (sets.getChildCount() > 1) {
            sets.removeViewAt(0);
        }
        for (Set set : row.getSets()) {
            addSet(set);
        }
    }

    public void update() {
        if (mRow == null) return;

        if (name != null)
            name.setText(mRow.getExercise().getName());

        if (note != null)
            note.setText(mRow.getNote());

        if (noteEdit != null) {
            noteEdit.setText(mRow.getNote());
        }

        if (rest != null)
            rest.setRest(mRow.getRest());

        if (flipper != null)
            flipper.setVisibility(
                    showNote && note.getText().length() > 0 || expanded ? VISIBLE : GONE
            );
    }

    public void showNote(boolean show) {
        showNote = show;
        update();
    }

    private void showAddSetButton(boolean show) {
        sets.getChildAt(sets.getChildCount()-1).setVisibility(show ? VISIBLE : GONE);
    }

    public void setExpanded(boolean expanded) {
        if (expanded == this.expanded) return;
        this.expanded = expanded;
        flipper.showNext();
        flipper.setVisibility(
                showNote && note.getText().length() > 0 || expanded ? VISIBLE : GONE
        );
        icMore.setVisibility(expanded ? VISIBLE : GONE);
        rest.setActivated(expanded);

        showAddSetButton(expanded);
        rest.setClickable(expanded);
    }

    public void removeSet(int pos) {
        sets.removeViewAt(pos);
    }

    public SetView findSetView(Set set) {
        return (SetView) sets.getChildAt(set.getPosition());
    }

    public SetView getSetView(int pos) {
        return (SetView) sets.getChildAt(pos);
    }

    public void updateSelectionState() {
        boolean select = false;
        int pos;
        for (pos = 0; pos < sets.getChildCount()-1; pos++) {
            SetView s = (SetView) sets.getChildAt(pos);
            if (s.isSelected()) {
                select = true;
                break;
            }
        }
        setSelected(select);

        if (select) {
            // scroll to current set (or following if exists)
            // next set in row if exists or current one
            View nextView = sets.getChildAt(
                    Math.min(pos+1, sets.getChildCount() -1));
            View prevView = pos > 0 ? sets.getChildAt(pos -1) : sets.getChildAt(pos);

            if (nextView.getRight() - scrollView.getScrollX() > scrollView.getWidth())
                scrollView.smoothScrollTo(nextView.getRight() - scrollView.getWidth(), 0);
            else if (prevView.getLeft() - scrollView.getScrollX() < 0)
                scrollView.smoothScrollTo(prevView.getLeft(), 0);
        }
    }

    public Row getRow() {
        return mRow;
    }

    public void setRowListener(RowListener listener) {
        this.listener = listener;
    }

    public void setRowDragListener(RowDragListener listener) {
        setOnTouchListener(listener);
        setOnDragListener(listener);
    }

    // called when row is being dragged
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
        refreshDrawableState();
    }

    // called when row is activated in selection mode
    @Override
    public void setActivated(boolean activated) {
        this.activated = activated;
        refreshDrawableState();
    }

    // called when row is currently active
    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        refreshDrawableState();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public boolean isDragging() {
        return dragging;
    }

    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public boolean dispatchDragEvent(DragEvent ev){
        boolean r = super.dispatchDragEvent(ev);
        if (r && (ev.getAction() == DragEvent.ACTION_DRAG_STARTED
                || ev.getAction() == DragEvent.ACTION_DRAG_ENDED)){
            // If we got a start or end and the return value is true, our
            // onDragEvent wasn't called by ViewGroup.dispatchDragEvent
            // So we do it here.
            onDragEvent(ev);
        }
        return r;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (dragging)
            mergeDrawableStates(drawableState, STATE_DRAGGING);
        if (selected)
            mergeDrawableStates(drawableState, new int[]{android.R.attr.state_selected});
        return drawableState;
    }

    public interface RowListener {
        void onSetClicked(Set set, boolean longClick);
        void onAddSetClicked();
        void onNoteChanged(String note);
        void onRestClicked();
        void onEditGoals();
        void onDelete();
        void onEditExercise();
    }
}
