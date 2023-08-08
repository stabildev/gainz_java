package com.mycompany.gains.Activities.MainActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.TabLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.mycompany.gains.Activities.BaseActivity;
import com.mycompany.gains.Activities.MainActivity.adapters.RoutineListAdapter;
import com.mycompany.gains.Activities.WorkoutEditor.WorkoutEditor;
import com.mycompany.gains.Activities.WorkoutEditorOld.WorkoutEditorOld;
import com.mycompany.gains.Activities.WorkoutViewer;
import com.mycompany.gains.Adapters.MSCursorRecyclerAdapter;
import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.Activities.MainActivity.adapters.MainPagerAdapter;
import com.mycompany.gains.Activities.MainActivity.fragments.HistoryListFragment;
import com.mycompany.gains.Activities.MainActivity.fragments.RoutineListFragment;
import com.mycompany.gains.Data.Model.Workout;
import com.mycompany.gains.Globals;
import com.mycompany.gains.R;

import java.util.Collections;
import java.util.List;

import static com.mycompany.gains.Arguments.*;

public class MainActivity extends BaseActivity
        implements MSCursorRecyclerAdapter.SelectionListener {

    DatabaseHelper db;
    MainPagerAdapter mainPagerAdapter;
    FloatingActionsMenu fam;
    FloatingActionButton fabNewFromRoutines;
    FloatingActionButton fabCopyWorkouts;

    private ActionMode mActionMode;
    private enum ActionModeState {
        ROUTINE_SELECTION,
        WORKOUT_SELECTION,
        NULL }
    private ActionModeState mActionModeState = ActionModeState.NULL;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar toolbar = getSupportActionBar();

        if (toolbar != null) {
            toolbar.setTitle(R.string.app_name);
            toolbar.setDisplayShowHomeEnabled(true);
            //toolbar.setIcon(R.mipmap.ic_launcher);
        }

        db = DatabaseHelper.getInstance(this);

        // create database and dummy data if it doesn't exist
        db.initialize();

        // Get the ViewPager and set its PagerAdapter so that it can display items
        final ViewPager viewPager = (ViewPager) findViewById(R.id.main_viewPager);
        mainPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), this);
        viewPager.setAdapter(mainPagerAdapter);

        // Give the ViewPager to the TabLayout and set colors
        TabLayout tabLayout = (TabLayout) findViewById(R.id.main_slidingTabs);

        tabLayout.setupWithViewPager(viewPager);

        // onPageChange, end ActionMode
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (mActionMode != null)
                    mActionMode.finish();
                fam.collapse();
                if (position == MainPagerAdapter.ROUTINES_FRAGMENT)
                    findViewById(R.id.fam_use_routine).setVisibility(View.GONE);
                else
                    findViewById(R.id.fam_use_routine).setVisibility(View.VISIBLE);
            }
        });

        // initialize floating action menu and on click listeners
        fam = (FloatingActionsMenu) findViewById(R.id.floating_menu);
        findViewById(R.id.fam_free_workout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEmptyWorkout();
            }
        });
        findViewById(R.id.fam_use_routine).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(MainPagerAdapter.ROUTINES_FRAGMENT);
                fam.collapse();
            }
        });
        // prepare Action Mode Fabs
        // tint icons
        Drawable icCopy = ContextCompat.getDrawable(this, R.drawable.ic_content_copy_white_24dp).mutate();
        Drawable icArrowForward = ContextCompat.getDrawable(this, R.drawable.ic_arrow_forward_white_24dp).mutate();
        icCopy.setColorFilter(ContextCompat.getColor(this, R.color.icons), PorterDuff.Mode.MULTIPLY);
        icArrowForward.setColorFilter(ContextCompat.getColor(this, R.color.icons), PorterDuff.Mode.MULTIPLY);

        fabNewFromRoutines = new FloatingActionButton(MainActivity.this);
        fabNewFromRoutines.setTitle(getString(R.string.action_newfrom_routines));
        fabNewFromRoutines.setColorNormalResId(R.color.white);
        fabNewFromRoutines.setColorPressedResId(R.color.white_pressed);
        fabNewFromRoutines.setIconDrawable(icArrowForward);
        fabNewFromRoutines.setSize(FloatingActionButton.SIZE_MINI);
        fabNewFromRoutines.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWorkout(routineList().getAdapter().getSelectedIds());
                mActionMode.finish();
            }
        });

        fabCopyWorkouts = new FloatingActionButton(MainActivity.this);
        fabCopyWorkouts.setTitle(getString(R.string.action_duplicate_selected));
        fabCopyWorkouts.setColorNormalResId(R.color.white);
        fabCopyWorkouts.setColorPressedResId(R.color.white_pressed);
        fabCopyWorkouts.setIconDrawable(icCopy);
        fabCopyWorkouts.setSize(FloatingActionButton.SIZE_MINI);
        fabCopyWorkouts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWorkout(historyList().getAdapter().getSelectedIds());
                mActionMode.finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();

            switch (mActionModeState) {
                case ROUTINE_SELECTION:
                    inflater.inflate(R.menu.action_routine, menu);
                    fam.addButton(fabNewFromRoutines);
                    break;
                case WORKOUT_SELECTION:
                    inflater.inflate(R.menu.action_history, menu);
                    fam.addButton(fabCopyWorkouts);
                    break;
            }

            if (Globals.getInstance().isWorkoutActive()) {
                menu.findItem(R.id.action_edit).setEnabled(false).getIcon().mutate().setAlpha(50);
                menu.findItem(R.id.action_duplicate).setEnabled(false).getIcon().mutate().setAlpha(50);
            }
            return true;
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
            final boolean routine = mActionModeState == ActionModeState.ROUTINE_SELECTION;
            switch (item.getItemId()) {
                case R.id.action_delete:
                    List<Integer> ids = routine ? routineList().getAdapter().getSelectedIds()
                            : historyList().getAdapter().getSelectedIds();

                    deleteWorkouts(ids, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (routine)
                                routineList().deleteSelectedRoutines();
                            else
                                historyList().deleteSelectedWorkouts();

                            mode.finish();
                        }
                    });
                    return true;
                case R.id.action_edit:
                    if (routine)
                        editWorkout(routineList().getAdapter().getSelectedIds().get(0));
                    else
                        editWorkout(historyList().getAdapter().getSelectedIds().get(0));
                    mode.finish();
                    return true;
                case R.id.action_duplicate:
                    if (routine)
                        startWorkout(routineList().getAdapter().getSelectedIds());
                    else
                        startWorkout(historyList().getAdapter().getSelectedIds());
                    mode.finish();
                    return true;
                case R.id.action_save_as_routine:
                    Workout w = db.getWorkout(historyList().getAdapter().getSelectedIds().get(0));
                    w.setIsRoutine(true);
                    db.addWorkout(w);
                    mode.finish();
                    routineList().onResume();
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            switch (mActionModeState) {
                case ROUTINE_SELECTION:
                    routineList().getAdapter().clearSelection();
                    fam.removeButton(fabNewFromRoutines);
                    break;
                case WORKOUT_SELECTION:
                    historyList().getAdapter().clearSelection();
                    fam.removeButton(fabCopyWorkouts);
                    break;
            }
            mActionMode = null;
            mActionModeState = ActionModeState.NULL;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        db.closeDB();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            openSettings();
            return true;
        } else if (id == R.id.action_test) {
            startActivity(new Intent(this, WorkoutViewer.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startWorkout(List<Integer> makeFrom) {
        Intent intent = new Intent(this, WorkoutEditor.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int[] intarray = new int[makeFrom.size()];
        for (int i = 0; i < makeFrom.size(); i++)
            intarray[i] = makeFrom.get(i);

        Bundle bundle = new Bundle();
        bundle.putIntArray(ARG_IDS, intarray);
        bundle.putBoolean(ARG_EDIT, false);

        intent.putExtras(bundle);

        this.startActivity(intent);
    }

    public void startEmptyWorkout() {
        startWorkout(Collections.singletonList(-1));
    }

    private RoutineListFragment routineList() {
        return (RoutineListFragment) mainPagerAdapter.getRegisteredFragment(MainPagerAdapter.ROUTINES_FRAGMENT);
    }

    private HistoryListFragment historyList() {
        return (HistoryListFragment) mainPagerAdapter.getRegisteredFragment(MainPagerAdapter.HISTORY_FRAGMENT);
    }

    public void startSelection(ActionModeState actionModeState) {
        // Start the CAB using the ActionMode.Callback defined above
        if (mActionModeState != actionModeState) {
            if (mActionMode != null)
                mActionMode.finish();
            mActionModeState = actionModeState;
            mActionMode = startSupportActionMode(mActionModeCallback);
        }
    }

    public void stopSelection() {
        if (mActionMode != null)
            mActionMode.finish();
    }

    public RoutineListAdapter.RoutineAdapterListener getRoutineListListener() {
        return new RoutineListAdapter.RoutineAdapterListener() {
            @Override
            public void onRoutineStarred(int id, boolean starred) {
                db.starWorkout(id, starred);
            }

            @Override
            public void onSelectionChanged(int selectionSize) {
                if (selectionSize > 0) {
                    startSelection(ActionModeState.ROUTINE_SELECTION);

                    mActionMode.setTitle(selectionSize + " " +
                            (selectionSize > 1 ? getString(R.string.routines_selected)
                                    : getString(R.string.routine_selected)));

                    final Menu menu = mActionMode.getMenu();

                    // disable edit if more than one workout is selected or workout is active
                    if (!Globals.getInstance().isWorkoutActive() && selectionSize == 1)
                        menu.findItem(R.id.action_edit).setEnabled(true).getIcon().mutate().setAlpha(255);
                    else
                        menu.findItem(R.id.action_edit).setEnabled(false).getIcon().mutate().setAlpha(50);
                } else
                    stopSelection();
            }

            @Override
            public void onClick(int id) {
                startWorkout(Collections.singletonList(id));
            }
        };
    }

    @Override
    public MSCursorRecyclerAdapter.OnSelectionListener getOnSelectionListener(final Object fragment) {
        if (fragment instanceof HistoryListFragment) {
            return new MSCursorRecyclerAdapter.OnSelectionListener() {
                @Override
                public void onSelectionChanged(int selectionSize) {
                    if (selectionSize > 0) {
                        startSelection(ActionModeState.WORKOUT_SELECTION);

                        mActionMode.setTitle(selectionSize + " " +
                                (selectionSize > 1 ? getString(R.string.workouts_selected)
                                        : getString(R.string.workout_selected)));

                        final Menu menu = mActionMode.getMenu();

                        // disable "save as routine" if more than workout is selected"
                        if (selectionSize == 1)
                            menu.findItem(R.id.action_save_as_routine).setEnabled(true);
                        else
                            menu.findItem(R.id.action_save_as_routine).setEnabled(false);

                        // disable edit if more than one workout is selected or workout is active
                        if (!Globals.getInstance().isWorkoutActive() && selectionSize == 1)
                            menu.findItem(R.id.action_edit).setEnabled(true).getIcon().mutate().setAlpha(255);
                        else
                            menu.findItem(R.id.action_edit).setEnabled(false).getIcon().mutate().setAlpha(50);

                        // disable delete if active workout is selected
                        if (Globals.getInstance().isWorkoutActive() && historyList().getAdapter().getSelectedPositions().contains(0))
                            menu.findItem(R.id.action_delete).setEnabled(false).getIcon().mutate().setAlpha(50);
                        else
                            menu.findItem(R.id.action_delete).setEnabled(true).getIcon().mutate().setAlpha(255);
                    } else
                        stopSelection();
                }

                @Override
                public void onClick(int id) {
                    viewWorkout(id);
                }
            };
        }
        else
            return null;
    }

    @Override
    public void onBackPressed() {
        if (fam.isExpanded())
            fam.collapse();
        else if (mActionMode != null)
            mActionMode.finish();
        else if (Globals.getInstance().isWorkoutActive()) {
            // close app
            moveTaskToBack(true);
        } else
            super.onBackPressed();
    }
}
