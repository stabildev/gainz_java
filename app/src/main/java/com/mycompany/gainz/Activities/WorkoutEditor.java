package com.mycompany.gainz.Activities;

import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.gainz.Adapters.WorkoutEditorListAdapter;
import com.mycompany.gainz.Adapters.SetEditorPagerAdapter;
import com.mycompany.gainz.Classes.EmptyRecyclerView;
import com.mycompany.gainz.Database.Helper.DBHelper;
import com.mycompany.gainz.Database.Model.Exercise;
import com.mycompany.gainz.Database.Model.Set;
import com.mycompany.gainz.Database.Model.Workout;
import com.mycompany.gainz.Dialogs.ExerciseSelectionDialog;
import com.mycompany.gainz.Fragments.SetEditorFragment;
import com.mycompany.gainz.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

public class WorkoutEditor extends AppCompatActivity
        implements SetEditorFragment.OnSetEditorFragmentInteractionListener,
        ExerciseSelectionDialog.OnDialogInteractionListener {

    private DBHelper db;

    private ActionBar mToolbar;

    private ViewPager mViewPager;
    private SetEditorPagerAdapter mPagerAdapter;

    private WorkoutEditorListAdapter mListAdapter;
    private EmptyRecyclerView mWorkoutList;

    private Workout mWorkout;
    private Set mCurrentSet;

    private boolean changed = false;
    private boolean started = false;

    private boolean edit;

    private final static String TAG = "WorkoutWindow";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_editor);

        Bundle bundle = getIntent().getExtras();
        int[] intArray = bundle.getIntArray("makeFrom");
        if (intArray.length == 0) intArray = new int[]{-1};

        edit = bundle.getInt("edit",0) == 1;

        db = DBHelper.getInstance(this);
        initializeWorkout(intArray);

        // Set up toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mToolbar = getSupportActionBar();

        int title;
        if (edit)
            title = mWorkout.isRoutine() ? R.string.edit_routine_title : R.string.edit_workout_title;
        else
            title = R.string.new_workout_title;

        if (mToolbar != null) {
            mToolbar.setTitle(title);
            mToolbar.setHomeButtonEnabled(true);
            mToolbar.setDisplayHomeAsUpEnabled(true);
        }

        setupEditArea();
        setupWorkoutList();
    }

    @Override
    public void onBackPressed() {
        if (!changed && !started && !edit) {
            db.dropWorkout(mWorkout.get_id());
            //Toast.makeText(this, "Dropped unchanged workout from database", Toast.LENGTH_SHORT).show();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_workout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_pause_resume_workout) {
            if (item.getTitle().equals(getString(R.string.action_pause_workout))) {
                item.setIcon(R.drawable.ic_play_arrow_white_24dp)
                        .setTitle(R.string.action_resume_workout);
            } else {
                if (!started && mCurrentSet != null) {
                    item.setIcon(R.drawable.ic_pause_white_24dp)
                            .setTitle(R.string.action_pause_workout);

                    mWorkout.setDate(new GregorianCalendar());
                    db.updateWorkout(mWorkout);
                    started = true;
                }
            }
        } else if (id == R.id.action_add_exercise) {

            List<Exercise> exerciseList = new ArrayList<>(db.getExercises().values());
            Collections.sort(exerciseList);

            String[] exerciseNames = new String[exerciseList.size()];
            int[] exerciseIDs = new int[exerciseList.size()];

            for (int i = 0; i < exerciseList.size(); i++) {
                exerciseNames[i] = exerciseList.get(i).getName();
                exerciseIDs[i] = exerciseList.get(i).get_id();
            }

            ExerciseSelectionDialog dialog = ExerciseSelectionDialog.newInstance(exerciseNames, exerciseIDs);
            dialog.show(getFragmentManager(), "dialog");
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSetEditorIconClick(Set set, String s) {
        if (s.equals("addSet")) {
            Set newSet = new Set(mWorkout, set.getSuperset(), set.getRow(), set.getExercise());
            db.addSet(newSet);

            if (mCurrentSet == null)
                mCurrentSet = newSet;

            changed = true;
            mPagerAdapter.notifyDataSetChanged();
            mListAdapter.notifyDataSetChanged();
            updateEditNavigation();
        } else if (s.equals("addNote")) {
            db.addSupersetNote(set);
            mWorkout.supersetNotes.put(set.getSuperset(), "TEST");
            // TODO
        } else if (s.equals("skipSet")) {
            // TODO
        } else if (s.equals("deleteSet")) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);

            mWorkout.setsInRows.values().remove(set);
            for (Set i : mWorkout.setsInRows.get(set.getRow()))
                if (i.posInRow() > set.posInRow())
                    i.setPosInRow(i.posInRow() - 1);

            db.dropSet(set.get_id());

            changed = true;
            mPagerAdapter.notifyDataSetChanged();
            mListAdapter.notifyDataSetChanged();
            updateEditNavigation();
        }
    }

    public void onLeftButtonClick(View v) {
        int pos = mViewPager.getCurrentItem();
        mViewPager.setCurrentItem(pos - 1);
    }
    public void onRightButtonClick(View v) {
        int pos = mViewPager.getCurrentItem();
        mViewPager.setCurrentItem(pos + 1);
    }
    public void onSetChanged(Set set) {
        db.updateSet(set);
        changed = true;
    }

    public void updateEditNavigation() {
        if (mCurrentSet == null) {
            ((TextView) findViewById(R.id.editSet_exerciseName)).
                    setText(getString(R.string.no_exercise));
            ((TextView) findViewById(R.id.editSet_setNumber)).
                    setText(getString(R.string.set) + " -/-");
        } else {
            ((TextView) findViewById(R.id.editSet_exerciseName)).setText(mCurrentSet.getExercise().getName());
            ((TextView) findViewById(R.id.editSet_setNumber)).
                    setText(getString(R.string.set) + " " + mCurrentSet.posInRow() + "/"
                            + mWorkout.setsInRows.get(mCurrentSet.getRow()).size());
        }

        int pos = mViewPager.getCurrentItem();

        if (pos == 1) {
            View arrow = findViewById(R.id.editSet_arrowLeft);
            arrow.setAlpha(1f);
            arrow.setFocusable(true);
            arrow.setClickable(true);
        } if (pos == (mWorkout.setsInRows.values().size() -2)) {
            View arrow = findViewById(R.id.editSet_arrowRight);
            arrow.setAlpha(1f);
            arrow.setFocusable(true);
            arrow.setClickable(true);
        } if (pos == 0) {
            View arrow = findViewById(R.id.editSet_arrowLeft);
            arrow.setAlpha(.2f);
            arrow.setFocusable(false);
            arrow.setClickable(false);
        } if (pos == (mWorkout.setsInRows.values().size() -1)) {
            View arrow = findViewById(R.id.editSet_arrowRight);
            arrow.setAlpha(.2f);
            arrow.setFocusable(false);
            arrow.setClickable(false);
        }
    }

    private void initializeWorkout(int[] intArray) {
        Log.i(TAG, "Initialize workout...");
        // initialize workout

        if (intArray[0] == -1) {
            if (!edit)
                mWorkout = new Workout();
            else {
                Toast.makeText(this, "Couldn't find workout!", Toast.LENGTH_SHORT);
                this.finish();
            }
        } else if (intArray.length == 1) {
            Log.i(TAG, "Get data from database...");
            mWorkout = db.getWorkout(intArray[0]);
            if (!edit) {
                mWorkout.setIsRoutine(false);
                mWorkout.setDate(new GregorianCalendar());
            }
        } else {
            if (edit) {
                Toast.makeText(this, "Can't edit multiple workouts at once!", Toast.LENGTH_SHORT);
                this.finish();
            }
            Log.i(TAG, "Get data from database...\nMerge workouts...");
            List<Workout> workoutList = new ArrayList<>(intArray.length);
            for (int i : intArray)
                workoutList.add(db.getWorkout(i));
            mWorkout = mergeWorkouts(workoutList);
        }

        if (!edit) {
            db.addWorkout(mWorkout);
            for (Set s : mWorkout.setsInRows.values())
                s.setIsDone(false);
            //Toast.makeText(this, "Added workout to database. workout_id = " + mWorkout.get_id(), Toast.LENGTH_SHORT).show();
        }

        if (mWorkout.setsInRows.values().size() > 0)
            mCurrentSet = mWorkout.setsInRows.get(1).first();
    }

    public static Workout mergeWorkouts(List<Workout> workouts) {
        Workout merged = new Workout();
        List<String> names = new ArrayList<>(workouts.size());
        List<String> notes = new ArrayList<>(workouts.size());
        int rowOffset = 0, supersetOffset = 0;
        Set add;
        for (Workout w : workouts) {
            if (!w.getName().equals("") && !names.contains(w.getName()))
                names.add(w.getName());
            if (!w.getNote().equals("") && !notes.contains(w.getNote()))
                notes.add(w.getNote());

            // offset rows and supersets and add sets
            for (Set s : w.setsInRows.values()) {
                add = new Set(merged,
                        s.getSuperset() + supersetOffset,
                        s.getRow() + rowOffset,
                        s.getExercise());
                add.setReps(s.getReps());
                add.setWeight(s.getWeight());
                add.setRest(s.getRest());
            }

            // offset superset notes and add
            for (Integer key : w.supersetNotes.keySet())
                merged.supersetNotes.put(key + rowOffset, w.supersetNotes.get(key));

            rowOffset = merged.setsInRows.keySet().size();
            supersetOffset = rowOffset > 0 ? merged.setsInRows.get(rowOffset).first().getSuperset() : 0;
        }
        if (!names.isEmpty())
            merged.setName(TextUtils.join(",", names));
        if (!notes.isEmpty())
            merged.setNote(TextUtils.join(",", notes));

        return merged;
    }

    private void setupEditArea() {
        // Get the ViewPager and set its PagerAdapter so that it can display items
        mViewPager = (ViewPager) findViewById(R.id.viewPager_editSet);

        mPagerAdapter = new SetEditorPagerAdapter(getSupportFragmentManager(), mWorkout.setsInRows);
        mViewPager.setAdapter(mPagerAdapter);
        mCurrentSet = mPagerAdapter.getSet(mViewPager.getCurrentItem());

        // Change, update the current set in the workout list
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mCurrentSet = mPagerAdapter.getSet(mViewPager.getCurrentItem());
                updateEditNavigation();
            }
        });
        updateEditNavigation();
    }

    public void onExerciseSelected(int exercise_id) {

        int row = mWorkout.setsInRows.keySet().size();
        int superset = row > 0 ? mWorkout.setsInRows.get(row).first().getSuperset() : 0;

        Set newSet = new Set(mWorkout, ++superset, ++row, db.getExercises().get(exercise_id));
        db.addSet(newSet);

        if (mCurrentSet == null)
            mCurrentSet = newSet;

        changed = true;
//        keep = true;

        Log.i(TAG, "Added exercise with _id: " + exercise_id + ", superset: "
                + newSet.getSuperset() + " and row: " + newSet.getRow());

/*        if (!keep) {
            Toast.makeText(this, "Saved workout", Toast.LENGTH_SHORT).show();
            keep = true;
        }*/

        mPagerAdapter.notifyDataSetChanged();
        mListAdapter.notifyDataSetChanged();

        updateEditNavigation();
    }

    private void setupWorkoutList() {

        // get RecyclerView and set empty view
        mWorkoutList = (EmptyRecyclerView) findViewById(R.id.workoutList);
        mWorkoutList.setEmptyView(findViewById(R.id.workoutList_empty));

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mWorkoutList.setHasFixedSize(true);

        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        mWorkoutList.setLayoutManager(new LinearLayoutManager(this));

        // Set the adapter
        mListAdapter = new WorkoutEditorListAdapter(mWorkout.setsInRows);
        mWorkoutList.setAdapter(mListAdapter);
        mListAdapter.notifyDataSetChanged();
    }
}
