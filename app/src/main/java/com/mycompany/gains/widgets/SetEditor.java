package com.mycompany.gains.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewFlipper;

import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.R;

import java.util.List;

/**
 * Created by Klee on 09.11.2015.
 */
public class SetEditor extends ViewFlipper {
    private Set mSet;

    private SetEditorView.SetEditorListener mListener;

    private Animation slideInLeft;
    private Animation slideInRight;
    private Animation slideOutLeft;
    private Animation slideOutRight;

    public SetEditor(Context context) {
        super(context);
        initialize(context, null, 0);
    }

    public SetEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        // load animations
        long duration = 100;
        slideInLeft = AnimationUtils.loadAnimation(getContext(), (android.R.anim.slide_in_left));
        slideInLeft.setDuration(duration);
        slideInRight = AnimationUtils.loadAnimation(getContext(), (R.anim.slide_in_right));
        slideInRight.setDuration(duration);
        slideOutLeft = AnimationUtils.loadAnimation(getContext(), (R.anim.slide_out_left));
        slideOutLeft.setDuration(duration);
        slideOutRight = AnimationUtils.loadAnimation(getContext(), (android.R.anim.slide_out_right));
        slideOutRight.setDuration(duration);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        // add listeners to SetEditorViews
        for (int i = 0; i < getChildCount(); i++) {
            SetEditorView v = (SetEditorView) getChildAt(i);
            v.setListener(new SetEditorView.SetEditorListener() {
                @Override
                public void onSetChanged(Set set) {
                    if (mListener != null)
                        mListener.onSetChanged(set);
                }
            });
        }
    }

    public void setSet(Set set, boolean animate) {
        if (mSet != null && mSet.equals(set)) {
            // just update
            update();
            return;
        }

        if (!animate || mSet == null) {
            mSet = set;

            // set set for foreground SetEditor
            ((SetEditorView) getCurrentView()).setSet(set);
        }
        else {
            // set set for background SetEditor
            for (int i = 0; i < getChildCount(); i++) {
                SetEditorView v = (SetEditorView) getChildAt(i);
                if (!v.equals(getCurrentView())) {
                    v.setSet(set);
                }
            }

            List<Set> allSets = set.getRow().getSuperset().getWorkout().getAllSets();
            int posCurrent = allSets.indexOf(mSet);
            int posNext = allSets.indexOf(set);
            if (posCurrent == -1 || posNext == -1) // sets not in workout
                return;

            mSet = set;

            // adjust animation direction
            setInAnimation(
                    posNext > posCurrent ? slideInRight : slideInLeft
            );

            setOutAnimation(
                    posNext > posCurrent ? slideOutLeft : slideOutRight
            );

            showNext();
        }
    }

    public Set getSet() {
        return mSet;
    }

    public void update() {
        ((SetEditorView) getCurrentView()).update();
    }

    public void setListener(SetEditorView.SetEditorListener listener) {
        mListener = listener;
    }
}
