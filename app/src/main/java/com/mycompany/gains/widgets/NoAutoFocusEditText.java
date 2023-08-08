package com.mycompany.gains.widgets;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

public class NoAutoFocusEditText extends EditText {

    private OnImeBackListener mListener;

    public NoAutoFocusEditText(@Nullable Context context) {
        super(context);
        init();
    }

    public NoAutoFocusEditText(@Nullable Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NoAutoFocusEditText(@Nullable Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /*public EditTextBackEvent(@Nullable Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }*/

    public void init() {
        this.setFocusable(false);
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.setFocusable(true);
                v.setFocusableInTouchMode(true);
                return false;
            }
        });
    }

    public interface OnImeBackListener {
        void onImeBack(NoAutoFocusEditText ctrl);
    }

    public void setOnImeBackListener(OnImeBackListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (mListener != null)
                mListener.onImeBack(this);
            else {
                this.clearFocus();
                this.setFocusable(false);
                this.setFocusableInTouchMode(false);
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
