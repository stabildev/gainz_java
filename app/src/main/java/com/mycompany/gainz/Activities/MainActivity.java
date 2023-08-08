package com.mycompany.gainz.Activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mycompany.gainz.Adapters.MainPagerAdapter;
import com.mycompany.gainz.Database.Helper.DBHelper;
import com.mycompany.gainz.Dialogs.RoutineSelectionDialog;
import com.mycompany.gainz.Fragments.HistoryListFragment;
import com.mycompany.gainz.Fragments.RoutineListFragment;
import com.mycompany.gainz.Fragments.StartFragment;
import com.mycompany.gainz.R;

import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements HistoryListFragment.OnFragmentInteractionListener,
        StartFragment.OnFragmentInteractionListener, RoutineListFragment.OnFragmentInteractionListener,
        RoutineSelectionDialog.OnDialogInteractionListener {

    DBHelper db;
    ActionBar mToolbar;
    MainPagerAdapter mainPagerAdapter;
    private ActionMode mActionMode;

    private android.support.design.widget.FloatingActionButton mActionButton;

    private final static String TAG = "MainWindow";

    private enum ActionModeState {
        ROUTINE_SELECTION,
        WORKOUT_SELECTION,
        NULL }

    private ActionModeState actionModeState = ActionModeState.NULL;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mToolbar = getSupportActionBar();

        if (mToolbar != null) {
            mToolbar.setTitle(R.string.app_name);
            mToolbar.setDisplayShowHomeEnabled(true);
            //mToolbar.setIcon(R.mipmap.ic_launcher);
        }

        db = DBHelper.getInstance(this);

        // Get the ViewPager and set its PagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.main_viewPager);
        mainPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), MainActivity.this);
        viewPager.setAdapter(mainPagerAdapter);

        // Give the ViewPager to the TabLayout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.main_slidingTabs);

        try {
            XmlResourceParser parser = getResources().getXml(R.drawable.tab_text_colors);
            ColorStateList tab_text_colors = ColorStateList.createFromXml(getResources(), parser);
            tabLayout.setTabTextColors(tab_text_colors);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't set tabTextColors", e);
        }

        tabLayout.setupWithViewPager(viewPager);

        // onPageChange, end ActionMode
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (mActionMode != null)
                    mActionMode.finish();
            }
        });

        viewPager.setCurrentItem(1);

        mActionButton = (android.support.design.widget.FloatingActionButton) findViewById(R.id.main_actionButton);
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();

            if (actionModeState == ActionModeState.ROUTINE_SELECTION) {
                inflater.inflate(R.menu.action_routine, menu);

                // set FAB icon to new workout
                if (Build.VERSION.SDK_INT < 21)
                    mActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_library_add_white_24dp));
                else
                    mActionButton.setImageDrawable(getDrawable(R.drawable.ic_library_add_white_24dp));

                return true;

            } else if (actionModeState == ActionModeState.WORKOUT_SELECTION) {
                inflater.inflate(R.menu.action_history, menu);

                // set FAB icon to new workout
                if (Build.VERSION.SDK_INT < 21)
                    mActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_library_add_white_24dp));
                else
                    mActionButton.setImageDrawable(getDrawable(R.drawable.ic_library_add_white_24dp));

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
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete_routine:
                    routineList().deleteSelectedRoutines();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.action_edit_routine:
                    if (routineList().getSelection().size() == 1) {
                        editWorkout(routineList().getSelection().get(0));
                        mode.finish();
                    }
                    else
                        Toast.makeText(getApplicationContext(), "Can't edit multiple routines at once!", Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.action_newfrom_routines:
                    startWorkout(routineList().getSelection());
                    mode.finish();
                    return true;
                case R.id.action_delete_workout:
                    historyList().deleteSelectedWorkouts();
                    mode.finish();
                    return true;
                case R.id.action_edit_workout:
                    if (historyList().getSelection().size() == 1) {
                        editWorkout(historyList().getSelection().get(0));
                        mode.finish();
                    }
                    else
                        Toast.makeText(getApplicationContext(), "Can't edit multiple workouts at once!", Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.action_newfrom_workouts:
                    startWorkout(historyList().getSelection());
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (actionModeState == ActionModeState.ROUTINE_SELECTION)
                routineList().clearSelection();
            else if (actionModeState == ActionModeState.WORKOUT_SELECTION)
                historyList().clearSelection();
            mActionMode = null;
            actionModeState = ActionModeState.NULL;

            // reset FAB icon
            if (Build.VERSION.SDK_INT < 21)
                mActionButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_add_white_24dp));
            else
                mActionButton.setImageDrawable(getDrawable(R.drawable.ic_add_white_24dp));
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
            return true;
        } else if (id == R.id.action_reset_database) {
            db.onUpgrade(db.getWritableDatabase(),1,1);
            historyList().onResume();
            routineList().onResume();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActionButtonClick(View v) {
        if (mActionMode != null) {
            if ( actionModeState == ActionModeState.ROUTINE_SELECTION
                    && routineList().getSelection().size() > 0) {
                startWorkout(routineList().getSelection());
                mActionMode.finish();
            }
            else if (actionModeState == ActionModeState.WORKOUT_SELECTION
                    && historyList().getSelection().size() > 0) {
                startWorkout(historyList().getSelection());
                mActionMode.finish();
            }
        }
        else
            showAddWorkoutDialog();
    }

    public void startWorkout(List<Integer> makeFrom) {
        Intent intent = new Intent(this, WorkoutEditor.class);

        int[] intarray = new int[makeFrom.size()];
        for (int i = 0; i < makeFrom.size(); i++)
            intarray[i] = makeFrom.get(i);

        Bundle bundle = new Bundle();
        bundle.putIntArray("makeFrom", intarray);
        bundle.putInt("edit", 0);

        intent.putExtras(bundle);

        this.startActivity(intent);
    }

    public void editWorkout(int workout_id) {
        Intent intent = new Intent(this, WorkoutEditor.class);

        Bundle bundle = new Bundle();
        bundle.putIntArray("makeFrom", new int[]{workout_id});
        bundle.putInt("edit", 1);

        intent.putExtras(bundle);

        this.startActivity(intent);
    }

    public void showAddWorkoutDialog() {
/*        RoutineSelectionDialog dialog = new RoutineSelectionDialog();
        dialog.show(getFragmentManager(), "dialog");*/
        startWorkout(Collections.singletonList(-1));
    }

    public void onStartFragmentInteraction(View v) {
        if (v == findViewById(R.id.start_button)) {
            showAddWorkoutDialog();
        }
    }

    public void onStartRoutineSelection() {
        // Start the CAB using the ActionMode.Callback defined above
        if (actionModeState != ActionModeState.ROUTINE_SELECTION) {
            if (mActionMode != null)
                mActionMode.finish();
            actionModeState = ActionModeState.ROUTINE_SELECTION;
            mActionMode = this.startSupportActionMode(mActionModeCallback);
        }
    }

    public void onStopRoutineSelection() {
        if (mActionMode != null)
            mActionMode.finish();
    }

    public void onClickRoutine(int _id) {

    }

    public void onStartWorkoutSelection() {
        // Start the CAB using the ActionMode.Callback defined above
        if (actionModeState != ActionModeState.WORKOUT_SELECTION) {
            if (mActionMode != null)
                mActionMode.finish();
            actionModeState = ActionModeState.WORKOUT_SELECTION;
            mActionMode = this.startSupportActionMode(mActionModeCallback);
        }
    }

    public void onStopWorkoutSelection() {
        if (mActionMode != null)
            mActionMode.finish();
    }

    public void onClickWorkout(int _id) {
        Intent intent = new Intent(this, WorkoutViewer.class);
        intent.putExtra("workout_id", _id);

        this.startActivity(intent);
    }


    private RoutineListFragment routineList() {
        return (RoutineListFragment) mainPagerAdapter.getRegisteredFragment(MainPagerAdapter.ROUTINES_FRAGMENT);
    }

    private HistoryListFragment historyList() {
        return (HistoryListFragment) mainPagerAdapter.getRegisteredFragment(MainPagerAdapter.HISTORY_FRAGMENT);
    }

    @Override
    public void onBackPressed() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
        else
            super.onBackPressed();
    }

    @Override
    public void onDialogRoutineSelected(List<Integer> routineIDs) {
        startWorkout(routineIDs);
    }
}
