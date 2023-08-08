package com.mycompany.gains.Activities.WorkoutEditor;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.mycompany.gains.Arguments;

/**
 * Created by Klee on 23.09.2015.
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Alarm", "Received alarm at " + CountdownNotifyService.formatTime(System.currentTimeMillis()));

        Intent updateCountdown = new Intent(context, CountdownNotifyService.class);
        updateCountdown.putExtra(Arguments.ARG_ACTION, Arguments.ACTION_WAKE_UP);
        startWakefulService(context, updateCountdown);
    }
}
