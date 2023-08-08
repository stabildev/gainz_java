package com.mycompany.gains.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.TextView;

import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.R;


public class SetView extends TextView {
    boolean done = false;
    int strokeWidth;
    int colorUndone;
    int colorDone;
    int colorSelected;

    Set mSet;

    enum State {
        SELECTED,
        UNDONE,
        DONE
    }

    public SetView(Context context) {
        super(context);
        init();
    }

    public SetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokeWidth = getResources().getDimensionPixelSize(R.dimen.set_view_stroke_width);
        colorUndone = ContextCompat.getColor(getContext(), R.color.set_view_undone);
        colorDone = ContextCompat.getColor(getContext(), R.color.icons);
        colorSelected = ContextCompat.getColor(getContext(), R.color.accent);
    }

    public void changeStateColor(State fromState, State toState, boolean animate) {
        final GradientDrawable bg = (GradientDrawable) getBackground().mutate();
        final int fromBgColor;
        final int toBgColor;
        final int fromTextColor;
        final int toTextColor;
        final int fromStrokeColor;
        final int toStrokeColor;

        switch (fromState) {
            case UNDONE:
                fromBgColor = Color.TRANSPARENT;
                fromTextColor = colorUndone;
                fromStrokeColor = colorUndone;
                break;
            case DONE:
                fromBgColor = Color.TRANSPARENT;
                fromTextColor = colorDone;
                fromStrokeColor = colorUndone;
                break;
            case SELECTED:
                fromBgColor = colorSelected;
                fromTextColor = Color.WHITE;
                fromStrokeColor = colorSelected;
                break;
            default:
                return;
        }

        switch (toState) {
            case UNDONE:
                toBgColor = Color.TRANSPARENT;
                toTextColor = colorUndone;
                toStrokeColor = colorUndone;
                break;
            case DONE:
                toBgColor = Color.TRANSPARENT;
                toTextColor = colorDone;
                toStrokeColor = colorDone;
                break;
            case SELECTED:
                toBgColor = colorSelected;
                toTextColor = Color.WHITE;
                toStrokeColor = colorSelected;
                break;
            default:
                return;
        }
        if (animate) {
            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float amount = (float) animation.getAnimatedValue();
                    int bgColor = blendColors(toBgColor, fromBgColor, amount);
                    int textColor = blendColors(toTextColor, fromTextColor, amount);
                    int strokeColor = blendColors(toStrokeColor, fromStrokeColor, amount);

                    bg.setColor(bgColor);
                    setTextColor(textColor);
                    bg.setStroke(strokeWidth, strokeColor);

                }
            });
            animator.setDuration(200).start();
        }
        else {
            bg.setStroke(strokeWidth, toTextColor);
            setTextColor(toTextColor);
            bg.setColor(toBgColor);
        }
    }

    public void setDone(boolean done) {
        if (this.done == done) return;

        this.done = done;

        // wait until deselection
        if (!isSelected()) {
            changeStateColor(
                    done ? State.UNDONE : State.DONE,
                    done ? State.DONE : State.UNDONE,
                    false);
        }
    }

    @Override
    public void setSelected(boolean selected) {
        setSelected(selected, true);
    }

    public void setSelected(boolean selected, boolean animate) {
        // if no change is required, return
        if (selected == isSelected()) return;

        super.setSelected(selected);

        changeStateColor(
                !selected ? State.SELECTED : done ? State.DONE : State.UNDONE,
                selected ? State.SELECTED : done ? State.DONE : State.UNDONE,
                animate);
    }

    public static int blendColors( int color1, int color2, float amount )
    {
        final byte ALPHA_CHANNEL = 24;
        final byte RED_CHANNEL   = 16;
        final byte GREEN_CHANNEL =  8;
        final byte BLUE_CHANNEL  =  0;

        final float inverseAmount = 1.0f - amount;

        int a = ((int)(((float)(color1 >> ALPHA_CHANNEL & 0xff )*amount) +
                ((float)(color2 >> ALPHA_CHANNEL & 0xff )*inverseAmount))) & 0xff;
        int r = ((int)(((float)(color1 >> RED_CHANNEL & 0xff )*amount) +
                ((float)(color2 >> RED_CHANNEL & 0xff )*inverseAmount))) & 0xff;
        int g = ((int)(((float)(color1 >> GREEN_CHANNEL & 0xff )*amount) +
                ((float)(color2 >> GREEN_CHANNEL & 0xff )*inverseAmount))) & 0xff;
        int b = ((int)(((float)(color1 & 0xff )*amount) +
                ((float)(color2 & 0xff )*inverseAmount))) & 0xff;

        return a << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b << BLUE_CHANNEL;
    }

    public void setSet(Set set) {
        this.mSet = set;
        update();
    }

    public void update() {
        setText(getSetText(mSet));
        setDone(mSet.isDone());
    }

    public Set getSet() {
        return mSet;
    }

    public static String getSetText(Set set) {
        String text = "";
        if (set.getReps() > 0 ||  set.isDone())
            text += set.getReps();
        if (set.getWeight() > 0)
            text += " × " + set.getWeightFormatted(true);
        return text;
    }
}
