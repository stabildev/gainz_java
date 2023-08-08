package com.mycompany.gains.Activities.WorkoutEditor.ExerciseSelection;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.view.WindowManager;

import com.mycompany.gains.Data.Model.Exercise;
import com.mycompany.gains.widgets.EmptyRecyclerView;
import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.mrapp.android.dialog.MaterialDialogBuilder;


public class ExerciseSelectionDialog extends DialogFragment {

    private OnDialogInteractionListener mListener;
    private DatabaseHelper db;
    private EmptyRecyclerView mRecyclerView;
    private ExerciseSelectionAdapter mAdapter;
    private AlertDialog mDialog;
    private boolean multiSelect = false;

    public static ExerciseSelectionDialog newInstance(boolean multiSelect) {
        ExerciseSelectionDialog dialog = new ExerciseSelectionDialog();
        Bundle args = new Bundle();
        args.putInt("multiSelect", multiSelect ? 1 : 0);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        try {
            mListener = (OnDialogInteractionListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement ExerciseSelectionDialog.OnDialogInteractionListener");
        }

        db = DatabaseHelper.getInstance(getActivity());

        multiSelect = getArguments().getInt("multiSelect") == 1;

        View view = View.inflate(getActivity(), R.layout.dialog_exercise_selection, null);

        // Use the Builder class for convenient dialog construction
        MaterialDialogBuilder builder = new MaterialDialogBuilder(getActivity());

        builder.setTitle(R.string.dialog_exercise_selection_title)
                .setMessage(R.string.dialog_exercise_selection_message)
            .setView(view)
            .setPositiveButton(R.string.dialog_addexercise_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!mAdapter.getSelectedIds().isEmpty())
                        mListener.onExercisesSelected(getTag(), mAdapter.getSelectedIds());
                    dialog.dismiss();
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

        setupExerciseList(view);

        mDialog  = builder.create();
        mDialog.show();

        // fix bug: editText not showing keyboard
        mDialog.getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        return mDialog;
    }

    private void setupExerciseList(View view) {

        mRecyclerView = (EmptyRecyclerView) view.findViewById(R.id.dialog_exerciseSelection_list);
        mRecyclerView.setEmptyView(view.findViewById(R.id.dialog_exerciseSelection_empty));

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Set the adapter
        List<Exercise> data = new ArrayList<>(db.getExercises().values());
        Collections.sort(data);
        mAdapter = new ExerciseSelectionAdapter(getActivity(), data);
        mRecyclerView.setAdapter(mAdapter);
        //mRecyclerView.scrollToPosition(1);

        mAdapter.setOnSelectionListener(new ExerciseSelectionAdapter.ExerciseSelectionListener() {
            @Override
            public void onSelectionChanged(int selectionSize) {
                switch (selectionSize) {
                    case 1:
                        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        if (!multiSelect) {
                            mListener.onExercisesSelected(getTag(), mAdapter.getSelectedIds());
                            mDialog.dismiss();
                        }
                        return;
                    case 0:
                        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        //return;
                }
            }

            @Override
            public void onAddExercise(String name) {
                Exercise newExercise = new Exercise(name);
                db.addExercise(newExercise);
                int adapterPos = mAdapter.addExercise(newExercise);

                // scroll to new exercise
                mRecyclerView.smoothScrollToPosition(adapterPos);
            }

            @Override
            public void onLongClick(int _id) {
                // TODO delete / edit exercise
            }
        });
    }

    public interface OnDialogInteractionListener {
        void onExercisesSelected(String tag, List<Integer> exercise_ids);
    }
}
