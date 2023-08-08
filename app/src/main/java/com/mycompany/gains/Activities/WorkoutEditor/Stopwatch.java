package com.mycompany.gains.Activities.WorkoutEditor;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Klee on 19.10.2015.
 */
public class Stopwatch implements Runnable {
    Handler handler = new Handler();
    long timeAtStart;
    long initialDuration;
    long duration;
    List<StopwatchListener> listeners = new ArrayList<>();

/*    public Stopwatch(Handler handler) {
        this.handler = handler;
    }*/

    public void init() {
        init(0);
    }

    public void init(long initialDuration) {
        timeAtStart = System.currentTimeMillis();
        this.initialDuration = initialDuration;
    }

    public void reset() {
        duration = 0;
        init(0);
    }

    public void start() {
        Log.i("Stopwatch", "START");
        handler.post(this);
    }

    public void stop() {
        handler.removeCallbacks(this);
        for (StopwatchListener listener : listeners)
            listener.onStop();
    }

    @Override
    public void run() {
        handler.removeCallbacks(this);
        duration = System.currentTimeMillis() - timeAtStart + initialDuration;
        for (StopwatchListener listener : listeners)
            listener.onTick(duration);
        handler.postDelayed(this, 100);
    }

    public void addStopwatchListener(StopwatchListener listener) {
        listeners.add(listener);
        Log.i("Stopwatch", "nListeners: " + listeners.size());
    }

    public interface StopwatchListener {
        void onTick(long duration);
        void onStop();
    }
}
