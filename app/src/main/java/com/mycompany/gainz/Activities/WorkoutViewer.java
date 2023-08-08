package com.mycompany.gainz.Activities;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mycompany.gainz.Database.Helper.DBHelper;
import com.mycompany.gainz.Database.Model.Set;
import com.mycompany.gainz.Database.Model.Workout;
import com.mycompany.gainz.R;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.SortedSet;

public class WorkoutViewer extends AppCompatActivity {

    private DBHelper db;

    Workout mWorkout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_viewer);

        int workout_id = getIntent().getIntExtra("workout_id",-1);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar toolbar = getSupportActionBar();

        if (toolbar != null) {
            toolbar.setTitle(R.string.workout_details_title);
            toolbar.setHomeButtonEnabled(true);
            toolbar.setDisplayHomeAsUpEnabled(true);
        }

        db = DBHelper.getInstance(this);

        initializeData(workout_id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_workout_view, menu);
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
        } else if (id == R.id.action_workoutview_delete) {
            db.dropWorkout(mWorkout.get_id());
            this.finish();
        } else if (id == R.id.action_workoutview_edit) {
            editWorkout(mWorkout.get_id());
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeData(int workout_id) {

        // retrieve workout metadata
        mWorkout = db.getWorkout(workout_id);

        if (mWorkout.get_id() == -1) {
            Toast.makeText(getApplicationContext(), "Workout not found.", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        // setting workout title
        String title = mWorkout.getName();

        if (title.equals(""))
            title = getString(R.string.unnamed_workout);

        ((TextView) findViewById(R.id.workout_title)).setText(title);

        // setting workout note
        if (!(mWorkout.getNote().equals(""))) {
            ((TextView) findViewById(R.id.workout_note)).setText(mWorkout.getNote());
            findViewById(R.id.workout_note).setVisibility(View.VISIBLE);
        }

        // setting workout date
        Calendar date = mWorkout.getDate();
        DateFormat df = DateFormat.getDateInstance();
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        ((TextView) findViewById(R.id.workout_date)).setText(df.format(date.getTime())
                + "\n" + tf.format(date.getTime()));


        // retrieving rows and sets

        int currentSuperset, previousSuperset = -1;
        String data = "";

        SortedSet<Set> setsInRow;
        int setNo;

        for (int row : mWorkout.setsInRows.keySet()) {
            setsInRow = mWorkout.setsInRows.get(row);
            currentSuperset = setsInRow.first().getSuperset();

            if (previousSuperset != -1 && previousSuperset != currentSuperset )
                data += "<br />";

            data += "<b>" + setsInRow.first().getExercise().getName()  + "</b>:<br />";

            setNo = 0;
            for (Set s : setsInRow) {
                if (s.isDone()) {

                    if (setNo++ > 0 )
                        data += "&emsp;";

                    data += s.getReps();

                    if (s.getWeight() != 0)
                        data += "×" + s.getFormattedWeight();

                    if (s.getRest() > 0)
                        data += " (" + s.getRest() + ")";
                }
            }

            data += "<br />";
            previousSuperset = currentSuperset;
        }

        ((TextView) findViewById(R.id.workout_content)).setText(Html.fromHtml(data),TextView.BufferType.SPANNABLE);
    }

    public void editWorkout(int workout_id) {
        Intent intent = new Intent(this, WorkoutEditor.class);

        Bundle bundle = new Bundle();
        bundle.putIntArray("makeFrom", new int[]{workout_id});
        bundle.putInt("edit", 1);

        intent.putExtras(bundle);

        this.startActivity(intent);
    }
}
