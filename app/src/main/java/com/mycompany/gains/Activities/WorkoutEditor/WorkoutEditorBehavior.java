package com.mycompany.gains.Activities.WorkoutEditor;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mycompany.gains.R;

/**
 * Created by Klee on 07.09.2015.
 * inspired by baselab
 */
public class WorkoutEditorBehavior extends AppBarLayout.ScrollingViewBehavior {
    public WorkoutEditorBehavior(Context context, AttributeSet attrs) {}

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        Log.i("BEHAV", "layoutDependsOn = " + (dependency instanceof CardView && dependency.getId() == R.id.set_editor_card));
        return dependency instanceof CardView && dependency.getId() == R.id.set_editor_card
                || super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, final View child, final View dependency) {
        if (dependency instanceof CardView) {
            boolean visible = dependency.getVisibility() != View.GONE;
            /* TODO scroll to bottom
            final NestedScrollView sv = (NestedScrollView) child;
            boolean scrollToBottom = sv.getChildAt(sv.getChildCount()-1).getBottom()
                    - sv.getHeight() - sv.getScrollY() == 0;*/
            int cm = parent.getResources().getDimensionPixelSize(R.dimen.content_margin);
            child.setPadding(cm, cm, cm, cm + (visible ? dependency.getHeight() : 0));
            /*if (scrollToBottom && visible) sv.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sv.scrollTo(0, sv.getBottom());
                }
            }, 500);*/
        }
            return super.onDependentViewChanged(parent, child, dependency);
    }
}
