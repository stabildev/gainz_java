package com.mycompany.gains.Activities.Preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.Toast;

import com.mycompany.gains.Data.Database.DatabaseHelper;
import com.mycompany.gains.Data.Database.DummyData;
import com.mycompany.gains.R;

/**
 * Created by Klee on 12.08.2015.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findPreference(getString(R.string.pref_key_cleanup_exercises)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DatabaseHelper.getInstance(getActivity()).deleteUnusedExercises();
                Toast.makeText(getActivity(), getString(R.string.pref_cleanup_exercises_done), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        findPreference(getString(R.string.pref_key_recreate_dummy_data)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new DummyData(DatabaseHelper.getInstance(getActivity())).createAll();
                Toast.makeText(getActivity(), getString(R.string.pref_recreate_dummy_data_done), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        findPreference(getString(R.string.pref_key_clear_database)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.pref_clear_database_dialog))
                        .setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DatabaseHelper db = DatabaseHelper.getInstance(getActivity());
                                db.onUpgrade(db.getWritableDatabase(), 1, 1);
                                Toast.makeText(getActivity(), getString(R.string.pref_clear_database_done), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), null)
                        .show();
                return true;
            }
        });
    }
}
