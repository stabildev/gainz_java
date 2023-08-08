// Vorher: 1416 Zeilen

package com.mycompany.gains.Activities.WorkoutEditorOld;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mycompany.gains.Activities.BaseActivity;
import com.mycompany.gains.Activities.MainActivity.MainActivity;
import com.mycompany.gains.Activities.WorkoutEditor.Stopwatch;
import com.mycompany.gains.Activities.WorkoutEditor.GoalEditor.GoalEditorDialog;
import com.mycompany.gains.Data.Model.Goal;
import com.mycompany.gains.Globals;
import com.mycompany.gains.Stuff.Misc;
import com.mycompany.gains.Activities.Preferences.SettingsActivity;
import com.mycompany.gains.Activities.WorkoutEditorOld.adapters.RowListAdapter;
import com.mycompany.gains.Activities.WorkoutEditorOld.fragments.RowListFragment;
import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.Data.Model.Superset;
import com.mycompany.gains.Data.WorkoutDataFragment;
import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.Data.Model.Exercise;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Data.Model.Workout;
import com.mycompany.gains.R;
import com.mycompany.gains.Activities.WorkoutEditorOld.adapters.SetEditorPagerAdapter;
import com.mycompany.gains.Activities.WorkoutEditor.ExerciseSelection.ExerciseSelectionDialog;
import com.mycompany.gains.Activities.WorkoutEditorOld.fragments.SetEditorFragment;
import com.mycompany.gains.Activities.WorkoutEditor.CountdownNotifyService;
import com.mycompany.gains.widgets.TimerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static com.mycompany.gains.Arguments.*;

