package com.mycompany.gains.Activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Data.Model.Superset;
import com.mycompany.gains.Data.Model.Workout;
import com.mycompany.gains.Globals;
import com.mycompany.gains.R;
import com.mycompany.gains.widgets.WorkoutView;

import java.text.DateFormat;
import java.util.Calendar;

import static com.mycompany.gains.Arguments.*;

public class WorkoutViewer extends BaseActivity {

    private DatabaseHelper db;
    private Workout mWorkout;

    private ActionBar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_viewer);

        // initialize toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mToolbar = getSupportActionBar();

        if (mToolbar != null) {
            mToolbar.setTitle(R.string.workout_details_title);
            mToolbar.setHomeButtonEnabled(true);
            mToolbar.setDisplayHomeAsUpEnabled(true);
        }

        db = DatabaseHelper.getInstance(this);

        initializeData();
    }

    public void initializeData() {
        // retrieve workout metadata
        mWorkout = db.getWorkout(getIntent().getIntExtra(ARG_ID, -1));

        if (mWorkout.getDatabaseId() == -1) {
            Toast.makeText(getApplicationContext(), "Workout not found.", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        // initialize fields
        TextView workoutTitle = (TextView) findViewById(R.id.workout_title);
        TextView workoutDate = (TextView) findViewById(R.id.workout_date);
        TextView workoutNote = (TextView) findViewById(R.id.workout_note);

        // setting workout title
        String title = mWorkout.getName();

        if (title.equals(""))
            title = getString(R.string.unnamed_workout);

        workoutTitle.setText(title);

        // setting workout note
        workoutNote.setText(mWorkout.getNote());

        // setting workout date
        Calendar date = mWorkout.getDate();
        DateFormat df = DateFormat.getDateInstance();
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        workoutDate.setText(df.format(date.getTime())
                + "\n" + tf.format(date.getTime()));

        // initialize WorkoutView
        WorkoutView workoutView = (WorkoutView) findViewById(R.id.workout_view);
        workoutView.setWorkout(mWorkout);
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_workout_viewer, menu);
        if (Globals.getInstance().isWorkoutActive())
            menu.findItem(R.id.action_edit).setEnabled(false).getIcon().mutate().setAlpha(50);
        if (Globals.getInstance().getActiveWorkoutId() == mWorkout.getDatabaseId())
            menu.findItem(R.id.action_delete).setEnabled(false).getIcon().mutate().setAlpha(50);

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
            case R.id.action_delete:
                deleteWorkout(mWorkout.getDatabaseId(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.dropWorkout(mWorkout.getDatabaseId());
                        WorkoutViewer.this.finish();
                    }
                });
                return true;
            case R.id.action_edit:
                editWorkout2(mWorkout.getDatabaseId());
                return true;
            case R.id.action_share:
                return true;
            default:
                return false;
        }
    }

    public String plainText() {
        String data = "";

        // setting workout title
        String title = mWorkout.getName();

        if (title.equals(""))
            title = getString(R.string.unnamed_workout);

        data += title + "<br />";

        // setting workout note
        if (!(mWorkout.getNote().equals(""))) {
            data += "[" + mWorkout.getNote() + "]<br />";
        }

        data += "<br />";

        // setting workout date
        Calendar date = mWorkout.getDate();
        DateFormat df = DateFormat.getDateInstance();
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        ((TextView) findViewById(R.id.workout_date)).setText(df.format(date.getTime())
                + "\n" + tf.format(date.getTime()));


        // retrieving rows and sets

        for (Superset s : mWorkout.getSupersets()) {
            if (s.getPosition() != 0)
                data += "<br />";

            for (Row r : s.getRows()) {
                data += "<b>" + r.getExercise().getName() + "</b>:<br />";

                if (r.getNote().length() > 0)
                    data += "<i>" + r.getNote() + "</i><br />";

                for (Set set : r.getSets()) {
                    if (set.isDone()) {

                        if (set.getPosition() > 0)
                            data += "&emsp;";

                        data += set.getReps();

                        if (set.getWeight() != 0)
                            data += "×" + set.getWeightFormatted(true);

                        if (set.getRest() > 0)
                            data += " (" + set.getRest() + ")";
                    }
                }
                data += "<br />";
            }
        }
        return data;
    }
}
