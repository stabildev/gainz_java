package com.mycompany.gains.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;

import com.mycompany.gains.Activities.WorkoutEditor.CustomTextWatcher;
import com.mycompany.gains.Activities.WorkoutEditor.HoldListener;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.R;

/**
 * Created by Klee on 10.11.2015.
 */
public class SetEditorView extends TableLayout {
    private Set mSet;
    private SetEditorListener mListener;

    private EditText reps;
    private EditText weight;
    private CustomTextWatcher repsListener;
    private CustomTextWatcher weightListener;

    public SetEditorView(Context context) {
        super(context);
        init();
    }

    public SetEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setSet(Set set) {
        Log.i("SE2", "setSet() for set " + set.getCoordinates());
        mSet = set;
        update();

    }

    public void setListener(SetEditorListener listener) {
        mListener = listener;
    }

    private void init() {
        // inflate view
        LayoutInflater inflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.set_editor_view, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // find views
        reps = (EditText) findViewById(R.id.reps);
        weight = (EditText) findViewById(R.id.weight);
        View repsUp = findViewById(R.id.reps_up);
        View repsDown = findViewById(R.id.reps_down);
        View weightUp = findViewById(R.id.weight_up);
        View weightDown = findViewById(R.id.weight_down);

        // add EditText listeners
        repsListener = new CustomTextWatcher(0) {
            @Override
            public void onFocusLostOrTimeOut(String text) {
                int newVal = text.length() > 0 ? Integer.parseInt(text) : 0;

                if (newVal != mSet.getReps()) {
                    mSet.setReps(newVal);
                    mSet.setIsDone(true);
                    fillInData();
                    if (mListener != null)
                        mListener.onSetChanged(mSet);
                }
            }
        };
        reps.setOnFocusChangeListener(repsListener);
        reps.addTextChangedListener(repsListener);

        weightListener = new CustomTextWatcher(0) {
            @Override
            public void onFocusLostOrTimeOut(String text) {
                int newVal = Set.normalizeWeight(
                        text.length() > 0 ? text : "0"
                );

                if (newVal != mSet.getWeight()) {
                    mSet.setWeight(newVal);
                    mSet.setIsDone(mSet.getReps() > 0);
                    fillInData();
                    if (mListener != null)
                        mListener.onSetChanged(mSet);
                }
            }
        };
        weight.setOnFocusChangeListener(weightListener);
        weight.addTextChangedListener(weightListener);

        // add Button listeners
        int interval = getResources().getInteger(R.integer.holdButton_timeOut);
        repsUp.setOnTouchListener(new HoldListener(interval) {
            @Override
            public void onClick(View v) {
                int old = reps.getText().length() > 0 ? Integer.parseInt(reps.getText().toString())
                        : reps.getHint().length() > 0 ? Integer.parseInt(reps.getHint().toString()) : 0;
                mSet.setReps(old + 1);
                mSet.setIsDone(true);
                fillInData();
            }

            @Override
            public void onRelease(View v) {
                if (mListener != null)
                    mListener.onSetChanged(mSet);
            }
        });
        repsDown.setOnTouchListener(new HoldListener(interval) {
            @Override
            public void onClick(View v) {
                int old = reps.getText().length() > 0 ? Integer.parseInt(reps.getText().toString())
                        : reps.getHint().length() > 0 ? Integer.parseInt(reps.getHint().toString()) : 0;
                mSet.setReps(old > 1 ? old - 1 : 0);
                mSet.setIsDone(true);
                fillInData();
            }

            @Override
            public void onRelease(View v) {
                if (mListener != null)
                    mListener.onSetChanged(mSet);
            }
        });
        weightUp.setOnTouchListener(new HoldListener(interval) {
            @Override
            public void onClick(View v) {
                int old = Set.normalizeWeight(
                        weight.getText().length() > 0 ? weight.getText().toString()
                                : weight.getHint().length() > 0 ? weight.getHint().toString() : "0"
                );
                int increment = mSet.getRow().getExercise().getDefaultIncrement();
                increment = increment > 0 ? increment : 500;

                mSet.setWeight(old + increment);

                if (mSet.getReps() > 0)
                    mSet.setIsDone(true);

                fillInData(false);
            }

            @Override
            public void onRelease(View v) {
                fillInData();
                if (mListener != null)
                    mListener.onSetChanged(mSet);
            }
        });
        weightDown.setOnTouchListener(new HoldListener(interval) {
            @Override
            public void onClick(View v) {
                int old = Set.normalizeWeight(
                        weight.getText().length() > 0 ? weight.getText().toString()
                                : weight.getHint().length() > 0 ? weight.getHint().toString() : "0"
                );
                int increment = mSet.getRow().getExercise().getDefaultIncrement();
                increment = increment > 0 ? increment : 500;
                int newval = old - increment > 0 ? old - increment : 0;

                mSet.setWeight(newval);

                if (mSet.getReps() > 0)
                    mSet.setIsDone(true);

                fillInData(false);
            }

            @Override
            public void onRelease(View v) {
                fillInData();
                if (mListener != null)
                    mListener.onSetChanged(mSet);
            }
        });
    }

    private void fillInData() {
        fillInData(true);
    }

    private void fillInData(boolean stripTrailingZeros) {
        // display information

        reps.setHint(mSet.getReps() > 0 ? mSet.getReps() + "" : "");
        weight.setHint(mSet.getWeight() > 0 ? mSet.getWeightFormatted(stripTrailingZeros) : "");

        if (mSet.isDone()) {
            if (!reps.hasFocus())
                reps.setText("" + mSet.getReps());
            if (!weight.hasFocus())
                weight.setText(mSet.getWeight() > 0 ? mSet.getWeightFormatted(stripTrailingZeros) : "");
        }
        else {
            if (!reps.hasFocus())
                reps.setText("");
            if (!weight.hasFocus())
                weight.setText("");
        }
    }

    public void update() {
        repsListener.disable();
        weightListener.disable();
        fillInData();
        repsListener.enable();
        weightListener.enable();
    }

    public interface SetEditorListener {
        void onSetChanged(Set set);
    }
}
