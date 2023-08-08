package com.mycompany.gains.Activities.WorkoutEditorOld.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.R;
import com.mycompany.gains.widgets.NoAutoFocusEditText;
import com.mycompany.gains.Activities.WorkoutEditor.HoldListener;

public class SetEditorFragment extends Fragment {

    private Set mSet;
    private int mPosition;
    private Activity mActivity;

    NoAutoFocusEditText note;
    EditText reps;
    EditText weight;

    private int holdTimeOut;

    private OnSetEditorFragmentInteractionListener mListener;

    public static final String TAG = "SET_EDITOR_FRAGMENT";

    public static SetEditorFragment newInstance(int position) {
        SetEditorFragment fragment = new SetEditorFragment();
        Bundle args = new Bundle();
        args.putInt("pos", position);
        fragment.setArguments(args);
        return fragment;
    }

    public SetEditorFragment() {
        // Required empty public constructor
    }

    public interface OnSetEditorFragmentInteractionListener {
        void onSetChanged(Set set);
        void onRowChanged(Row row);
        void onLayoutSizeChanged();
    }

    public void setSet(Set set) {
        this.mSet = set;
    }

    public Set getSet() {
        return this.mSet;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mPosition = args.getInt("pos");

        holdTimeOut = getResources().getInteger(R.integer.holdButton_timeOut);
        //mSet = args.getParcelable("set");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNote();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_set_editor_old, container, false);
        v.setTag(TAG + "_" + mPosition);

        initializeViews(v);
        fillInData();

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        try {
            mListener = (OnSetEditorFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    public void initializeViews(View v) {
        // find views
        reps = (EditText) v.findViewById(R.id.reps);
        weight = (EditText) v.findViewById(R.id.weight);
        note = (NoAutoFocusEditText) v.findViewById(R.id.note);

        note.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mListener.onLayoutSizeChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // disable movement of set editor when keyboard is shown
        note.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mActivity.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

                    // adjust font color
                    note.setTextColor(getResources().getColor(R.color.secondary_text));
                }

                if (!hasFocus) {
                    // persist change to database
                    if (!(mSet.getRow().getNote().equals(note.getText().toString()))) {
                        Log.i("LEL", "setNote(" + note.getText().toString() + ")");
                        mSet.getRow().setNote(note.getText().toString());
                        mListener.onRowChanged(mSet.getRow());
                    }

                    // adjust font color
                    if (note.getText().length() == 0)
                        note.setTextColor(getResources().getColor(R.color.disabled_text));

                    // hide keyboard
                    InputMethodManager imm = (InputMethodManager) note.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(note.getWindowToken(), 0);
                }
            }
        });

        // lose focus when keyboard is hidden
        note.setOnImeBackListener(new NoAutoFocusEditText.OnImeBackListener() {
            @Override
            public void onImeBack(NoAutoFocusEditText ctrl) {
                note.clearFocus();
            }
        });
        note.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    note.clearFocus();
                    return true;
                }
                else
                    return false;
            }
        });

        v.findViewById(R.id.reps_up).setOnTouchListener(new HoldListener(holdTimeOut) {
            @Override
            public void onClick(View v) {
                int old = reps.getText().length() > 0 ? Integer.parseInt(reps.getText().toString())
                        : reps.getHint().length() > 0 ? Integer.parseInt(reps.getHint().toString()) : 0;
                mSet.setReps(old + 1);
                mSet.setIsDone(true);
                fillInData();
                mListener.onSetChanged(mSet);
            }
            @Override
            public void onRelease(View v) {

            }
        });
        v.findViewById(R.id.reps_down).setOnTouchListener(new HoldListener(holdTimeOut) {
            @Override
            public void onClick(View v) {
                int old = reps.getText().length() > 0 ? Integer.parseInt(reps.getText().toString())
                        : reps.getHint().length() > 0 ? Integer.parseInt(reps.getHint().toString()) : 0;
                mSet.setReps(old > 1 ? old - 1 : 0);
                mSet.setIsDone(true);
                fillInData();
                mListener.onSetChanged(mSet);
            }
            @Override
            public void onRelease(View v) {

            }
        });
        v.findViewById(R.id.weight_up).setOnTouchListener(new HoldListener(holdTimeOut) {
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

                fillInData();
                mListener.onSetChanged(mSet);
            }
            @Override
            public void onRelease(View v) {

            }
        });
        v.findViewById(R.id.weight_down).setOnTouchListener(new HoldListener(holdTimeOut) {
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

                fillInData();
                mListener.onSetChanged(mSet);
            }
            @Override
            public void onRelease(View v) {

            }
        });

        reps.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    mActivity.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                else {
                    int newVal = reps.getText().length() > 0 ? Integer.parseInt(reps.getText().toString())
                            : reps.getHint().length() > 0 ? Integer.parseInt(reps.getHint().toString()) : 0;

                    if (newVal != mSet.getReps()) {
                        mSet.setReps(newVal);
                        mSet.setIsDone(true);
                        fillInData();
                        mListener.onSetChanged(mSet);
                    }
                }
            }
        });

        weight.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    mActivity.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                else {
                    InputMethodManager imm = (InputMethodManager)weight.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(weight.getWindowToken(), 0);

                    int newVal = Set.normalizeWeight(
                            weight.getText().length() > 0 ? weight.getText().toString()
                                    : weight.getHint().length() > 0 ? weight.getHint().toString() : "0"
                    );

                    if (newVal != mSet.getWeight()) {
                        mSet.setWeight(newVal);
                        mSet.setIsDone(true);
                        fillInData();
                        mListener.onSetChanged(mSet);
                    }
                }
            }
        });
        weight.setImeOptions(EditorInfo.IME_ACTION_DONE);
        weight.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    weight.clearFocus();
                    return false;
                } else
                    return false;
            }
        });
    }

    public void fillInData() {
        // display information

        if (mSet.isDone()) {
            reps.setText(mSet.getReps() + "");
            weight.setText(mSet.getWeight() > 0 ? mSet.getWeightFormatted(false) : "");
        }
        else {
            reps.setHint(mSet.getReps() > 0 ? mSet.getReps() + "" : "");
            weight.setHint(mSet.getWeight() > 0 ? mSet.getWeightFormatted(false) : "");
        }

        updateNote();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void updateNote() {
        if (note == null) return;

        if (mSet != null)
            note.setText(mSet.getRow().getNote());

        note.setTextColor(
                note.getText().length() > 0 ? getResources().getColor(R.color.secondary_text)
                        : getResources().getColor(R.color.disabled_text));
    }
}
