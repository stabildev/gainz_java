package com.mycompany.gains.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mycompany.gains.Data.Model.Set;
import com.mycompany.gains.Stuff.Misc;
import com.mycompany.gains.R;


/**
 * Created by Klee on 07.09.2015.
 */
public class TimerView extends LinearLayout {
    ImageButton button;
    TextView text;

    @DrawableRes int startDrawable;
    @DrawableRes int timerDrawable;
    @DrawableRes int checkDrawable;
    @DrawableRes int skipDrawable;

    boolean useTimer = true;
    boolean skip = false;
    boolean started = false;
    boolean counting = false;
    boolean dragging = false;

    int displayHeight;
    int max = 300;
    int increment = 15;
    int time;

    private Set mSet;

    TimerListener listener;


    public TimerView(Context context) {
        super(context);
        initializeViews(context);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public TimerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    private void initializeViews(Context context) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.timer_view, this);

        setOrientation(HORIZONTAL);
        int edgeMargin = getResources().getDimensionPixelSize(R.dimen.edge_margin);
        setPadding(edgeMargin, edgeMargin, edgeMargin, edgeMargin);
        setClipToPadding(false);
        setClipChildren(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // initialize views
        button = (ImageButton) this.findViewById(R.id.timer_button);
        text = (TextView) this.findViewById(R.id.timer_text);

        // get screen height
        displayHeight = Misc.getDisplayHeight(getContext());
        displayHeight -= 2 * getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);

        // get drawable res ids
        startDrawable = R.drawable.ic_play_arrow_white_48dp;
        timerDrawable = R.drawable.ic_timer_white_24dp;
        checkDrawable = R.drawable.ic_check_white_48dp;
        skipDrawable = R.drawable.ic_skip_next_white_48dp;

        updateIcon();

        final GestureDetector detector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll (MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // if workout is started, workout timing enabled and timer not being autoskipped
                if (started && useTimer && !skip) {
                    if (!dragging)
                        cancel();
                    dragging = true;
                    float distance = e1.getRawY() - e2.getRawY();

                    // move timer up
                    TimerView.this.setTranslationY(-distance);

                    updateIcon();

                    float fraction = distance/displayHeight;
                    int rest = fraction > 0 ? (int) (fraction * max) : 0;
                    rest = Misc.roundTime(rest, increment); // round to multiples of 15 seconds
                    rest = rest < max ? rest : max; // limit rest to max

                    setTime(rest, true);
                }
                return true;
            }
            @Override
            public boolean onSingleTapUp (MotionEvent e) {
                if (!started) {
                    if (listener != null) listener.onClick();
                }
                else {
                    if (counting) {
                        skip(true);
                    }
                    else {
                        if (useTimer && !skip && time > 0)
                            start();
                        else
                            skip(false);
                    }
                }
                return true;
            }
        });

        button.setOnTouchListener(new View.OnTouchListener() {
            final float translationYBefore = TimerView.this.getTranslationY();

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // released after drag
                if (!detector.onTouchEvent(event) && event.getActionMasked() == MotionEvent.ACTION_UP && !skip && useTimer) {
                    dragging = false;

                    // start countdown
                    //countdown.start(time);
                    start();

                    // animate return to original position
                    ValueAnimator animator = ValueAnimator.ofFloat(
                            TimerView.this.getTranslationY(),
                            translationYBefore);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            TimerView.this.setTranslationY((float) animation.getAnimatedValue());
                        }
                    });
                    animator.setInterpolator(new OvershootInterpolator(1.0f));
                    animator.setDuration(150).start();
                }

                return false;
            }
        });
    }

    private void updateIcon() {
        button.setImageResource(
                !started ? startDrawable
                        : !useTimer || skip || time <= 0 ? checkDrawable
                        : counting && !dragging ? skipDrawable
                        : timerDrawable
        );
        text.setTextColor(started && !skip ? Color.WHITE : ContextCompat.getColor(getContext(), R.color.white_disabled));
        text.setVisibility(started && useTimer ? VISIBLE : GONE);
    }

    public void setTimerListener(TimerListener listener) {
        this.listener = listener;
    }

    public void setTime(int seconds, boolean updateLabel) {
        time = seconds;
        if (updateLabel)
            text.setText(Misc.formatTime(seconds));
    }

    public void setMax(int seconds) {
        max = seconds;
    }

    public void setIncrement(int seconds) {
        increment = seconds;
    }

    public void useTimer(boolean useTimer) {
        this.useTimer = useTimer;
        text.setVisibility(useTimer ? View.VISIBLE : View.GONE);
        updateIcon();
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
        updateIcon();
    }

    public void setStarted(boolean started) {
        this.started = started;
        updateIcon();
    }

    @Override
    public void setEnabled(boolean enabled) {
        setEnabled(enabled, false);
    }

    public void setEnabled(boolean enabled, boolean animate) {
        float alpha = enabled ? 1f : .2f;
        if (animate)
            Misc.animateAlpha(this, alpha);
        else
            this.setAlpha(alpha);
        button.setClickable(enabled);
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isCounting() {
        return counting;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setCounting(boolean counting) {
        this.counting = counting;
        // update:
        setSet(mSet);
    }

    public void update() {
        setSet(mSet);
    }

    public void setSet(Set set) {
        mSet = set;
        setTime(getRest(), !dragging && !counting);
        updateIcon();
    }

    public Set getSet() {
        return mSet;
    }

    public int getRest() {
        if (mSet != null)
            return mSet.getRow().getRest();
        else
            return 0;
    }

    public void start() {
        if (listener != null) listener.onStartCountdown(time);
        setCounting(true);
    }

    private void skip(boolean isTimerEnabled) {
        setSkip(false);
        if (listener != null)
            listener.onSkipCountdown(isTimerEnabled);
    }

    public void cancel() {
        if (listener != null) listener.onCancelCountdown();
    }

    public interface TimerListener {
        void onClick();
        void onStartCountdown(int initial);
        void onSkipCountdown(boolean isTimerEnabled);
        void onCancelCountdown();
    }
}
