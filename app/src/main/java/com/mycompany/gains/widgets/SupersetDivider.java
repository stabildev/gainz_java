package com.mycompany.gains.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.mycompany.gains.R;

/**
 * Created by henri on 08.11.2015.
 */
public class SupersetDivider extends View {

    public SupersetDivider(Context context) {
        super(context);
        init(context);
    }

    public SupersetDivider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SupersetDivider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setBackgroundResource(R.color.divider_cards);
    }
}
