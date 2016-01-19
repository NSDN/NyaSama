package com.nyasama.util;

import android.content.Context;
import android.text.Selection;
import android.text.Spannable;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by oxyflour on 2015/6/15.
 * ref: https://code.google.com/p/android/issues/detail?can=2&start=0&num=100&q=&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars&groupby=&sort=&id=137509
 */
public class SafeHtmlText extends TextView {
    public SafeHtmlText(Context context) {
        super(context);
    }

    public SafeHtmlText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SafeHtmlText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (selStart == -1 || selEnd == -1) {
            // @hack : https://code.google.com/p/android/issues/detail?id=137509
            // also see #96
        } else {
            super.onSelectionChanged(selStart, selEnd);
        }
    }
}
