package com.mycompany.gains.Activities.WorkoutEditor;

import android.graphics.Rect;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

public abstract class HoldListener implements View.OnTouchListener, Runnable {

    private int interval;
    private boolean hold;

    private Handler handler = new Handler();
    private View thisView;

    public HoldListener(int interval) {
        this.interval = interval;
    }

    @Override
    public boolean onTouch(View view, MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                thisView = view;
                hold = false;
                handler.postDelayed(this, interval);
                break;
            case MotionEvent.ACTION_MOVE:
                Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                if (rect.contains(view.getLeft() + (int) ev.getX(), view.getTop() + (int) ev.getY()))
                    break;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(this);
                if (hold)
                    onRelease(view);
                break;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(this);
                if (!hold) {
                    onClick(view);
                }
                onRelease(view);
                break;
        }
        return false;
    }

    @Override
    public void run() {
        onClick(thisView);
        handler.postDelayed(this, interval);
        hold = true;
    }

    // callback methods
    public abstract void onClick(View v);
    public abstract void onRelease(View v);
}