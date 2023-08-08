package com.mycompany.gains.Activities.WorkoutEditor.GoalEditor;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycompany.gains.Activities.WorkoutEditor.HoldListener;
import com.mycompany.gains.Data.Model.Goal;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Stuff.Misc;
import com.mycompany.gains.R;


public class GoalEditorPagerFragment extends Fragment {
    
    private Goal mGoal;
    private Activity mActivity;
    private int holdTimeOut;

    public static final String TAG = "GOAL_EDITOR_PAGER_FRAGMENT";

    public static GoalEditorPagerFragment newInstance(Goal goal) {
        GoalEditorPagerFragment fragment = new GoalEditorPagerFragment();
        /*Bundle args = new Bundle();
        fragment.setArguments(args);*/
        fragment.mGoal = goal;
        return fragment;
    }

    public GoalEditorPagerFragment() {
        // Required empty public constructor
    }

    public void setGoal(Goal goal) {
        mGoal = goal;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Bundle args = getArguments();

        holdTimeOut = getResources().getInteger(R.integer.holdButton_timeOut);
        //mRow = args.getParcelable("row");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.dialog_goal_editor_pager_fragment, container, false);

        final TextView sets = (TextView) v.findViewById(R.id.sets);
        sets.setHint(mGoal.sets > 0 ? (mGoal.sets + "") : "");

        v.findViewById(R.id.sets_up).setOnTouchListener(new HoldListener(2 * holdTimeOut) {
            @Override
            public void onClick(View v) {
                int oldVal = sets.getText().length() > 0 ? Integer.parseInt(sets.getText().toString())
                        : sets.getHint().length() > 0 ? Integer.parseInt(sets.getHint().toString()) : 0;
                int newVal = oldVal + 1;
                sets.setText("" + newVal);
                mGoal.sets = newVal;

            }
            @Override
            public void onRelease(View v) {

            }
        });
        v.findViewById(R.id.sets_down).setOnTouchListener(new HoldListener(2 * holdTimeOut) {
            @Override
            public void onClick(View v) {
                int oldVal = sets.getText().length() > 0 ? Integer.parseInt(sets.getText().toString())
                        : sets.getHint().length() > 0 ? Integer.parseInt(sets.getHint().toString()) : 0;
                int newVal = oldVal > 2 ? oldVal - 1 : 1;
                sets.setText("" + newVal);
                mGoal.sets = newVal;
            }
            @Override
            public void onRelease(View v) {

            }
        });

        final TextView reps = (TextView) v.findViewById(R.id.reps);
        reps.setHint(mGoal.reps > 0 ? (mGoal.reps + "") : "");

        v.findViewById(R.id.reps_up).setOnTouchListener(new HoldListener(holdTimeOut) {
            @Override
            public void onClick(View v) {
                int oldVal = reps.getText().length() > 0 ? Integer.parseInt(reps.getText().toString())
                        : reps.getHint().length() > 0 ? Integer.parseInt(reps.getHint().toString()) : 0;
                int newVal = oldVal +1;
                reps.setText("" + newVal);
                mGoal.reps = newVal;

            }
            @Override
            public void onRelease(View v) {

            }
        });
        v.findViewById(R.id.reps_down).setOnTouchListener(new HoldListener(holdTimeOut) {
            @Override
            public void onClick(View v) {
                int oldVal = reps.getText().length() > 0 ? Integer.parseInt(reps.getText().toString())
                        : reps.getHint().length() > 0 ? Integer.parseInt(reps.getHint().toString()) : 0;
                int newVal = oldVal > 1 ? oldVal - 1 : 0;
                reps.setText("" + newVal);
                mGoal.reps = newVal;
            }
            @Override
            public void onRelease(View v) {

            }
        });

        final TextView weight = (TextView) v.findViewById(R.id.weight);
        weight.setHint(mGoal.weight > 0 ? Set.formatWeight(mGoal.weight, false) : "");

        v.findViewById(R.id.weight_up).setOnTouchListener(new HoldListener(holdTimeOut) {
            @Override
            public void onClick(View v) {
                String olds = weight.getText().length() > 0 ? weight.getText().toString()
                        : weight.getHint().toString();
                int oldVal = Set.normalizeWeight(olds.length() > 0 ? olds : "0");

                int increment = mGoal.exercise.getDefaultIncrement();
                increment = increment > 0 ? increment : 500;

                int newVal = oldVal + increment;

                weight.setText(Set.formatWeight(newVal, false));
                mGoal.weight = newVal;
            }
            @Override
            public void onRelease(View v) {

            }
        });
        v.findViewById(R.id.weight_down).setOnTouchListener(new HoldListener(holdTimeOut) {
            @Override
            public void onClick(View v) {
                String olds = weight.getText().length() > 0 ? weight.getText().toString()
                        : weight.getHint().toString();
                int oldVal = Set.normalizeWeight(olds.length() > 0 ? olds : "0");

                int increment = mGoal.exercise.getDefaultIncrement();
                increment = increment > 0 ? increment : 500;

                int newVal = oldVal > increment ? oldVal - increment : 0;

                weight.setText(Set.formatWeight(newVal, false));
                mGoal.weight = newVal;
            }
            @Override
            public void onRelease(View v) {

            }
        });

        final TextView rest = (TextView) v.findViewById(R.id.rest);
        rest.setHint(mGoal.rest > 0 ? (mGoal.rest + "") : "");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final int restIncrement = Integer.parseInt(prefs.getString(getString(R.string.pref_key_rest_increment), "15"));

        v.findViewById(R.id.rest_up).setOnTouchListener(new HoldListener(3 * holdTimeOut) {
            @Override
            public void onClick(View v) {
                int oldVal = rest.getText().length() > 0 ? Integer.parseInt(rest.getText().toString())
                        : rest.getHint().length() > 0 ? Integer.parseInt(rest.getHint().toString()) : 0;
                int newVal = Misc.roundTime(oldVal + restIncrement, restIncrement);
                rest.setText("" + newVal);
                mGoal.rest = newVal;
            }
            @Override
            public void onRelease(View v) {

            }
        });
        v.findViewById(R.id.rest_down).setOnTouchListener(new HoldListener(3 * holdTimeOut) {
            @Override
            public void onClick(View v) {
                int oldVal = rest.getText().length() > 0 ? Integer.parseInt(rest.getText().toString())
                        : rest.getHint().length() > 0 ? Integer.parseInt(rest.getHint().toString()) : 0;
                int newVal = oldVal > restIncrement ? Misc.roundTime(oldVal - restIncrement, restIncrement) : 0;
                rest.setText("" + newVal);
                mGoal.rest = newVal;
            }
            @Override
            public void onRelease(View v) {

            }
        });


        sets.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)
                    mGoal.sets = sets.getText().length() > 0 ? Integer.parseInt(sets.getText().toString()) 
                            : sets.getHint().length() > 0 ? Integer.parseInt(sets.getHint().toString()) : 0;
            }
        });
        reps.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)
                    mGoal.reps = reps.getText().length() > 0 ? Integer.parseInt(reps.getText().toString())
                            : reps.getHint().length() > 0 ? Integer.parseInt(reps.getHint().toString()) : 0;
            }
        });
        weight.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)
                    mGoal.weight = weight.getText().length() > 0 ? Set.normalizeWeight(weight.getText().toString()) 
                            : weight.getHint().length() > 0 ? Set.normalizeWeight(weight.getHint().toString()) : 0;
            }
        });
        rest.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus)
                    mGoal.rest = rest.getText().length() > 0 ? Integer.parseInt(rest.getText().toString())
                            : rest.getHint().length() > 0 ? Integer.parseInt(rest.getHint().toString()) : 0;
            }
        });

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
