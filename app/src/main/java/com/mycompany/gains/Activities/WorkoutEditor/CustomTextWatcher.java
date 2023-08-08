package com.mycompany.gains.Activities.WorkoutEditor;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

/**
 * Created by Klee on 09.11.2015.
 */
public abstract class CustomTextWatcher implements View.OnFocusChangeListener, TextWatcher {
    private long delay;
    private boolean activated = false;
    private String text = "";
    private Handler handler = new Handler();
    private Runnable timeOut = new Runnable() {
        @Override
        public void run() {
            onFocusLostOrTimeOut(text);
            textChanged = false;
        }
    };
    boolean textChanged = false;

    public CustomTextWatcher(long delay) {
        this.delay = delay;
        enable();
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public void disable() {
        activated = false;
    }

    public void enable() {
        // to avoid notification of initial change
        activated = false;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                activated = true;
            }
        }, 1000);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus && textChanged && activated) {
            handler.removeCallbacks(timeOut);
            onFocusLostOrTimeOut(text);
            textChanged = false;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        text = s.toString();
        if (activated) {
            textChanged = true;
            handler.removeCallbacks(timeOut);
            handler.postDelayed(timeOut, delay);
        }
    }

    public abstract void onFocusLostOrTimeOut(String text);
}
