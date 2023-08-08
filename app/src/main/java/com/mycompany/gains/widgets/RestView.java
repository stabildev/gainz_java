package com.mycompany.gains.widgets;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mycompany.gains.R;
import com.mycompany.gains.Stuff.Misc;

/**
 * Created by henri on 05.11.2015.
 */
public class RestView extends LinearLayout {

    private TextView textView;
    private ImageView icTimer;
    private int rest;

    private int activatedColor;
    private int defaultColor;

    public RestView(Context context) {
        super(context);
        init(context, null);
    }

    public RestView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RestView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.rest_view, this);

        defaultColor = ContextCompat.getColor(context, R.color.disabled_text);
        activatedColor = ContextCompat.getColor(context, R.color.secondary_text);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // initialize views
        textView = (TextView) findViewById(R.id.time);
        textView.setText(Misc.formatTime(rest));
        icTimer = (ImageView) findViewById(R.id.ic_timer);
    }

    public void setRest(int rest) {
        this.rest = rest;
        if (textView != null)
            textView.setText(Misc.formatTime(rest));
    }

    public void setActivated(boolean activated) {
        int newColor = activated ? activatedColor : defaultColor;
        textView.setTextColor(newColor);
        icTimer.clearColorFilter();
        icTimer.setColorFilter(newColor);
    }
}
