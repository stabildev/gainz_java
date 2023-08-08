package com.mycompany.gains.Activities.WorkoutEditor;

import android.os.Handler;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Created by Klee on 07.11.2015.
 */
public abstract class RowDragListener implements View.OnTouchListener, View.OnDragListener {
    boolean isLongClicked = false;
    boolean isMoving = false;
    int y0, y1;
    int threshold;
    Handler handler = new Handler();
    Runnable longPressTimeOut = new Runnable() {
        @Override
        public void run() {
            if (!isMoving) {
                isLongClicked = true;
            }
        }
    };

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // initialize variables
                isLongClicked = false;
                isMoving = false;
                y0 = (int) event.getY();
                y1 = y0;

                // start long press timeout
                handler.postDelayed(longPressTimeOut, (long) (ViewConfiguration.getLongPressTimeout()));

                break;

            case MotionEvent.ACTION_MOVE:
                // update position (movement distance)
                y1 = (int) event.getY();

                // if item is pressed long enough, not moving yet and dragged beyond the threshold,
                // initiate item drag
                if (isLongClicked && !isMoving && Math.abs(y1 - y0) > threshold) {
                    isMoving = true;
                    onRequestDrag(view);
                }
                break;

            case MotionEvent.ACTION_UP:
                // stop long press timeout
                handler.removeCallbacks(longPressTimeOut);
        }
        return false;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        View d = (View) event.getLocalState();
        int action = event.getAction();

        if (action == DragEvent.ACTION_DRAG_STARTED) {
            onDragStarted(d);

            // return true to receive future events
            return canDropOver(d, v);
        } else if (action == DragEvent.ACTION_DRAG_ENDED) {
            // notify of drag end
            onDragEnded(d);
        }

        // indicate dropzone on drop targets and switch places
        if (action == DragEvent.ACTION_DRAG_LOCATION) {
            if (!canDropOver(d, v)) return false;

            onHover(d, v, event.getX(), event.getY());
        }

        if (action == DragEvent.ACTION_DROP) {
            if (!canDropOver(d,v)) return false;
            onDrop(d, v);
        }

        // reset drop zone state if dragView exits v or drag ends
        if (action == DragEvent.ACTION_DRAG_EXITED)
            onExit(d, v);

        return false;
    }

    // called when a drag should be initiated
    public abstract void onRequestDrag(View view);

    // called when a drag HAS BEEN initiated
    public abstract void onDragStarted(View dragged);

    // indicates whether the dragged view can be dropped over the target
    public abstract boolean canDropOver(View dragged, View target);

    // called after a drag has ended
    public abstract void onDragEnded(View dragged);

    // called when the dragged view hovers the target
    public abstract void onHover(View dragged, View target, float x, float y);

    // called after a drop event
    public abstract void onDrop(View dragged, View target);

    // called when the dragged view exits the target
    public abstract void onExit(View dragged, View target);
}
