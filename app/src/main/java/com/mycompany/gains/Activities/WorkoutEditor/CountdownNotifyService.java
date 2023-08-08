package com.mycompany.gains.Activities.WorkoutEditor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.mycompany.gains.R;
import com.mycompany.gains.Stuff.Misc;

import static com.mycompany.gains.Arguments.*;

/**
 * Created by Klee on 21.09.2015.
 */
public class CountdownNotifyService extends Service {

    public static final String COUNTDOWN_BR = "com.mycompany.gains.countdown_br";
    Intent broadcastIntent = new Intent(COUNTDOWN_BR);

    public static final String INTENT_ACTION_ALARM = "com.mycompany.gains.intent_action_alarm";

    Countdown timer;
    Handler handler = new Handler();
    boolean notify = false;

    NotificationManager mNotifyMgr;
    NotificationCompat.Builder mNotifyBuilder;

    BroadcastReceiver screenWatcher;
    BroadcastReceiver alarm;
    AlarmManager am;
    PendingIntent pi;
    PowerManager.WakeLock wl;

    Uri alarmSound;
    Uri beepSound;
    boolean vibrate;
    int beepAfter;
    int progress;

    //int secondsLeft;
    long timeBeforeSleep;

    private static final int NOTIFICATION_ONGOING = 1;

    class Countdown implements Runnable {
        public int secondsLeft;
        public int initial;
        public boolean finished;

        public void init(int initial) {
            this.initial = initial;
            this.secondsLeft = initial;
            this.finished = false;
            handler.post(this);
        }
        @Override
        public void run() {
            Log.i("Alarm", "[COUNTDOWN] " + secondsLeft);
            handler.removeCallbacks(this);
            if (secondsLeft > 0) {
                broadcastIntent.putExtra(ARG_ACTION, ACTION_TICK);
                broadcastIntent.putExtra(ARG_TIME, secondsLeft);
                broadcastIntent.putExtra(ARG_INITIAL, initial);
                sendBroadcast(broadcastIntent);

                // issue notifications
                if (notify) {
                    mNotifyBuilder.setContentTitle(
                            getString(R.string.notification_rest_remaining) + Misc.formatTime(secondsLeft))
                            .setProgress(initial, secondsLeft, false);

                    if (secondsLeft <= beepAfter) {
                        if (!wl.isHeld()) wl.acquire();
                        mNotifyBuilder.setSound(beepSound);
                        if (vibrate) mNotifyBuilder.setVibrate(new long[]{0, 100});

                        mNotifyBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
                    }
                    mNotifyBuilder.setOngoing(true);
                    mNotifyMgr.notify(NOTIFICATION_ONGOING, mNotifyBuilder.build());
                }

                secondsLeft--;
                handler.postDelayed(this, 1000);
            }
            else {
                broadcastIntent.putExtra(ARG_ACTION, ACTION_FINISH);
                sendBroadcast(broadcastIntent);

                // issue notification
                if (notify) {
                    mNotifyBuilder.setContentTitle(getString(R.string.notification_rest_is_up))
                            .setProgress(100, progress, false)
                            .setSound(alarmSound)
                            .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);

                    mNotifyBuilder.setPriority(NotificationCompat.PRIORITY_MAX);

                    Log.i("Alarm", "Issue notification at " + formatTime(System.currentTimeMillis()));
                    mNotifyMgr.notify(NOTIFICATION_ONGOING, mNotifyBuilder.build());
                }
                finished = true;

                // stop service
                stopSelf();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (wl.isHeld()) wl.release();
        am.cancel(pi);
        unregisterReceiver(screenWatcher);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return super.onStartCommand(null, flags, startId);
        }
        switch (intent.getStringExtra(ARG_ACTION)) {
            case ACTION_WAKE_UP:
                Log.i("Alarm", "ACTION_WAKE_UP");

                if (!wl.isHeld()) wl.acquire();

                Log.i("Alarm", "wl.isHeld() == " + wl.isHeld());

                AlarmReceiver.completeWakefulIntent(intent);

                if (timer != null && !timer.finished) {
                    updateTime();
                    handler.post(timer);
                }
                break;
            case ACTION_START_NOTIFY:
                progress = intent.getIntExtra(ARG_PROGRESS, 0);
                mNotifyBuilder.setContentText(
                        intent.getStringExtra(ARG_NOTIFY_TEXT)
                );
                // create ongoing notification if not counting
                if (timer == null) {
                    mNotifyBuilder.setProgress(100, progress, false)
                            .setOngoing(true)
                            .setContentTitle(getString(R.string.notification_back_to_workout));
                    mNotifyMgr.notify(NOTIFICATION_ONGOING, mNotifyBuilder.build());
                    stopSelf();
                }
                notify = true;
                break;
            case ACTION_STOP_NOTIFY:
                mNotifyMgr.cancelAll();
                if (timer == null)
                    stopSelf();
                notify = false;
                break;
            case ACTION_CANCEL:
                Log.i("LEL", "[CountdownNotifyService] ACTION_CANCEL");

                if (timer != null)
                    handler.removeCallbacks(timer);
                stopSelf();
                break;
            case ACTION_START:
                Log.i("LEL", "[CountdownNotifyService] ACTION_START");

                if (timer != null)
                    handler.removeCallbacks(timer);

                final int initial = intent.getIntExtra(ARG_INITIAL, 0);

                broadcastIntent.putExtra(ARG_ACTION, ACTION_START);
                broadcastIntent.putExtra(ARG_INITIAL, initial);

                timer = new Countdown();
                timer.init(initial);
                Log.i("LEL", "timer started");
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateTime() {
        Log.i("Alarm", "updateTime()");
        Log.i("Alarm", "secondsLeft before = " + timer.secondsLeft);
        int secondsPassed = (int) (float) (System.currentTimeMillis() - timeBeforeSleep) / 1000;
        Log.i("Alarm", "secondsPassed = " + secondsPassed);
        timer.secondsLeft -= secondsPassed;
        if (timer.secondsLeft < 0) timer.secondsLeft = 0;
        Log.i("Alarm", "secondsLeft after = " + timer.secondsLeft);
    }

    public void initialize() {
        // broadcastreceiver for the alarm
        alarm = new AlarmReceiver();

        am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        final Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        alarmIntent.setAction(INTENT_ACTION_ALARM);
        pi = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);

        PowerManager pm = (PowerManager) this
                .getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "Breeze WakeLock");

