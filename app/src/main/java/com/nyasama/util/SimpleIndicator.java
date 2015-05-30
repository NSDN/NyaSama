package com.nyasama.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by oxyflour on 2015/5/29.
 *
 */
public class SimpleIndicator extends LinearLayout {
    public SimpleIndicator(Context context) {
        super(context);
    }
    public SimpleIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public void createIndicators(int total, int resId) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        removeAllViews();
        for (int i = 0, n = total; i < n; i ++)
            inflater.inflate(resId, this);
    }
    public void setActive(int position) {
        for (int i = 0, n = getChildCount(); i < n; i ++) {
            View view = getChildAt(i);
            if (view != null)
                view.getBackground().mutate().setAlpha(i == position ? 255 : 80);
        }
    }
}