public class WorkoutEditorOld extends BaseActivity
        implements SetEditorFragment.OnSetEditorFragmentInteractionListener,
        ExerciseSelectionDialog.OnDialogInteractionListener, GoalEditorDialog.OnDialogInteractionListener,
        WorkoutSync.SyncListener {

    private DatabaseHelper db;

    // ACTION MODE
    private ActionMode mActionMode;

    private enum ActionModeState {
        SUPERSET_SELECTION,
        NULL }

    private ActionModeState actionModeState = ActionModeState.NULL;

    // SET EDITOR
    private ViewPager mViewPager;
    private SetEditorPagerAdapter mPagerAdapter;

    private ProgressBar mProgress;

    private Workout mWorkout;

    // TIMER & ALARM
    private TimerView timer;
    private boolean autoSkip;
    private boolean vibrate;
    private Uri alarm;
    private Uri beep;
    private int beepAfter;

    // EXERCISE SELECTION
    private int selectedSuperset = -1;
    private int selectedRow = -1;

    // GLOBAL FLAGS
    private boolean changed = false;
    private boolean started = false;
    private boolean ending = false;

    private boolean notifying = false;

    private boolean isSetEditorUpdating = false;

    private boolean edit;

    private final static String TAG = "WorkoutEditorOld";

    private final Handler handler = new Handler();

    private ActionBar mToolbar;

    // CONSTANTS
    private static final String FRAGMENT_TAG_DATA_PROVIDER = "data provider";
    private static final String FRAGMENT_LIST_VIEW = "list view";

    private enum WorkoutState {PREPARE, STARTED, EDIT, PAUSED}
    private WorkoutState workoutState;

    // used for timer:
    //private Set mCurrentSet;

    // used to synchronize workout across adapters, activities etc
    WorkoutSync sync;

    // broadcast receiver for timer ticks
    BroadcastReceiver br;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_editor_old);

        Bundle bundle = getIntent().getExtras();
        edit = bundle.getBoolean(ARG_EDIT, false);

        // get database helper
        db = DatabaseHelper.getInstance(this);

        sync = WorkoutSync.getInstance(this);
        sync.addSyncListener(this);

        timer = (TimerView) findViewById(R.id.timer);
        initializeTimer();

        initializeWorkout(bundle);

        // set up row list fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(WorkoutDataFragment.newInstance(mWorkout), FRAGMENT_TAG_DATA_PROVIDER)
                    .commit();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new RowListFragment(), FRAGMENT_LIST_VIEW)
                    .commit();
        }

        // initialize toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mToolbar = getSupportActionBar();

        if (mToolbar != null) {
            //mToolbar.setHomeButtonEnabled(true);
            mToolbar.setDisplayHomeAsUpEnabled(true);
            mToolbar.setDisplayShowHomeEnabled(true);
        }

        // set up progress bar
        mProgress = (ProgressBar) findViewById(R.id.workout_progress);

        workoutState = edit ? WorkoutState.EDIT : WorkoutState.PREPARE;

        // set up edit area
        setupEditArea();

        // open add exercise dialog if workout is empty
        if (mWorkout.getSupersetCount() == 0) {
            ExerciseSelectionDialog dialog = ExerciseSelectionDialog.newInstance(true);
            dialog.show(getSupportFragmentManager(), "addExercise");
        }

        updateWorkoutState();
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
        unregisterReceiver(br);
        super.onDestroy();
    }

    private void initializeTimer() {
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getStringExtra(ARG_ACTION)) {

                    case ACTION_FINISH:
                        if (timer != null) timer.setTime(0, !timer.isDragging());

                        // play alarm sound
                        if (!notifying) {
                            if (vibrate)
                                ((Vibrator) WorkoutEditorOld.this.getSystemService(VIBRATOR_SERVICE)).vibrate(500);
                            if (alarm != null)
                                RingtoneManager.getRingtone(WorkoutEditorOld.this, alarm).play();
                        }

                        // notify countdown widget
                        if (timer != null) timer.setCounting(false);

                        // go to next set
                        goToNextSetAfterCountdown();
                        return;

                    case ACTION_TICK:
                        int remaining = intent.getIntExtra(ARG_TIME, 0);
                        if (timer != null && !timer.isDragging()) timer.setTime(remaining, !timer.isDragging());

                        if (!notifying) {
                            if (remaining <= beepAfter) {
                                if (vibrate)
                                    ((Vibrator) WorkoutEditorOld.this.getSystemService(VIBRATOR_SERVICE)).vibrate(100);
                                if (beep != null)
                                    RingtoneManager.getRingtone(WorkoutEditorOld.this, beep).play();
                            }
                        }
                }
            }
        };
        registerReceiver(br, new IntentFilter(CountdownNotifyService.COUNTDOWN_BR));

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
                // update currentSet
                sync.updateSetBeforeCountdown();
                // mark set as done
                markCurrentSetAsDone();
                // set new rest
                sync.getSetBeforeCountdown().setRest(initial);

                // start countdown service
                Intent intent = new Intent(WorkoutEditorOld.this, CountdownNotifyService.class);
                intent.putExtra(ARG_ACTION, ACTION_START);
                intent.putExtra(ARG_INITIAL, initial);
                WorkoutEditorOld.this.startService(intent);
            }

            @Override
            public void onSkipCountdown(boolean isTimerEnabled) {
                if (isTimerEnabled) {
                    // notify countdown service
                    Intent intent = new Intent(WorkoutEditorOld.this, CountdownNotifyService.class);
                    intent.putExtra(ARG_ACTION, ACTION_CANCEL);
                    WorkoutEditorOld.this.startService(intent);

                    timer.setCounting(false);

                    // go to next set
                    goToNextSetAfterCountdown();
                } else {
                    // mark set as done
                    markCurrentSetAsDone();
                    // update currentSet
                    sync.updateSetBeforeCountdown();
                    // go to next set
                    goToNextSetAfterCountdown();
                }
            }

            @Override
            public void onCancelCountdown() {
                // notify countdown service
                Intent intent = new Intent(WorkoutEditorOld.this, CountdownNotifyService.class);
                intent.putExtra(ARG_ACTION, ACTION_CANCEL);
                WorkoutEditorOld.this.startService(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        stopNotify();

        // update settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useTimer = prefs.getBoolean(getString(R.string.pref_key_enable_timer), false);
        int restIncrement = Integer.parseInt(prefs.getString(getString(R.string.pref_key_rest_increment), "15"));
        int timerMax = Integer.parseInt(prefs.getString(getString(R.string.pref_key_timer_max), "" + 5)) * 60;
        autoSkip = prefs.getBoolean(getString(R.string.pref_key_auto_skip), true);

        alarm = Uri.parse(prefs.getString(getString(R.string.pref_key_alarm_sound), ""));
        alarm = alarm.toString().equals("") ? null : alarm; // allow for silent
        beep = Uri.parse(prefs.getString(getString(R.string.pref_key_acustic_countdown), ""));
        beep = beep.toString().equals("") ? null : beep; // allow for silent

        vibrate = prefs.getBoolean(getString(R.string.pref_key_vibrate_countdown), true);
        beepAfter = Integer.parseInt(prefs.getString(getString(R.string.pref_key_beep), "3"));

        timer.useTimer(useTimer);
        timer.setIncrement(restIncrement);
        timer.setMax(timerMax);
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();

            if (actionModeState == ActionModeState.SUPERSET_SELECTION) {
                inflater.inflate(R.menu.action_superset, menu);
                return true;

            } else
                return false;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            final List<Integer> selectedSupersets = getListAdapter().getSelectedItems();
            switch (item.getItemId()) {
                case R.id.action_edit: {
                    PopupMenu popup = new PopupMenu(WorkoutEditorOld.this, findViewById(R.id.action_edit)) {
                        @Override
                        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                            if (item.getItemId() == R.id.action_replace_exercise) {
                                ExerciseSelectionDialog dialog = ExerciseSelectionDialog.newInstance(false);
                                dialog.show(getSupportFragmentManager(), "replaceExercise");
                                // mode is finished elsewhere to keep superset and row selection
                                return true;
                            } else if (item.getItemId() == R.id.action_edit_goals) {
                                List<Integer> exerciseIds = new ArrayList<>();

                                for (int superset : selectedSupersets) {
                                    for (Row row : mWorkout.getSuperset(superset).getRows()) {
                                        if (getListAdapter().isSubSelection() && row.getPosition() != getListAdapter().getSelectedSubItem())
                                            continue;
                                        exerciseIds.add(row.getExercise().get_id());
                                    }
                                }

                                GoalEditorDialog dialog = GoalEditorDialog.newInstance(
                                        exerciseIds,
                                        mWorkout.getRoutineId(),
                                        WorkoutEditorOld.this);
                                dialog.show(getSupportFragmentManager(), "editGoals");
                                // mode is finished elsewhere to keep superset and row selection
                                return true;
                            }
                            mode.finish();
                            return super.onMenuItemSelected(menu, item);
                        }
                    };
                    popup.inflate(R.menu.action_superset_edit);
                    if (selectedSupersets.size() > 1) popup.getMenu().findItem(R.id.action_replace_exercise)
                            .setEnabled(false);
                    popup.show();
                    return true;
                }
                case R.id.action_group: {
                    List<Row> changedRows = new ArrayList<>();
                    if (selectedSupersets.size() < 2) return true;

                    for (int i : selectedSupersets)
                        changedRows.addAll(mWorkout.getSuperset(i).getRows());

                    mWorkout.mergeSupersets(selectedSupersets);

                    mode.finish();
                    sync.notifySetOrderChanged();
                    getListAdapter().notifyDataSetChanged();
                    updateSetEditor(true);

                    for (Row r : changedRows) {
                        db.updateRow(r);
                    }

                    changed = true;
                    return true;
                }
                case R.id.action_separate_from_group: {
                    mWorkout.separateRowFromSuperset(selectedSuperset, selectedRow);
                    //mPagerAdapter.notifyDataSetChanged();

                    for (Row r : mWorkout.getSuperset(selectedSuperset).getRows())
                        db.updateRow(r);
                    for (Row r : mWorkout.getSuperset(selectedSuperset + 1).getRows())
                        db.updateRow(r);

                    changed = true;
                    mode.finish();
                    sync.notifySetOrderChanged();
                    getListAdapter().notifyDataSetChanged();
                    updateSetEditor(true);
                    return true;
                }
                case R.id.action_ungroup: {
                    List<Row> rowList = new ArrayList<>();

                    for (int i : selectedSupersets) {
                        rowList.addAll(mWorkout.getSuperset(i).getRows());
                        mWorkout.splitSuperset(i);
                    }

                    mode.finish();
                    sync.notifySetOrderChanged();
                    getListAdapter().notifyDataSetChanged();
                    updateSetEditor(true);

                    for (Row r : rowList)
                        db.updateRow(r);

                    changed = true;
                    return true;
                }
                case R.id.action_delete: {
                    // if row is selected, delete row
                    if (selectedRow != -1) {
                        deleteRow(selectedSuperset, selectedRow);
                    }
                    // else, delete selected supersets
                    else {
                        // sort superset positions from high to low so the position is never out of bounds
                        Collections.sort(selectedSupersets, Collections.reverseOrder());
                        for (int i : selectedSupersets) {
                            deleteSuperset(i);
                        }
                    }
                    mode.finish();
                    updateSetEditor(true);
                    return true;
                }
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (actionModeState == ActionModeState.SUPERSET_SELECTION)
                getListAdapter().clearSelection();
            mActionMode = null;
            actionModeState = ActionModeState.NULL;
        }
    };

    public void onSupersetSelectionChanged(int selectionSize, int subSelectionSize) {
        if (selectionSize > 0) {
            selectedSuperset = getListAdapter().getSelectedItems().get(0);
            selectedRow = getListAdapter().getSelectedSubItem();

            if (actionModeState != ActionModeState.SUPERSET_SELECTION) {
                if (mActionMode != null)
                    mActionMode.finish();
                actionModeState = ActionModeState.SUPERSET_SELECTION;
                mActionMode = this.startSupportActionMode(mActionModeCallback);
            }

            mActionMode.setTitle(subSelectionSize == 1 ? "1 " + getString(R.string.sub_exercise_selected)
                    : selectionSize + " " + (selectionSize > 1 ? getString(R.string.exercises_selected)
                    : getString(R.string.exercise_selected)));

            final Menu menu = mActionMode.getMenu();

            // adjust menu item states
            List<Pair<MenuItem, Drawable>> hide = new ArrayList<>();
            List<Pair<MenuItem, Drawable>> enable = new ArrayList<>();
            List<Pair<MenuItem, Drawable>> disable = new ArrayList<>();

            // prepare items

            Pair<MenuItem, Drawable> action_group = new Pair<>(
                    menu.findItem(R.id.action_group),
                    ContextCompat.getDrawable(WorkoutEditorOld.this, R.drawable.ic_call_merge_white_24dp));

            Pair<MenuItem, Drawable> action_ungroup = new Pair<>(
                    menu.findItem(R.id.action_ungroup),
                    ContextCompat.getDrawable(WorkoutEditorOld.this, R.drawable.ic_call_split_white_24dp));

            /*Pair<MenuItem, Drawable> action_edit = new Pair<>(
                    menu.findItem(R.id.action_edit),
                    ContextCompat.getDrawable(WorkoutEditorOld.this, R.drawable.ic_edit_white_24dp));*/

            Pair<MenuItem, Drawable> action_separate_from_group = new Pair<>(
                    menu.findItem(R.id.action_separate_from_group),
                    ContextCompat.getDrawable(WorkoutEditorOld.this, R.drawable.ic_call_split_white_24dp));

            if (subSelectionSize == 1) {
                hide.add(action_group);
                hide.add(action_ungroup);
//                enable.add(action_edit);
                enable.add(action_separate_from_group);
            } else {
                hide.add(action_separate_from_group);

                // disable "merge supersets" if only one item is selected
                // disable "split superset" if more than one item is selected OR item has only one row
                // disable "replace exercise" if more than one item is selected OR item has more than one row
                if (selectionSize == 1) {
                    int rowCount = mWorkout.getSuperset(selectedSuperset).getRowCount();
                    disable.add(action_group);
//                    enable.add(action_edit);
                    if (rowCount > 1) {
                        enable.add(action_ungroup);
//                        disable.add(action_edit);
                        hide.add(action_group);
                    } else if (rowCount == 1) {
                        hide.add(action_ungroup);
                        enable.add(action_group);
                        disable.add(action_group);
                    }
                } else {
                    enable.add(action_group);
                    hide.add(action_ungroup);
//                    disable.add(action_edit);
                }
            }
            menu.findItem(R.id.action_separate_from_group).setVisible(false);

            // apply new states
            for (Pair<MenuItem, Drawable> item : enable) {
                item.first.setVisible(true);
                item.first.setEnabled(true);
                item.first.setIcon(item.second);
            }
            for (Pair<MenuItem, Drawable> item : disable) {
                item.first.setVisible(true);
                item.first.setEnabled(false);
                item.second.mutate().setAlpha(50);
                item.first.setIcon(item.second);
            }
            for (Pair<MenuItem, Drawable> item : hide) {
                item.first.setVisible(false);
            }

        } else {
            selectedSuperset = -1;
            actionModeState = ActionModeState.NULL;
            if (mActionMode != null) mActionMode.finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (mActionMode != null)
            mActionMode.finish();
        else {
            if (!changed && !started && !edit) {
                db.dropWorkout(mWorkout.getDatabaseId());
                Toast.makeText(this, getString(R.string.toast_discarded_workout), Toast.LENGTH_SHORT).show();
            }
            if (!changed && !started) {
                Globals.getInstance().setActiveWorkoutId(-1);
                backToMain();
                this.finish();
            }
            else {
                backToMain();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_workout_editor_old, menu);
        if (mWorkout != null)
            menu.findItem(R.id.action_mark_as_routine).setChecked(mWorkout.isRoutine());

        Drawable icPause = ContextCompat.getDrawable(this, R.drawable.ic_pause_white_24dp).mutate();

        if (started) menu.findItem(R.id.action_mark_as_routine).setEnabled(false);

        if (started && !(workoutState == WorkoutState.PAUSED)) {
            menu.findItem(R.id.action_pause_workout).setEnabled(true).setIcon(icPause);
        }
        else {
            icPause.setAlpha(50);
            menu.findItem(R.id.action_pause_workout).setEnabled(false).setIcon(icPause);
        }
        if (mWorkout.isRoutine())
            menu.findItem(R.id.action_pause_workout).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                Log.i("LEL", "home");
                this.onBackPressed();
                return true;
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                this.startActivity(intent);
            }
                return true;
            case R.id.action_pause_workout:
                setWorkoutState(WorkoutState.PAUSED);
                invalidateOptionsMenu();
                return true;
            case R.id.action_add_exercise:
                ExerciseSelectionDialog dialog = ExerciseSelectionDialog.newInstance(true);
                dialog.show(getSupportFragmentManager(), "addExercise");
                return true;
            case R.id.action_end_workout:
                endWorkout();
                return true;
            case R.id.action_mark_as_routine:
                item.setChecked(!item.isChecked());
                if (!started)
                mWorkout.setIsRoutine(item.isChecked());
                db.updateWorkout(mWorkout);
                setWorkoutState(WorkoutState.EDIT);
                return true;
            default:
                return false;
        }
    }

    public void addSet(int supersetPos, int rowPos) {
        Row row = mWorkout.getSuperset(supersetPos).getRow(rowPos);

        Set newSet = new Set(row.getSet(row.getSetCount()-1)); // copy last set in row
        newSet.setIsDone(false);

        row.addSet(newSet);
        db.addSet(newSet);
        changed = true;
        ending = false;
        updateSetEditor(false);
        //updateTimer();
        Log.i(TAG, "row = " + newSet.getRow().getPosition() + ", superset = "
                + newSet.getRow().getSuperset().getPosition());
        getListAdapter().addSet(newSet);
        sync.notifySetOrderChanged();
    }

    public void showSetPopup(final Set set) {
        if (set.getRow().getSetCount() <2)
            return;

        RowListAdapter.ItemViewHolder holder  = (RowListAdapter.ItemViewHolder) getListAdapter().getRecyclerView()
                .findViewHolderForAdapterPosition(set.getRow().getSuperset().getPosition() + getListAdapter().getRowOffset());
        View setView = holder.findSetView(set);
        PopupMenu overflow = new PopupMenu(this, setView, Gravity.TOP) {
            @Override
            public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                if (item.getItemId() == R.id.action_delete_set) {
                    deleteSet(set);
                    return true;
                } else
                    return super.onMenuItemSelected(menu, item);
            }
        };
        overflow.inflate(R.menu.popup_set);
        overflow.show();
    }

    public void onLeftButtonClick(View v) {
        int pos = mViewPager.getCurrentItem();
        mViewPager.setCurrentItem(pos - 1);
    }

    public void onRightButtonClick(View v) {
        int pos = mViewPager.getCurrentItem();
        mViewPager.setCurrentItem(pos + 1);
    }

    // called when set is changed in set editor
    @Override
    public void onSetChanged(Set set) {
        db.updateSet(set);
        getListAdapter().updateSet(set);
        updateWorkoutProgress();
        changed = true;
        if (set.equals(sync.getCurrentSet()))
            sync.notifyCurrentSetModified();
    }

    // called when row data is changed in set editor, i.e. note
    @Override
    public void onRowChanged(Row row) {
        db.updateRow(row);
        changed = true;

        for (SetEditorFragment f : mPagerAdapter.getRegisteredFragments())
            if (f.getSet().getRow().getDatabaseId() == row.getDatabaseId()
                    && !f.equals(mPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem()))) {
                f.updateNote();
            }
    }

    private void startNotify() {
        int pos = mViewPager.getCurrentItem();
        Set nextSet = mPagerAdapter.getSet(pos + ((timer.isCounting() && hasNextSet()) ? 1 : 0));

        Intent intent = new Intent(this, CountdownNotifyService.class);
        intent.putExtra(ARG_ACTION, ACTION_START_NOTIFY);
        intent.putExtra(ARG_PROGRESS, mProgress.getProgress());
        intent.putExtra(ARG_NOTIFY_TEXT,
                (timer.isCounting() ? "Up next: " : "")
                        + nextSet.getRow().getExercise().getName()
                        + ", " + getString(R.string.set) + " "
                        + (nextSet.getPosition() + 1) + "/" + nextSet.getRow().getSetCount());
        startService(intent);
        notifying = true;
    }

    private void stopNotify() {
        Intent intent = new Intent(this, CountdownNotifyService.class);
        intent.putExtra(ARG_ACTION, ACTION_STOP_NOTIFY);
        startService(intent);
        notifying = false;
    }

    @Override
    public void onLayoutSizeChanged() {
        updateViewPagerHeight();
    }

    public boolean hasNextSet() {
        return mViewPager != null && mPagerAdapter != null &&
                mPagerAdapter.getCount() > mViewPager.getCurrentItem() + 1;
    }

    public void setWorkoutState(WorkoutState state) {
        workoutState = state;
        updateWorkoutState();
    }

    public void updateWorkoutState() {
        switch(workoutState) {
            case PREPARE:
                timer.setStarted(false);
                mToolbar.setTitle(R.string.title_prepare_workout);
                findViewById(R.id.navigation).setVisibility(View.GONE);
                mViewPager.setVisibility(View.GONE);
                if (mPagerAdapter.getCount() > 0) {
                    timer.setEnabled(true);
                }
                else {
                    timer.setEnabled(false);
                }
                break;
            case STARTED:
                started = true;
                timer.setStarted(true);
                timer.setEnabled(true);
                mToolbar.setTitle(R.string.title_active_workout);
                findViewById(R.id.navigation).setVisibility(View.VISIBLE);
                mViewPager.setVisibility(View.VISIBLE);
                invalidateOptionsMenu();
                Stopwatch stopwatch = sync.getStopwatch();

                if (stopwatch == null) {
                    stopwatch = new Stopwatch();
                    sync.setStopwatch(stopwatch);
                    stopwatch.addStopwatchListener(new Stopwatch.StopwatchListener() {
                        int wait;
                        long duration;

                        @Override
                        public void onTick(long duration) {
                            if (wait++ >= 150) { // every 15 seconds
                                this.duration = duration;
                                update();
                            }
                        }

                        @Override
                        public void onStop() {
                            update();
                        }

                        private void update() {
                            Log.i("Stopwatch", "UPDATE");
                            wait = 0;
                            mWorkout.setDuration(duration);
                            updateMetaData();
                        }
                    });
                    // add stopwatch to header viewholder
                    if (getListAdapter() != null && getListAdapter().getRecyclerView() != null) {
                        RowListAdapter.HeaderViewHolder holder = (RowListAdapter.HeaderViewHolder)
                                getListAdapter().getRecyclerView().findViewHolderForAdapterPosition(0);

                        if (holder != null) {
                            holder.setStopwatch(stopwatch);
                            holder.showTime(true);
                        }
                    }
                }
                stopwatch.init(mWorkout.getDuration());
                stopwatch.start();
                break;
            case EDIT:
                findViewById(R.id.navigation).setVisibility(View.GONE);
                mViewPager.setVisibility(View.GONE);
                if (mWorkout.isRoutine()) {
                    timer.setVisibility(View.GONE);
                    mToolbar.setTitle(R.string.title_edit_routine);
                }
                else {
                    timer.setVisibility(View.VISIBLE);
                    mToolbar.setTitle(R.string.title_edit_workout);
                }
                break;
            case PAUSED:
                timer.setStarted(false);
                if (sync.getStopwatch() != null)
                    sync.getStopwatch().stop();
                break;
        }
        invalidateOptionsMenu();
    }

    public void updateSetEditorNavigation(boolean updateList, final int pos) {
        final Set currentSet = mPagerAdapter.getSet(pos);

        // update left arrow
        if (pos > 0) {
            View arrow = findViewById(R.id.arrow_left);
            Misc.animateAlpha(arrow, 1f);
            arrow.setFocusable(true);
            arrow.setClickable(true);
        } else if (pos == 0) {
            View arrow = findViewById(R.id.arrow_left);
            Misc.animateAlpha(arrow, .2f);
            arrow.setFocusable(false);
            arrow.setClickable(false);
        }

        // update right arrow
        if (pos < (mPagerAdapter.getCount() -1)) {
            View arrow = findViewById(R.id.arrow_right);
            Misc.animateAlpha(arrow, 1f);
            arrow.setFocusable(true);
            arrow.setClickable(true);
        } else if (pos == (mPagerAdapter.getCount() -1)) {
            View arrow = findViewById(R.id.arrow_right);
            Misc.animateAlpha(arrow, .2f);
            arrow.setFocusable(false);
            arrow.setClickable(false);
        }

        if (getListAdapter() != null) {
            updateWorkoutProgress();
            if (updateList) getListAdapter().setActiveSet(currentSet);
        }
    }

    public void updateSetEditor(boolean updateList) {
        // get currently displayed set and then notifyDataSetChanged
        final int previousPos = mViewPager.getCurrentItem();
        final Set previousSet = mPagerAdapter.getSet(previousPos);

        isSetEditorUpdating = true;
        mPagerAdapter.notifyDataSetChanged();

        // if ViewPager is now empty, make its navigation bar invisible and return
        if (mPagerAdapter.getCount() < 1) {
            findViewById(R.id.navigation).setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            timer.setTime(0, !timer.isDragging());
            timer.setEnabled(false);
            return;
        } else if (workoutState == WorkoutState.STARTED) {
            findViewById(R.id.navigation).setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.VISIBLE);
            updateViewPagerHeight();
            timer.setEnabled(true);
        }

        // set currentSet to previousSet if exists or first set if not
        int pos = previousSet != null ? mPagerAdapter.getData().indexOf(previousSet) : 0;

        // set was deleted?
        if (pos == -1)
            pos = Math.min(previousPos, mPagerAdapter.getCount()-1);

        mViewPager.setCurrentItem(pos, false);

        updateWorkoutState();

        isSetEditorUpdating = false;

        updateSetEditorNavigation(updateList, pos);
    }

    public void updateViewPagerHeight() {
        handler.removeCallbacks(viewPagerUpdater);
        int position = mViewPager.getCurrentItem();

        // adjust ViewPager height
        ViewGroup.LayoutParams params = mViewPager.getLayoutParams();
        //View child = mViewPager.findViewWithTag(SetEditorFragment.TAG + "_" + position);
        SetEditorFragment fragment = mPagerAdapter.getRegisteredFragment(position);
        View child = fragment != null ? fragment.getView() : null;

        if (child != null) {
            child.measure(0, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            params.height = child.getMeasuredHeight();
            mViewPager.setLayoutParams(params);
        } else {
            Log.i(TAG, "handler.postDelayed(viewPagerUpdater, 100)");
            handler.postDelayed(viewPagerUpdater, 100);
        }
    }

    public void updateWorkoutProgress() {
        int progress = 0;
        if (mPagerAdapter.getCount() > 0) {
            Set currentSet = mPagerAdapter.getSet(mViewPager.getCurrentItem());
            if (currentSet != null)
                progress = mViewPager.getCurrentItem() + (currentSet.isDone() ? 1 : 0);

            progress = (int) ((float) progress / mPagerAdapter.getCount() * 100);
        }
        setWorkoutProgress(progress);
    }

    private void updateTimer() {
        if (timer != null && !timer.isCounting()) {
            if (mPagerAdapter.getCount() > 0) {
                int position = mViewPager.getCurrentItem();
                Set currentSet = mPagerAdapter.getSet(position);

                // skip timer if set is last set or auto-skip is enabled and set is
                // not in last row in superset and next set is in different row in same superset
                boolean skip = false;
                if (position + 1 >= mPagerAdapter.getCount())
                    skip = true;
                else if (autoSkip) {
                    Row currentRow = currentSet.getRow();
                    Row nextRow = mPagerAdapter.getSet(mViewPager.getCurrentItem() + 1).getRow();
                    if (nextRow.getSuperset().equals(currentRow.getSuperset())) {
                        if (currentRow.getPosition() != nextRow.getPosition())
                            if (currentRow.getPosition() != currentRow.getSuperset().getRowCount() - 1)
                                skip = true;
                    }
                }
                timer.setSkip(skip);
                timer.setTime(currentSet.getRest(), !timer.isDragging());
            }
        }
    }

    public void setWorkoutProgress(int progress) {
        // will update the "progress" propriety of mProgress until it reaches progress
        ObjectAnimator animation = ObjectAnimator.ofInt(mProgress, "progress", progress);
        animation.setDuration(250); // 0.25 seconds
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    public void onWorkoutFinished() {
        ending = true;
        setWorkoutProgress(100);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_workout_complete_title))
                .setMessage(getString(R.string.dialog_workout_complete_message))
                .setPositiveButton(getString(R.string.dialog_finish), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        endWorkout();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_workout_complete_back), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ending = false;
                    }
                })
                .show();
    }

    public void endWorkout() {
        Globals.getInstance().setActiveWorkoutId(-1);

        ending = true;

        backToMain();
        this.finish();
    }

    private void backToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(intent);
    }

    private Runnable viewPagerUpdater = new Runnable() {
        int noTry = 0;

        @Override
        public void run() {
            if (noTry++ < 3)
                updateViewPagerHeight();
        }
    };

    private void initializeWorkout(Bundle args) {
        // get workout id and initialize workout
        int[] ids = args.getIntArray(ARG_IDS);
        if (ids == null || ids.length == 0) ids = new int[]{-1};

        Log.i(TAG, "Initialize workout...");
        // initialize workout

        if (ids[0] == -1) {
            if (!edit)
                mWorkout = new Workout();
            else {
                Toast.makeText(this, "Couldn't find workout!", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        } else if (ids.length == 1) {
            Log.i(TAG, "Get data from database...");

            // load workout, check if routine, load rest of workout or create new workout from routine
            mWorkout = db.getWorkout(ids[0], false);
            if (mWorkout.isRoutine() && !edit)
                mWorkout = db.getWorkoutFromRoutine(ids[0]);
            else
                mWorkout = db.getWorkout(ids[0], true);

            if (!edit)
                mWorkout.setDate(new GregorianCalendar());
        } else {
            if (edit) {
                Toast.makeText(this, "Can't edit multiple workouts at once!", Toast.LENGTH_SHORT).show();
                this.finish();
            }
            Log.i(TAG, "Get data from database...\nMerge workouts...");
            List<Workout> workoutList = new ArrayList<>(ids.length);
            for (int i : ids)
                workoutList.add(db.getWorkout(i));
            mWorkout = mergeWorkouts(workoutList);
            changed = true;
        }

        if (!edit) {
            List<Set> allSets = mWorkout.getAllSets();
            for (Set s : allSets)
                s.setIsDone(false);
            db.addWorkout(mWorkout);
            //Toast.makeText(this, "Added workout to database. workout_id = " + mWorkout.get_id(), Toast.LENGTH_SHORT).show();
        }
        invalidateOptionsMenu();

        // set the global variable for active workout
        Globals.getInstance().setActiveWorkoutId(mWorkout.getDatabaseId());
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

    private void setupEditArea() {
        // Get the ViewPager and set its PagerAdapter so that it can display items
        mViewPager = (ViewPager) findViewById(R.id.set_editor_pager);

        mPagerAdapter = new SetEditorPagerAdapter(getSupportFragmentManager(), mWorkout);
        mViewPager.setAdapter(mPagerAdapter);

        // Change, update the current set in the workout list
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // don't update while SetEditor is being updated to avoid bugs
                if (isSetEditorUpdating) return;

                sync.setCurrentSet(mPagerAdapter.getSet(position));

                updateViewPagerHeight();
                updateSetEditorNavigation(true, position);
            }
        });
        if (mPagerAdapter.getCount() > 0)
            sync.setCurrentSet(mPagerAdapter.getSet(0));
        updateSetEditor(true);
    }

    @Override
    public void onExercisesSelected(String tag, List<Integer> exercise_ids) {
        if (exercise_ids.isEmpty())
            return;

        switch (tag) {
            case "addExercise":
                showGoalEditorDialog(exercise_ids);
                return;

            case "replaceExercise": {
                Log.i(TAG, "selectedSuperset = " + selectedSuperset + ", selectedRow = " + selectedRow);
                Row row;

                if (selectedSuperset == -1) {
                    row = mPagerAdapter.getSet(mViewPager.getCurrentItem()).getRow();
                } else {
                    Superset superset = mWorkout.getSuperset(selectedSuperset);
                    row = superset.getRow(selectedRow != -1 ? selectedRow : 0);
                }

                replaceExercise(row, db.getExercises().get(exercise_ids.get(0)));

                if (mActionMode != null)
                    mActionMode.finish();
                //return;
            }
        }
    }

    public void showGoalEditorDialog(List<Integer> exerciseIds) {
        GoalEditorDialog dialog = GoalEditorDialog.newInstance(
                exerciseIds,
                mWorkout.getRoutineId(),
                this);
        dialog.show(getSupportFragmentManager(), "setGoals");
    }

    public void replaceExercise(Row row, Exercise exercise) {
        row.setExercise(exercise);
        getListAdapter().notifyDataSetChanged();
        updateSetEditor(true);

        db.updateRow(row);
        changed = true;

        // TODO show goal editor dialog
    }

    public void deleteSet(Set set) {
        boolean isCurrentSetAffected = set.equals(sync.getCurrentSet());

        int pos = mViewPager.getCurrentItem();
        if (mPagerAdapter.getSet(pos).equals(set)) {
            if (pos < mPagerAdapter.getCount()-1)
                mViewPager.setCurrentItem(++pos);
            else
                mViewPager.setCurrentItem(--pos);
        }

        // if the current set is affected, setCurrentSet(Set set) will be called instead
        if (!isCurrentSetAffected)
            sync.notifySetOrderChanged();

        getListAdapter().removeSet(set);

        set.getRow().removeSet(set.getPosition());
        db.dropSet(set.getDatabaseId());

        changed = true;
        updateSetEditor(false);
    }

    public void deleteRow(int supersetPos, int rowPos) {
        Row row = mWorkout.getSuperset(supersetPos).removeRow(rowPos);
        deleteRow(row);
    }

    public void deleteRow(Row row) {
        boolean isCurrentSetAffected = row.equals(sync.getCurrentSet().getRow());

        db.dropRow(row.getDatabaseId());
        getListAdapter().notifyDataSetChanged();
//        getListAdapter().getRecyclerView().getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getListAdapter().getRecyclerView().requestLayout();

        // if the current set is affected, setCurrentSet(Set set) will be called instead
        if (!isCurrentSetAffected)
            sync.notifySetOrderChanged();
    }

    public void deleteSuperset(int position) {
        boolean isCurrentSetAffected = sync.getCurrentSet().getRow().getSuperset().getPosition() == position;

        for (Row row : mWorkout.getSuperset(position).getRows()) {
            db.dropRow(row.getDatabaseId());
            mWorkout.getSuperset(position).removeRow(row.getPosition());
        }
//        getListAdapter().getRecyclerView().getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getListAdapter().getRecyclerView().requestLayout();

        /*TODO getListAdapter().removeItem(position + getListAdapter().getRowOffset());*/

        mWorkout.removeSuperset(position);

        // if the current set is affected, setCurrentSet(Set set) will be called instead
        if (!isCurrentSetAffected)
            sync.notifySetOrderChanged();

        getListAdapter().notifyDataSetChanged();
        updateSetEditor(true);
    }

    public void moveRow(int supersetPos, int row, boolean up) {
        final int newPos = up ? row -1 : row + 1;
        final Superset superset = mWorkout.getSuperset(supersetPos);
        superset.moveRow(row, newPos);

        // doesn't matter if current set is in different superset
        if (sync.getCurrentSet().getRow().getSuperset().getPosition() == supersetPos)
            sync.notifySetOrderChanged();

        db.updateRow(superset.getRow(row));
        db.updateRow(superset.getRow(newPos));

        changed = true;
        updateSetEditor(false);
    }

    // is called after a superset is moved AND RELEASED in list
    // data in mWorkout and list is already changed.
    public void moveSuperset(int fromPosition, int toPosition) {
        updateSetEditor(false);
        sync.notifySetOrderChanged();

        // persist movement to database
        for (int i = Math.min(fromPosition, toPosition); i <= Math.max(fromPosition, toPosition); i++) {
            for (Row row : mWorkout.getSuperset(i).getRows()) {
                db.updateRow(row);
            }
        }
    }

    public void goToRow(int position, int subPosition) {
        subPosition = subPosition == -1 ? 0 : subPosition;
        final Set currentSet = mPagerAdapter.getSet(mViewPager.getCurrentItem());

        // if the superset and row exist and contain at least one set AND the row isn't the current row already
        if (mWorkout.getSupersetCount() >= position+1) {
            final Superset superset = mWorkout.getSuperset(position);
            if (superset.getRowCount() >= subPosition+1) {
                final Row row = superset.getRow(subPosition);
                if (row.getSetCount() > 0) {
                    final Set set = row.getSet(0);
                    if (!(row.equals(currentSet.getRow()) && superset.equals(currentSet.getRow().getSuperset())))
                        goToSet(set);
                }
            }
        }
    }

    public void goToSet(int pos) {
        mViewPager.setCurrentItem(pos);
    }

    public void goToSet(Set set) {
        int pos = mPagerAdapter.getData().indexOf(set);
        if (pos >= 0) goToSet(pos);
    }

    private void goToNextSetAfterCountdown() {
        int pos = mPagerAdapter.getData().indexOf(sync.getSetBeforeCountdown());
        if (pos++ == -1) return;

        if (pos < mPagerAdapter.getCount())
            goToSet(pos);
        else {
            setWorkoutProgress(100);
            onWorkoutFinished();
        }
    }

    // persists metadata change to database
    public void updateMetaData() {
        db.updateWorkout(mWorkout);
        changed = true;
    }

    private void markCurrentSetAsDone() {
        sync.getCurrentSet().setIsDone(true);

        // notify set editor fragment to switch from hints to text
        mPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem())
                .fillInData();

        // update set in database
        db.updateSet(sync.getCurrentSet());

        // update list
        if (getListAdapter() != null)
            getListAdapter().updateSet(sync.getCurrentSet());

        sync.notifyCurrentSetModified();

        changed = true;
    }

    public void startWorkout() {
        if (workoutState != WorkoutState.EDIT && workoutState != WorkoutState.PAUSED) {
            mWorkout.setDate(new GregorianCalendar());
            getListAdapter().notifyDataSetChanged();
            db.updateWorkout(mWorkout);
        }
        setWorkoutState(WorkoutState.STARTED);
    }

    @Override
    public void onGoalsChanged(String tag, List<Goal> goals) {
        if (goals.isEmpty()) return;

        switch (tag) {
            case "editGoals": {
                List<Integer> supersets = getListAdapter().getSelectedItems();

                boolean isCurrentSetAffected = false;

                int g = 0;
                for (int superset : supersets) {
                    for (Row row : mWorkout.getSuperset(superset).getRows()) {
                        isCurrentSetAffected |= row.equals(sync.getCurrentSet().getRow());

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

                // notifyCurrentSetModified if set is in changed row
                if (isCurrentSetAffected)
                    sync.notifyCurrentSetModified();
                else
                    sync.notifySetOrderChanged();

                updateSetEditor(false);
                //updateTimer();

                if (mActionMode != null)
                    mActionMode.finish();

                break;
            }
            case "setGoals":
                for (Goal goal : goals) {
                    if (goal.sets < 1) continue;

                    Set newSet = new Set();
                    newSet.setReps(goal.reps);
                    newSet.setWeight(goal.weight);
                    newSet.setRest(goal.rest);

                    Superset newSuperset = new Superset();
                    Row newRow = new Row(goal.exercise);
                    newRow.setNote(goal.note);

                    for (int i = 0; i < goal.sets; i++) {
                        newRow.addSet(new Set(newSet));
                    }

                    newSuperset.addRow(newRow);
                    mWorkout.addSuperset(newSuperset);

                    db.addRow(newRow, true);
                    getListAdapter().notifyItemInserted(mWorkout.getSupersets().indexOf(newSuperset)
                            + getListAdapter().getRowOffset());
//                    getListAdapter().getRecyclerView().getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    getListAdapter().getRecyclerView().requestLayout();

                }
        }

        changed = true;
        ending = false;
        updateSetEditor(true);
        //updateTimer();
    }

    @Override
    public void onGoalEditorNegative(String tag, List<Integer> exerciseIds) {
        if (!tag.equals("setGoals")) return;

        Map<Integer, Exercise> exercises = db.getExercises();
        for (int i : exerciseIds) {
            Superset newSuperset = new Superset();
            mWorkout.addSuperset(newSuperset);
            Row newRow = new Row(exercises.get(i));
            Set newSet = new Set();
            newRow.addSet(newSet);
            newSuperset.addRow(newRow);
            db.addRow(newRow, true);

            getListAdapter().notifyItemInserted(mWorkout.getSupersets().indexOf(newSuperset)
                    + getListAdapter().getRowOffset());
//            getListAdapter().getRecyclerView().getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            getListAdapter().getRecyclerView().requestLayout();
        }
        changed = true;
        ending = false;
        updateSetEditor(true);
    }

    // react to changes in active set
    @Override
    public void onCurrentSetChanged(Set set) {
        updateTimer();
//        updateSetEditor(true);
    }

    // react to changes in set order
    @Override
    public void onSetOrderChanged() {
        updateTimer();
    }

    @Override
    public void onCurrentSetModified() {
        updateTimer();
    }

    public Workout getWorkout() {
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DATA_PROVIDER);
        return ((WorkoutDataFragment) fragment).getWorkoutData();
    }

    private RowListAdapter getListAdapter() {
        final RowListFragment fragment = ((RowListFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_LIST_VIEW));
        return fragment != null ? fragment.getAdapter() : null;
    }
}