        // register broadcastreceiver to find out when screen goes off
        screenWatcher = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case Intent.ACTION_SCREEN_OFF:
                        Log.i("Alarm", "SCREEN_OFF");
                        if (timer != null && !timer.finished) {
                            if (timer.secondsLeft <= beepAfter && !wl.isHeld())
                                wl.acquire();
                            else {
                                timeBeforeSleep = System.currentTimeMillis();
                                handler.removeCallbacks(timer);
                                Log.i("LEL", "handler.removeCallbacks(timer)");
                                Log.i("Alarm", "secondsLeft = " + timer.secondsLeft + ", beepAfter = " + beepAfter);
                                long delay = (timer.secondsLeft - beepAfter) * 1000 - 1500; // 1.5s for safety
                                Log.i("Alarm", "Alarm in " + delay + " ms at " + formatTime((timeBeforeSleep + delay)));
                                if (Build.VERSION.SDK_INT >= 19)
                                    am.setExact(AlarmManager.RTC_WAKEUP,
                                            timeBeforeSleep + delay, pi);
                                else
                                    am.set(AlarmManager.RTC_WAKEUP,
                                            timeBeforeSleep + delay, pi);
                            }
                        }
                        return;
                    case Intent.ACTION_SCREEN_ON:
                        Log.i("Alarm", "SCREEN_ON");
                        Log.i("Alarm", "wl.isHeld() == " + wl.isHeld());
                        if (timer != null && !timer.finished) {
                            am.cancel(pi);
                            updateTime();
                            Log.i("Alarm", "timer.secondsLeft = " + timer.secondsLeft);
                            handler.post(timer);
                        }
                        //return;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        registerReceiver(screenWatcher, filter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        alarmSound = Uri.parse(prefs.getString(getString(R.string.pref_key_alarm_sound), ""));
        alarmSound = alarmSound.toString().equals("") ? null : alarmSound; // allow for silent
        beepSound = Uri.parse(prefs.getString(getString(R.string.pref_key_acustic_countdown), ""));
        beepSound = beepSound.toString().equals("") ? null : beepSound; // allow for silent

        vibrate = prefs.getBoolean(getString(R.string.pref_key_vibrate_countdown), true);
        beepAfter = Integer.parseInt(prefs.getString(getString(R.string.pref_key_beep), "3"));

        // Gets an instance of the NotificationManager service
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // create the notification builder
        Intent intent = new Intent(this, WorkoutEditor.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotifyBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_notify)
                .setContentIntent(resultPendingIntent);

        mNotifyBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
    }

    public static String formatTime(long time) {
        return (String) android.text.format.DateFormat.format("hh:mm:ss", time);
        //return  String.format("%02d:%02d:%02d", time / 3600, (time % 3600) / 60, (time % 60));
    }
}
