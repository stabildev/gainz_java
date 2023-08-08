package com.mycompany.gains.Activities.WorkoutEditor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.gains.Activities.BaseActivity;
import com.mycompany.gains.Activities.MainActivity.MainActivity;
import com.mycompany.gains.Activities.WorkoutEditor.ExerciseSelection.ExerciseSelectionDialog;
import com.mycompany.gains.Activities.WorkoutEditor.GoalEditor.GoalEditorDialog;
import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.Data.Model.Exercise;
import com.mycompany.gains.Data.Model.Goal;
import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Data.Model.Superset;
import com.mycompany.gains.Data.Model.Workout;
import com.mycompany.gains.Globals;
import com.mycompany.gains.R;
import com.mycompany.gains.widgets.SetEditor;
import com.mycompany.gains.widgets.SetEditorView;
import com.mycompany.gains.widgets.TimerView;
import com.mycompany.gains.widgets.WorkoutView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static com.mycompany.gains.Arguments.*;

public class WorkoutEditor extends BaseActivity implements WorkoutView.WorkoutListener,
        SetEditorView.SetEditorListener, ExerciseSelectionDialog.OnDialogInteractionListener,
        GoalEditorDialog.OnDialogInteractionListener, RestPickerDialog.RestPickerListener {

    private static final String TAG = "WE2";

    private DatabaseHelper db;
    private Workout mWorkout;

    // state flags
    private boolean edit;
    private boolean changed = false;
    private boolean started = false;
    private boolean ending = false;

    private boolean autoSkip;
    private boolean vibrate;
    private boolean notifying;
    private int beepAfter;

    // temp for selection
    private int selectionSize = 0;

    // components
    private ActionBar mToolbar;
    private CardView setEditorCard;
    private SetEditor setEditor;
    private WorkoutView workoutView;
    private TimerView timer;

    // broadcast receiver for timer ticks
    BroadcastReceiver timerReceiver;

    // vibrator service
    private Vibrator vibrator;
    private Ringtone finishRingtone;
    private Ringtone beepRingtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_editor);

        // initialize toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mToolbar = getSupportActionBar();

        if (mToolbar != null) {
            mToolbar.setHomeButtonEnabled(true);
            mToolbar.setDisplayHomeAsUpEnabled(true);
        }

        db = DatabaseHelper.getInstance(this);

        initialize();
    }

    @Override
    public void onBackPressed() {
        // hide Set Editor
        if (setEditorCard.getVisibility() != View.GONE) {
            setEditorCard.setVisibility(View.GONE);

            // deactivate set in Workout View if workout isn't started
            if (!started)
                workoutView.setActiveSet(null, true);
            return;
        }

        //  TODO collapse & deselect row; return;

        if (started || (changed && !edit)) {
            // stay open
            // back to main activity
            this.startActivityForResult(new Intent(this, MainActivity.class), 1234);
        } else {
            if (!changed && !edit) {
                // discard
                db.dropWorkout(mWorkout.getDatabaseId());
                Toast.makeText(this, getString(R.string.toast_discarded_workout), Toast.LENGTH_SHORT).show();
            }
            close();
        }
    }

    public void close() {
        Globals.getInstance().setActiveWorkoutId(-1);
        this.finish();
    }

    public void initialize() {
        // initialize workout
        mWorkout = initializeWorkout();

        if (mWorkout == null) {
            Toast.makeText(getApplicationContext(), "Workout not found.", Toast.LENGTH_SHORT).show();
            this.finish();
        } else if (!edit)
            db.addWorkout(mWorkout);

        // set the global variable for active workout
        Globals.getInstance().setActiveWorkoutId(mWorkout.getDatabaseId());

        // set toolbar title
        updateToolbarTitle();

        // initialize system services
        vibrator = (Vibrator) WorkoutEditor.this.getSystemService(VIBRATOR_SERVICE);

        setUpHeader();

        // initialize WorkoutView
        workoutView = (WorkoutView) findViewById(R.id.workout_view);
        workoutView.setEditMode(true);
        workoutView.setWorkout(mWorkout);
        workoutView.setWorkoutListener(this);

        setEditorCard = (CardView) findViewById(R.id.set_editor_card);
        setEditor = (SetEditor) findViewById(R.id.set_editor);
        setEditor.setListener(this);

        setUpTimerAndCountdownService();
    }

    private void updatePreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useTimer = prefs.getBoolean(getString(R.string.pref_key_enable_timer), false);
        int restIncrement = Integer.parseInt(prefs.getString(getString(R.string.pref_key_rest_increment), "15"));
        int timerMax = Integer.parseInt(prefs.getString(getString(R.string.pref_key_timer_max), "" + 5)) * 60;
        autoSkip = prefs.getBoolean(getString(R.string.pref_key_auto_skip), true);

        Uri alarm = Uri.parse(prefs.getString(getString(R.string.pref_key_alarm_sound), ""));
        Uri beep = Uri.parse(prefs.getString(getString(R.string.pref_key_acustic_countdown), ""));

        finishRingtone = !alarm.toString().equals("") ? RingtoneManager.getRingtone(WorkoutEditor.this, alarm) : null;
        beepRingtone = !beep.toString().equals("") ? RingtoneManager.getRingtone(WorkoutEditor.this, beep) : null;

        vibrate = prefs.getBoolean(getString(R.string.pref_key_vibrate_countdown), true);
        beepAfter = Integer.parseInt(prefs.getString(getString(R.string.pref_key_beep), "3"));

        timer.useTimer(useTimer);
        timer.setIncrement(restIncrement);
        timer.setMax(timerMax);
    }

    private void setUpHeader() {
        // initialize fields
        TextView workoutTitle = (TextView) findViewById(R.id.workout_title);
        TextView workoutDate = (TextView) findViewById(R.id.workout_date);
        TextView workoutNote = (TextView) findViewById(R.id.workout_note);

        // setting workout title
        workoutTitle.setText(mWorkout.getName());
        CustomTextWatcher f = new CustomTextWatcher(3000) {
            @Override
            public void onFocusLostOrTimeOut(String text) {
                mWorkout.setName(text);
                db.updateWorkout(mWorkout);
                changed = true;
//                Toast.makeText(WorkoutEditor.this, "Workout title changed to " + text, Toast.LENGTH_SHORT).show();
            }
        };
        workoutTitle.setOnFocusChangeListener(f);
        workoutTitle.addTextChangedListener(f);

        // setting workout note
        workoutNote.setText(mWorkout.getNote());
        f = new CustomTextWatcher(3000) {
            @Override
            public void onFocusLostOrTimeOut(String text) {
                mWorkout.setNote(text);
                db.updateWorkout(mWorkout);
                changed = true;
//                Toast.makeText(WorkoutEditor.this, "Workout note changed to " + text, Toast.LENGTH_SHORT).show();
            }
        };
        workoutNote.setOnFocusChangeListener(f);
        workoutNote.addTextChangedListener(f);

        // setting workout date
        Calendar date = mWorkout.getDate();
        DateFormat df = DateFormat.getDateInstance();
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        workoutDate.setText(df.format(date.getTime())
                + "\n" + tf.format(date.getTime()));
    }

    private void setUpTimerAndCountdownService() {
        // set up timer
        timer = (TimerView) findViewById(R.id.timer);
        timer.setTimerListener(new TimerView.TimerListener() {
            @Override
            public void onClick() {
                // start workout if not started
                if (!timer.isStarted()) {
                    startWorkout();
                }
            }

            @Override
            public void onStartCountdown(int initial) {
                markSetAsDone(setEditor.getSet());

                // start countdown service
                Intent intent = new Intent(WorkoutEditor.this, CountdownNotifyService.class);
                intent.putExtra(ARG_ACTION, ACTION_START);
                intent.putExtra(ARG_INITIAL, initial);
                WorkoutEditor.this.startService(intent);
            }

            @Override
            public void onSkipCountdown(boolean isTimerEnabled) {
                if (timer.isCounting()) {
                    // stop countdown
                    Intent intent = new Intent(WorkoutEditor.this, CountdownNotifyService.class);
                    intent.putExtra(ARG_ACTION, ACTION_CANCEL);
                    WorkoutEditor.this.startService(intent);

                    // update widget state
                    timer.setCounting(false);
                }
                else {
                    markSetAsDone(setEditor.getSet());
                }
                // go to next set
                goToNextSet(timer.getSet());
            }

            @Override
            public void onCancelCountdown() {
                // notify countdown service
                Intent intent = new Intent(WorkoutEditor.this, CountdownNotifyService.class);
                intent.putExtra(ARG_ACTION, ACTION_CANCEL);
                WorkoutEditor.this.startService(intent);
            }
        });

        // set up timer receiver
        timerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getStringExtra(ARG_ACTION)) {

                    case ACTION_FINISH:
                        if (timer != null) timer.setTime(0, !timer.isDragging());
                        // play alarm sound
                        if (!notifying) {
                            if (vibrate)
                                vibrator.vibrate(500);
                            if (finishRingtone != null)
                                finishRingtone.play();
                        }

                        // notify countdown widget
                        if (timer != null) timer.setCounting(false);

                        // go to next set
                        goToNextSet(setEditor.getSet());
                        return;

                    case ACTION_TICK:
                        int remaining = intent.getIntExtra(ARG_TIME, 0);
                        // update timer label
                        if (timer != null && !timer.isDragging())
                            timer.setTime(remaining, !timer.isDragging());

                        if (!notifying) {
                            if (remaining <= beepAfter) {
                                if (vibrate)
                                    vibrator.vibrate(100);
                                if (beepRingtone != null)
                                    beepRingtone.play();
                            }
                        }
                }
            }
        };
        registerReceiver(timerReceiver, new IntentFilter(CountdownNotifyService.COUNTDOWN_BR));
    }

    private void startNotify() {
        Set set = null;

        if (timer.isCounting()) {
            set = getNextSet(timer.getSet());
        }
        if (set == null)
            set = setEditor.getSet();

        Intent intent = new Intent(this, CountdownNotifyService.class);
        intent.putExtra(ARG_ACTION, ACTION_START_NOTIFY);
        intent.putExtra(ARG_PROGRESS, 38);//TODO mProgress.getProgress());
        String notifyText = "";
        if (timer.isCounting()) {
            notifyText += "Up next: ";
        }
        notifyText += set.getRow().getExercise().getName()
                + ", " + getString(R.string.set) + " "
                + (set.getPosition() + 1) + "/" + set.getRow().getSetCount();
        intent.putExtra(ARG_NOTIFY_TEXT, notifyText);
        startService(intent);
        notifying = true;
    }

    private void stopNotify() {
        Intent intent = new Intent(this, CountdownNotifyService.class);
        intent.putExtra(ARG_ACTION, ACTION_STOP_NOTIFY);
        startService(intent);
        notifying = false;
    }

    public void startWorkout() {
        if (mWorkout.getSuperset(0) == null)
            return;

        // set date if started for first time
        if (!started && !edit) {
            mWorkout.setDate(new GregorianCalendar());
            // TODO update header
            db.updateWorkout(mWorkout);
            // TODO start stopwatch
        }
        started = true;
        // to show "End workout" menu entry
        invalidateOptionsMenu();

        if (setEditor.getSet() == null)
            goToSet(mWorkout.getSuperset(0).getRow(0).getSet(0));

        timer.setStarted(true);
        timer.setEnabled(true);
    }

    private void markSetAsDone(Set set) {
        set.setIsDone(true);
        db.updateSet(set);
        workoutView.updateSet(set);
        if (set.equals(setEditor.getSet()))
            setEditor.update();
    }

    private void goToSet(Set set) {
        openSetEditor(set);
        timer.setSet(set);
        workoutView.setActiveSet(set, true);
        // TODO set autoskip
        // TODO recognize end of workout
    }

    private Set getNextSet(Set set) {
        List<Set> allSets = mWorkout.getAllSets();
        int pos = allSets.indexOf(set);

        // return next set if it exists
        if (pos != -1 && pos < allSets.size() -1)
            return allSets.get(pos +1);
        else
            return null;
    }

    private void goToNextSet(Set previous) {
        Set setAfter = getNextSet(previous);
        if (setAfter != null)
            goToSet(setAfter);
    }

    private void updateToolbarTitle() {
        if (mToolbar != null)
            mToolbar.setTitle(
                    mWorkout.isRoutine() ? R.string.title_edit_routine
                            : edit ? R.string.title_edit_workout : R.string.title_active_workout);
    }

    private Workout initializeWorkout() {
        // get workout id and initialize workout
        Bundle args = getIntent().getExtras();
        int[] ids = args.getIntArray(ARG_IDS);
        edit = args.getBoolean(ARG_EDIT, false);
        if (ids == null || ids.length == 0)
            return null;

        Log.i(TAG, "Initialize workout...");

        if (edit) {
            return db.getWorkout(ids[0]);
        } else {
            Workout wo;
            if (ids.length == 1) {
                if (db.getWorkout(ids[0], false).isRoutine()) {
                    wo = db.getWorkoutFromRoutine(ids[0]);
                    // TODO ...
                } else if (ids[0] == -1) {
                    wo = new Workout();
                } else {
                    wo = null;
                }
            } else {
                List<Workout> workoutList = new ArrayList<>(ids.length);
                for (int i : ids)
                    workoutList.add(db.getWorkout(i));
                wo = mergeWorkouts(workoutList);
                changed = true;
            }
            if (wo != null) {
                wo.setDate(new GregorianCalendar());

                List<Set> allSets = wo.getAllSets();
                for (Set s : allSets)
                    s.setIsDone(false);
            }
            return wo;
        }
    }

    public static Workout mergeWorkouts(List<Workout> workouts) {
        Workout merged = new Workout();
        List<String> names = new ArrayList<>(workouts.size());
        List<String> notes = new ArrayList<>(workouts.size());

        for (Workout w : workouts) {
            if (!w.getName().equals("") && !names.contains(w.getName()))
                names.add(w.getName());
            if (!w.getNote().equals("") && !notes.contains(w.getNote()))
                notes.add(w.getNote());

            for (Superset s : w.getSupersets())
                merged.addSuperset(s);
        }
        if (!names.isEmpty())
            merged.setName(TextUtils.join(", ", names));
        if (!notes.isEmpty())
            merged.setNote(TextUtils.join(", ", notes));

        return merged;
    }

    @Override
    public void onResume() {
        super.onResume();
        stopNotify();
        updatePreferences();
    }

    @Override
    public void onStop() {
        if (started && !ending) {
            startNotify();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        stopNotify();
        unregisterReceiver(timerReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_workout_editor, menu);

        // show "End workout" only if workout is started
        if (!started)
            menu.removeItem(menu.findItem(R.id.action_end_workout).getItemId());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_end_workout:
                close();
                return true;
            default:
                return false;
        }
    }

    public void openSetEditor(Set set) {
        // set set in set editor and animate if already visible
        setEditor.setSet(set, setEditorCard.getVisibility() == View.VISIBLE);

        // show set editor
        setEditorCard.setVisibility(View.VISIBLE);
    }

    public void openAddExerciseDialog() {
        ExerciseSelectionDialog dialog = ExerciseSelectionDialog.newInstance(true);
        dialog.show(getSupportFragmentManager(), ARG_ADD);
    }

    public void openGoalEditorDialog(List<Integer> exerciseIds) {
        /*TODO GoalEditorDialog dialog = GoalEditorDialog.newInstance(
                exerciseIds,
                mWorkout.getRoutineId(),
                this);
        dialog.show(getSupportFragmentManager(), ARG_SET);*/
    }

    public void openRestPickerDialog(Row row) {
        RestPickerDialog dialog = RestPickerDialog.newInstance(row);
        dialog.show(getSupportFragmentManager(), "restPicker");
    }

    // WorkoutView interaction
    // called by the WorkoutView when a row has been long-clicked
    @Override
    public void onRowLongClicked(Row row) {
        // TODO start selection mode
        Toast.makeText(WorkoutEditor.this, "START SELECTION MODE [Row " + row.getCoordinates() + "]", Toast.LENGTH_SHORT).show();
        selectionSize = 1;
    }

    // called by the WorkoutView when two rows have been swapped and ActionMode should be finished
    @Override
    public void onRowsSwapped() {
        if (selectionSize == 1) {
            // TODO finish selection mode
            selectionSize = 0;
            Toast.makeText(WorkoutEditor.this, "STOP SELECTION MODE", Toast.LENGTH_SHORT).show();
        }
    }
    // called by the WorkoutView when a row has been moved via drag n drop
    @Override
    public void onMoveRow(int fromSuperset, int fromRow, int toSuperset, int toRow, boolean newSuperset) {
        // change data in mWorkout
        mWorkout.moveRow(fromSuperset, fromRow, toSuperset, toRow, newSuperset);

        // find out affected range of supersets
        int firstSupersetToBeChanged = Math.min(fromSuperset, toSuperset);
        int lastSupersetToBeChanged = newSuperset ? mWorkout.getSupersetCount() - 1 : Math.max(fromSuperset, toSuperset);

        // update SUPERSET field in database for every affected row
        for (int i = firstSupersetToBeChanged; i <= lastSupersetToBeChanged; i++) {
            Superset si = mWorkout.getSuperset(i);
            for (Row r : si.getRows())
                db.updateRow(r);
        }

        // update state flags
        changed = true;

        /*Toast.makeText(WorkoutEditor.this,
                "Moved row " + fromSuperset + "." + fromRow + " to " + toSuperset + "." + toRow + "[" + newSuperset + "]", Toast.LENGTH_SHORT).show();*/
    }

    // called by the WorkoutView when an exercise should be added
    @Override
    public void onAddExerciseClicked() {
        openAddExerciseDialog();
        //Toast.makeText(WorkoutEditor.this, "ADD EXERCISE", Toast.LENGTH_SHORT).show();
    }

    // called by the RowView when a set has been clicked
    @Override
    public void onSetClicked(Set set, boolean longClick) {
        if (!longClick) {
            goToSet(set);
        }
        //else TODO mark set for deletion
//        Toast.makeText(WorkoutEditor.this, "Set " + set.getCoordinates() + " clicked", Toast.LENGTH_SHORT).show();
    }

    // called by the RowView when the "add set" button has been clicked
    @Override
    public void onAddSetClicked(Row row) {
        // clone last set in row
        Set newSet = new Set(row.getSet(row.getSetCount() - 1));
        newSet.setIsDone(false);

        // add set
        row.addSet(newSet);
        db.addSet(newSet);

        // notify WorkoutView
        workoutView.addSet(newSet);

        // update state flags
        changed = true;
        ending = false;

        //Toast.makeText(WorkoutEditor.this, "ADD SET TO ROW " + row.getCoordinates(), Toast.LENGTH_SHORT).show();
    }

    // called by the RowView when the note has been edited
    @Override
    public void onNoteChanged(Row row, String note) {
        row.setNote(note);
        db.updateRow(row);

        // update state flags
        changed = true;
//        Toast.makeText(WorkoutEditor.this, "Note changed in row " + row.getCoordinates() + " to " + note, Toast.LENGTH_SHORT).show();
    }

    // called by the RowView when the rest has been clicked
    @Override
    public void onRestClicked(Row row) {
        openRestPickerDialog(row);
        //Toast.makeText(WorkoutEditor.this, "Open rest spinner for row " + row.getCoordinates(), Toast.LENGTH_SHORT).show();
    }

    // called by the RowView when the goals should be edited
    @Override
    public void onEditGoals(Row row) {
        // TODO show goal Editor
        Toast.makeText(WorkoutEditor.this, "EDIT GOALS FOR ROW " + row.getCoordinates(), Toast.LENGTH_SHORT).show();
    }

    // called by the RowView when the row should be deleted
    @Override
    public void onDeleteRow(Row row) {
        // remove view from Workout View
        workoutView.removeRow(row);

        Superset s = row.getSuperset();
        // delete superset if single-row
        if (s.getRowCount() == 1) {
            int ssPos = s.getPosition();
            mWorkout.removeSuperset(ssPos);
            // delete row from database and update consequent rows
            db.dropRow(row.getDatabaseId());
            for (int i = ssPos; i < mWorkout.getSupersetCount(); i++)
                for (Row r : mWorkout.getSuperset(i).getRows())
                    db.updateRow(r);
        }
        else {
            // delete row
            int rowPos = row.getPosition();
            s.removeRow(rowPos);
            db.dropRow(row.getDatabaseId());

            // update consequent rows in same superset
            for (int i = rowPos; i < s.getRowCount(); i++)
                db.updateRow(s.getRow(i));
        }

        //Toast.makeText(WorkoutEditor.this, "DELETE ROW " + row.getCoordinates(), Toast.LENGTH_SHORT).show();
    }

    // called by the RowView when the exercise should be edited
    @Override
    public void onEditExercise(Exercise exercise) {
        // TODO open exercise editor
        Toast.makeText(WorkoutEditor.this, "EDIT EXERCISE " + exercise.getName(), Toast.LENGTH_SHORT).show();
    }

    // SetEditor interaction
    // called when a set has been changed in workout editor
    @Override
    public void onSetChanged(Set set) {
        workoutView.updateSet(set);
        db.updateSet(set);

        // update state flags
        changed = true;

        //Toast.makeText(WorkoutEditor.this, "SET CHANGED: " + set.getCoordinates(), Toast.LENGTH_SHORT).show();
    }

    // ExerciseSelectionDialog interaction
    @Override
    public void onExercisesSelected(String tag, List<Integer> exerciseIds) {
        switch (tag) {
            case ARG_ADD:
                // add selected exercises to workout

                // memorize new rows for Goal Editor result
                List<Row> newRows = new ArrayList<>(exerciseIds.size());

                Map<Integer, Exercise> exercises = db.getExercises();
                for (int i : exerciseIds) {
                    Superset newSuperset = new Superset();
                    mWorkout.addSuperset(newSuperset);
                    Row newRow = new Row(exercises.get(i));
                    Set newSet = new Set();
                    newRow.addSet(newSet);
                    newSuperset.addRow(newRow);
                    newRows.add(newRow);

                    workoutView.addSuperset(newSuperset);
                    db.addRow(newRow, true);

                    Log.i("WE2", newRow.getSuperset().getWorkout().getDatabaseId() + "." + newRow.getCoordinates() + " ADDED");
                }
                // update state flags
                changed = true;
                ending = false;
                // TODO openGoalEditorDialog(newRows);
                openGoalEditorDialog(exerciseIds);
        }
    }

    // RestPickerDialog interaction
    @Override
    public void onRestPicked(String rowCoordinates, int rest) {
        Log.i("RestPicker", "(onRestPicked) rowCoordinates = " + rowCoordinates);
        Row row = mWorkout.getRowFromCoordinates(rowCoordinates);

        if (row == null) return;

        row.setRest(rest);
        db.updateRow(row);

        timer.update();
        workoutView.findRowView(row).update();
    }

    // GoalEditorDialog interaction
    @Override
    public void onGoalsChanged(String tag, List<Goal> goals) {
        /*List<Integer> supersets = getListAdapter().getSelectedItems();

        int g = 0;
        for (int superset : supersets) {
            for (Row row : mWorkout.getSuperset(superset).getRows()) {
                if (getListAdapter().isSubSelection() && row.getPosition() != getListAdapter().getSelectedSubItem())
                    continue;

                Goal goal = goals.get(g);

                // delete sets if too many
                for (int i = row.getSetCount() - 1; i > 0 && row.getSetCount() > goal.sets; i--) {
                    Set set = row.getSet(i);
                    if (!set.isDone())
                        deleteSet(set);
                }

                // add sets if too few
                for (int i = row.getSetCount(); i < goal.sets; i++) {
                    addSet(superset, row.getPosition());
                }

                // replace goal info
                for (Set s : row.getSets()) {
                    if (s.isDone()) continue;
                    s.setReps(goal.reps);
                    s.setWeight(goal.weight);
                    s.setRest(goal.rest);
                    getListAdapter().updateSet(s);
                    db.updateSet(s);
                }
                g++;
            }
        }
        // set state flags
        changed = true;
        ending = false;*/
    }

    @Override
    public void onGoalEditorNegative(String tag, List<Integer> exerciseIds) {
    }
}
