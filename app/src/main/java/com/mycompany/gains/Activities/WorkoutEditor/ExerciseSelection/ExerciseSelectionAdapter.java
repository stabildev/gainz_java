package com.mycompany.gains.Activities.WorkoutEditor.ExerciseSelection;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycompany.gains.Adapters.CheckBoxRecyclerAdapter;
import com.mycompany.gains.Data.Model.Exercise;
import com.mycompany.gains.Stuff.Misc;
import com.mycompany.gains.R;
import com.mycompany.gains.widgets.NoAutoFocusEditText;
import com.mycompany.gains.widgets.EmptyRecyclerView;

import java.util.Collections;
import java.util.List;

public class ExerciseSelectionAdapter extends CheckBoxRecyclerAdapter<RecyclerView.ViewHolder>
        implements EmptyRecyclerView.ContainsHeaders{
    private final static int VT_TOP = 0;
    private final static int VT_ITEM = 1;

    private final static int ITEM_OFFSET = 1;

    private List<Exercise> mExerciseList;
    private ExerciseSelectionListener mListener;

    public ExerciseSelectionAdapter(Context context, List<Exercise> exercises){
        super(context);
        mExerciseList = exercises;
    }

    public interface ExerciseSelectionListener extends CheckBoxRecyclerAdapter.OnSelectionListener {
        void onAddExercise(String name);
    }

    public void setOnSelectionListener(ExerciseSelectionListener listener) {
        mListener = listener;
        super.setOnSelectionListener(new CheckBoxRecyclerAdapter.OnSelectionListener() {
            @Override
            public void onSelectionChanged(int selectionSize) {
                mListener.onSelectionChanged(selectionSize);
            }
            @Override
            public void onLongClick(int id) {
                mListener.onLongClick(id);
            }
        });
    }

    public static class AddExerciseViewHolder extends RecyclerView.ViewHolder {
        ImageView button;
        NoAutoFocusEditText editText;

        public AddExerciseViewHolder(View v) {
            super(v);
            button = (ImageView) v.findViewById(R.id.btn_ok);
            editText = (NoAutoFocusEditText) v.findViewById(R.id.name);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    boolean enabled = editText.getText().length() > 0;

                    Misc.animateAlpha(button, enabled ? 1f : 0f);
                    button.setClickable(enabled);
                    button.setFocusable(enabled);
                }
            });
            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    Misc.animateAlpha(
                            (View) editText.getParent(),
                            hasFocus || editText.getText().length() > 0 ? 1 : .5f);
                }
            });
            ((View) editText.getParent()).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editText.requestFocus();

                    final InputMethodManager imm = (InputMethodManager) editText.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                }
            });
            editText.setOnImeBackListener(new NoAutoFocusEditText.OnImeBackListener() {
                @Override
                public void onImeBack(NoAutoFocusEditText ctrl) {
                    editText.clearFocus();
                }
            });
            editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE && button.isClickable()) {
                        button.performClick();
                        return true;
                    }
                    else
                        return false;
                }
            });
        }
    }

    public static class ItemViewHolder extends CheckBoxRecyclerAdapter.CheckBoxViewHolder {
        TextView name;

        public ItemViewHolder(View v) {
            super(v);
            checkBox = (CheckBox) v.findViewById(R.id.item_exerciseSelection_multi_checkBox);
            name = (TextView) v.findViewById(R.id.item_exerciseSelection_multi_name);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case VT_TOP:
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_exercise_selection_add, parent, false);
                return new AddExerciseViewHolder(itemView);
            default:
                itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_exercise_selection, parent, false);
                return new ItemViewHolder(itemView);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VT_TOP : VT_ITEM;
    }

    @Override
    public int getItemCount() {
        return mExerciseList.size() + ITEM_OFFSET;
    }

    public int addExercise(Exercise newExercise) {
        mExerciseList.add(newExercise);
        Collections.sort(mExerciseList);
        int pos = mExerciseList.indexOf(newExercise);
        int adapterPos = pos + ITEM_OFFSET;
        insertItem(adapterPos); // move selection
        toggleSelection(adapterPos, newExercise.get_id());
        notifyItemInserted(adapterPos);
        return adapterPos;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        switch (getItemViewType(position)) {
            case VT_TOP: {
                final AddExerciseViewHolder holder = (AddExerciseViewHolder) viewHolder;
                holder.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onAddExercise(holder.editText.getText().toString());
                        holder.editText.setText("");

                        // hide keyboard and clear focus
                        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(holder.editText.getWindowToken(), 0);
                        holder.editText.clearFocus();
                    }
                });
                // doesn't work in xml:
                holder.button.setClickable(false);
                holder.button.setFocusable(false);
                return;
            }
            default: {
                final ItemViewHolder holder = (ItemViewHolder) viewHolder;
                super.onBindViewHolder(holder, position);

                final Exercise exercise = mExerciseList.get(position - ITEM_OFFSET);

                holder.setId(exercise.get_id());
                holder.name.setText(exercise.getName());
            }
        }
    }

    @Override
    public int getRowOffset() {
        return ITEM_OFFSET;
    }
}