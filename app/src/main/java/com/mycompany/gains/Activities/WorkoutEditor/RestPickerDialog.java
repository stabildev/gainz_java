package com.mycompany.gains.Activities.WorkoutEditor;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mycompany.gains.Data.Model.Row;
import com.mycompany.gains.R;
import com.mycompany.gains.Stuff.Misc;

import de.mrapp.android.dialog.MaterialDialogBuilder;

import static com.mycompany.gains.Arguments.*;

/**
 * Created by Klee on 11.11.2015.
 */
public class RestPickerDialog extends DialogFragment {
    private int rest;
    private int maxRest;
    private int stepSize;
    private String row;

    private RestPickerListener mListener;

    public static RestPickerDialog newInstance(Row row) {
        RestPickerDialog dialog = new RestPickerDialog();

        Bundle args = new Bundle();
        Log.i("RestPicker", "(newInstance) rowCoordinates = " + row.getCoordinates());
        args.putString(ARG_ROW, row.getCoordinates());
        args.putInt(ARG_INITIAL, row.getRest());
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rest = getArguments().getInt(ARG_INITIAL);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        stepSize = Integer.parseInt(prefs.getString(getString(R.string.pref_key_rest_increment), "15"));
        maxRest = Integer.parseInt(prefs.getString(getString(R.string.pref_key_timer_max), "" + 5)) * 60;
        row = getArguments().getString(ARG_ROW);

        try {
            mListener = (RestPickerListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException("Calling Fragment must implement RestPickerListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialogBuilder dialogBuilder = new MaterialDialogBuilder(getContext());
        dialogBuilder.setTitle(R.string.dialog_rest_picker_title);

        View layout = View.inflate(getContext(), R.layout.rest_selector, null);

        final TextView progressTextView = (TextView) layout.findViewById(R.id.progress_text);
        progressTextView.setText(Misc.formatTime(rest));

        final SeekBar seekBar = (SeekBar) layout.findViewById(R.id.seek_bar);
        seekBar.setMax(maxRest);
        seekBar.setProgress(rest);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rest = Misc.roundTime(progress, 15);
                progressTextView.setText(Misc.formatTime(rest));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        dialogBuilder.setView(layout);

        dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onRestPicked(row, rest);
            }
        });
        dialogBuilder.setNegativeButton(android.R.string.cancel, null);
        return dialogBuilder.create();
    }

    public interface RestPickerListener {
        void onRestPicked(String rowCoordinates, int rest);
    }
}
