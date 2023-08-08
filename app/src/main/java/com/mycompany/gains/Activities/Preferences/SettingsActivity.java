package com.mycompany.gains.Activities.Preferences;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.mycompany.gains.Activities.BaseActivity;
import com.mycompany.gains.R;

/**
 * Created by Klee on 12.08.2015.
 */
public class SettingsActivity extends BaseActivity {

    private ActionBar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        // initialize toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mToolbar = getSupportActionBar();

        if (mToolbar != null) {
            mToolbar.setTitle(R.string.action_settings);
            mToolbar.setHomeButtonEnabled(true);
            mToolbar.setDisplayHomeAsUpEnabled(true);
        }
    }

}
