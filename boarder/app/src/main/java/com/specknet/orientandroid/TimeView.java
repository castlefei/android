package com.specknet.orientandroid;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class TimeView extends android.support.v7.widget.AppCompatTextView {

    private boolean started;
    private long start_point;

    public TimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        started = false;
        setText("00:00");
    }

    public void setStarted(boolean value){
        started = value;
        if (started){
            start_point = System.currentTimeMillis() / 1000;
        }
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        if (started){
            long difference = System.currentTimeMillis() / 1000 - start_point;
            int min = (int) (difference / 60);
            String min_s = min + "";
            if (min < 10){
                min_s = "0" + min_s;
            }
            int sec = (int) (difference % 60);
            String sec_s = sec + "";
            if (sec < 10){
                sec_s = "0" + sec_s;
            }
            setText(min_s + ":" + sec_s );
        }
    }
}
