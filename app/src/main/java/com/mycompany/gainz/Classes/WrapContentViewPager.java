package com.mycompany.gainz.Classes;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class WrapContentViewPager extends ViewPager
{
    public WrapContentViewPager (Context context)
    {
        super(context);
    }

    public WrapContentViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = 0;
        for(int i = 0; i < getChildCount(); i++)
        {
            View child = getChildAt(i);
            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

            int h = child.getMeasuredHeight();
            if(h > height) height = h;
        }

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

/*    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            super.onTouchEvent(ev);
            return true;
        } else {
            return false;
        }
    }*/

/*    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        //Ignore scroll events.
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                onTouchEvent(ev);
                return true;
            default:
                return false;
        }
    }*/
}