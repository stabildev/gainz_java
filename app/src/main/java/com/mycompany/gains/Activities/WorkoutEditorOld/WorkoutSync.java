package com.mycompany.gains.Activities.WorkoutEditorOld;

import android.content.Context;

import com.mycompany.gains.Activities.WorkoutEditor.Stopwatch;
import com.mycompany.gains.Data.Model.Set;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Klee on 19.10.2015.
 */
public class WorkoutSync {
    static WorkoutSync mInstance;
    Context mContext;
    Set mCurrentSet;
    Set mSetBeforeCountdown;
    Stopwatch mStopwatch;
    List<SyncListener> mListener;

    private WorkoutSync(Context context) {
        mContext = context;
        mListener = new ArrayList<>();
    }

    public static WorkoutSync getInstance(Context context) {
        if (mInstance == null)
            mInstance = new WorkoutSync(context.getApplicationContext());
        return mInstance;
    }

    public void updateSetBeforeCountdown() {
        mSetBeforeCountdown = mCurrentSet;
    }

    public Set getSetBeforeCountdown() {
        return mSetBeforeCountdown;
    }

    public void setCurrentSet(Set set) {
        // return if both the current set and the new set are equal or null
        if (set == null && mCurrentSet == null || (set != null && set.equals(mCurrentSet)) || (mCurrentSet != null && mCurrentSet.equals(set)))
            return;
        mCurrentSet = set;
        for (SyncListener listener : mListener)
            listener.onCurrentSetChanged(set);
//        String logText = "onCurrentSetChanged("+(set != null ? set.getCoordinates() : "null")+")";
//        Toast.makeText(mContext, logText,Toast.LENGTH_SHORT).show();
//        Log.i("Sync", logText);
    }

    public void setStopwatch(Stopwatch stopwatch) {
        mStopwatch = stopwatch;
    }

    public Stopwatch getStopwatch() {
        return mStopwatch;
    }

    public void notifySetOrderChanged() {
        // test if set was moved
        for (SyncListener listener : mListener)
            listener.onSetOrderChanged();
//        String logText = "onSetOrderChanged()";
//        Toast.makeText(mContext, logText,Toast.LENGTH_SHORT).show();
//        Log.i("Sync", logText);
    }

    public void notifyCurrentSetModified() {
        for (SyncListener listener : mListener)
            listener.onCurrentSetModified();
//        String logText = "onCurrentSetModified()";
//        Toast.makeText(mContext, logText,Toast.LENGTH_SHORT).show();
//        Log.i("Sync", logText);
    }

    public Set getCurrentSet() {
        return mCurrentSet;
    }

    public void addSyncListener(SyncListener listener) {
        mListener.add(listener);
    }

    public interface SyncListener {
        void onCurrentSetChanged(Set set);
        void onSetOrderChanged();
        void onCurrentSetModified();
    }
}
