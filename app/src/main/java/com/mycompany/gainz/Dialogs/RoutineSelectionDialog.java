package com.mycompany.gainz.Dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.mycompany.gainz.R;

import java.util.Collections;
import java.util.List;

public class RoutineSelectionDialog extends DialogFragment {

    private OnDialogInteractionListener mListener;

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction

        try {
            mListener = (OnDialogInteractionListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement OnDialogInteractionListener");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_addworkout_title)
                .setMessage(R.string.dialog_addworkout_text)
                .setPositiveButton(R.string.dialog_addworkout_newworkout, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogRoutineSelected(Collections.singletonList(-1));
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    public interface OnDialogInteractionListener {
        void onDialogRoutineSelected(List<Integer> routineIDs);
    }
}