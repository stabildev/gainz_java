package com.mycompany.gains.Activities.WorkoutEditor.GoalEditor;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.mycompany.gains.Adapters.FixedFragmentStatePagerAdapter;
import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.Data.Model.Exercise;
import com.mycompany.gains.Data.Model.Goal;
import com.mycompany.gains.Stuff.Misc;
import com.mycompany.gains.R;
import com.mycompany.gains.widgets.WrapContentViewPager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mycompany.gains.Arguments.*;


public class GoalEditorDialog extends DialogFragment {

    private OnDialogInteractionListener mListener;
    private WrapContentViewPager mPager;
    private FixedFragmentStatePagerAdapter mAdapter;
    private List<Goal> mData;

    private TextSwitcher exerciseName;
    int routineId = -1;

    public static final boolean routineOnly = true;

    boolean rightButtonActive = false, leftButtonActive = false;

    public static GoalEditorDialog newInstance(List<Integer> exerciseIds, int routineId, OnDialogInteractionListener listener) {
        GoalEditorDialog dialog = new GoalEditorDialog();
        Bundle args = new Bundle();
        args.putIntegerArrayList(ARG_IDS, (ArrayList<Integer>) exerciseIds);
        args.putInt(ARG_ROUTINE, routineId);
        dialog.setArguments(args);
        dialog.mListener = listener;
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DatabaseHelper db = DatabaseHelper.getInstance(getActivity());

        // get arguments
        routineId = getArguments().getInt(ARG_ROUTINE, -1);
        final List<Integer> ids = getArguments().getIntegerArrayList(ARG_IDS);

        mData = new ArrayList<>(ids.size());

        Map<Integer, Exercise> exerciseMap = db.getExercises();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int restIncrement = Integer.parseInt(prefs.getString(getString(R.string.pref_key_rest_increment), "15"));

        for (int id : ids) {
            Goal goal = DatabaseHelper.compressGoals(
                    db.getGoals(exerciseMap.get(id), routineOnly ? routineId : null, 3));
            goal.rest = Misc.roundTime(goal.rest, restIncrement);
            goal.exercise = exerciseMap.get(id);
            mData.add(goal);
        }

        // create view
        final View view = inflater.inflate(R.layout.dialog_goal_editor, container);
        exerciseName = (TextSwitcher) view.findViewById(R.id.exercise_name);
        exerciseName.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                return (TextView) View.inflate(getActivity(), R.layout.dialog_goal_editor_exercise_name, null);
            }
        });

        final View leftButton = view.findViewById(R.id.arrow_left);
        final View rightButton = view.findViewById(R.id.arrow_right);

        final Button btnPositive = (Button) view.findViewById(R.id.positive_btn);
        final Button btnNegative = (Button) view.findViewById(R.id.negative_btn);

        mPager = (WrapContentViewPager) view.findViewById(R.id.goal_editor_pager);
        mAdapter = new FixedFragmentStatePagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return GoalEditorPagerFragment.newInstance(mData.get(position));
            }

            @Override
            public int getCount() {
                return mData.size();
            }

            @Override
            public String getTag(int position) {
                return position + "";
            }

            @Override
            public int getItemPosition(Object object) {
                return PagerAdapter.POSITION_NONE;
            }
        };

        mPager.setAdapter(mAdapter);

        // set up navigation buttons

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(mPager.getCurrentItem()-1);
            }
        });
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            }
        });

        // set up dialog buttons
        btnPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rightButtonActive)
                    mPager.setCurrentItem(mPager.getCurrentItem()+1);
                else {
                    if (!mData.isEmpty())
                        mListener.onGoalsChanged(getTag(), mData);
                    dismiss();
                }
            }
        });

        btnNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onGoalEditorNegative(getTag(), ids);
                dismiss();
            }
        });

        ViewPager.SimpleOnPageChangeListener vpListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                exerciseName.setText(mData.get(position).exercise.getName());

                // update navigation buttons
                leftButtonActive = position != 0;
                rightButtonActive = position != mAdapter.getCount() -1;
                leftButton.setClickable(leftButtonActive);
                Misc.animateAlpha(leftButton, leftButtonActive ? 1 : .2f);
                rightButton.setClickable(rightButtonActive);
                Misc.animateAlpha(rightButton, rightButtonActive ? 1 : .2f);

                btnPositive.setText(rightButtonActive ? R.string.dialog_next : R.string.dialog_finish);
            }
        };
        mPager.addOnPageChangeListener(vpListener);
        exerciseName.setCurrentText(mData.get(0).exercise.getName());
        vpListener.onPageSelected(0);

        Dialog dialog = getDialog();

        // fix bug: editText not showing keyboard
        dialog.getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        // disable title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }

    public interface OnDialogInteractionListener {
        void onGoalsChanged(String tag, List<Goal> goals);
        void onGoalEditorNegative(String tag, List<Integer> exerciseIds);
    }
}
