package com.mycompany.gains.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.mycompany.gains.Activities.Preferences.SettingsActivity;
import com.mycompany.gains.Activities.WorkoutEditorOld.WorkoutEditorOld;
import com.mycompany.gains.Activities.WorkoutEditor.WorkoutEditor;
import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.Globals;
import com.mycompany.gains.R;

import java.util.Collections;
import java.util.List;

import de.mrapp.android.dialog.MaterialDialogBuilder;

import static com.mycompany.gains.Arguments.*;

/**
 * Created by Klee on 06.11.2015.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected DatabaseHelper db;

    public void backToWorkout(View v) {
        Globals globals = Globals.getInstance();
        if (globals.isWorkoutActive()) {
            Intent intent = new Intent(this, WorkoutEditor.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            this.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DatabaseHelper.exportDatabase("gains_db_backup");
        db = DatabaseHelper.getInstance(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // show "back to workout" bar if workout is active
        View backToWorkout = findViewById(R.id.back_to_workout);
        if (backToWorkout != null)
            backToWorkout.setVisibility(
                    Globals.getInstance().isWorkoutActive() ? View.VISIBLE : View.GONE
            );
    }

    public void viewWorkout(int id) {
        Intent intent = new Intent(this, WorkoutViewer.class);
        intent.putExtra(ARG_ID, id);

        startActivity(intent);
    }

    public void editWorkout(int workoutId) {
        // don't edit workout if workout already active
        if (Globals.getInstance().isWorkoutActive()) {
            Toast.makeText(this, getString(R.string.toast_workout_already_active), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, WorkoutEditorOld.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Bundle bundle = new Bundle();
        bundle.putIntArray(ARG_IDS, new int[]{workoutId});
        bundle.putBoolean(ARG_EDIT, true);

        intent.putExtras(bundle);

        this.startActivity(intent);
    }

    public void deleteWorkout(int workoutId, @Nullable DialogInterface.OnClickListener onDelete) {
        deleteWorkouts(Collections.singletonList(workoutId), onDelete);
    }

    public void deleteWorkouts(final List<Integer> workoutIds, @Nullable DialogInterface.OnClickListener onDelete) {
        boolean routine = db.getWorkout(workoutIds.get(0)).isRoutine();
        new MaterialDialogBuilder(BaseActivity.this)
                .setTitle(getString(R.string.dialog_delete_title))
                .setMessage(getString(R.string.dialog_delete_message_a)

                        + (routine ? getString(workoutIds.size() > 1
                        ? R.string.the_selected_routines : R.string.the_selected_routine)
                        : getString(workoutIds.size() > 1
                        ? R.string.the_selected_workouts : R.string.the_selected_workout))

                        + getString(R.string.dialog_delete_message_b))

                .setPositiveButton(getString(R.string.dialog_delete), onDelete == null ? new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (int id : workoutIds)
                            db.dropWorkout(id);
                    }
                } : onDelete)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void editWorkout2(int workoutId) {
        // don't edit workout if workout already active
        if (Globals.getInstance().isWorkoutActive()) {
            Toast.makeText(this, getString(R.string.toast_workout_already_active), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, WorkoutEditor.class);

        Bundle bundle = new Bundle();
        bundle.putIntArray(ARG_IDS, new int[]{workoutId});
        bundle.putBoolean(ARG_EDIT, true);

        intent.putExtras(bundle);

        this.startActivity(intent);
    }

    public void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}
