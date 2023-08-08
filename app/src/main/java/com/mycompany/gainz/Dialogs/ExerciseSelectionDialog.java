package com.mycompany.gainz.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.mycompany.gainz.R;


public class ExerciseSelectionDialog extends DialogFragment {

    private OnDialogInteractionListener mListener;

    public static ExerciseSelectionDialog newInstance(String[] exerciseNames, int[] exerciseIDs) {
        ExerciseSelectionDialog dialog = new ExerciseSelectionDialog();

        Bundle args = new Bundle();
        args.putStringArray("exerciseNames", exerciseNames);
        args.putIntArray("exerciseIDs", exerciseIDs);
        dialog.setArguments(args);

        return dialog;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        try {
            mListener = (OnDialogInteractionListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement ExerciseSelectionDialog.OnDialogInteractionListener");
        }

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final String[] exerciseNames = getArguments().getStringArray("exerciseNames");
        final int[] exerciseIDs = getArguments().getIntArray("exerciseIDs");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
            getActivity(),
            android.R.layout.select_dialog_singlechoice);

        arrayAdapter.addAll(exerciseNames);

        builder.setTitle(R.string.dialog_addexercise_title)
            .setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mListener.onExerciseSelected(exerciseIDs[which]);
                    dialog.dismiss();
                }
            })
            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

        return builder.create();
    }

    public interface OnDialogInteractionListener {
        void onExerciseSelected(int exercise_id);
    }
}
