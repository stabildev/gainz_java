package com.mycompany.gains.Stuff;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by Klee on 03.08.2015.
 */
public class Misc {
    public static void animateAlpha(View v, float alpha) {
        if (Build.VERSION.SDK_INT >= 12)
            v.animate().alpha(alpha);
        else
            v.setAlpha(alpha);
    }

    @SuppressWarnings( "deprecation" )
    public static void setBackground(View v, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 16)
            v.setBackground(drawable);
        else
            v.setBackgroundDrawable(drawable);
    }

    public static int roundTime(int time, int precision){
        return precision * Math.round((float) time/precision);
    }

    public static String formatTime(int seconds) {
        String secondsRest = ""+seconds%60;
        while (secondsRest.length() < 2) secondsRest = "0" + secondsRest;
        return ((int) (float) seconds/60) + ":" + secondsRest;
    }

    @SuppressWarnings( "deprecation" )
    public static int getDisplayHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            return size.y;
        } else
            return display.getHeight();
    }
}
