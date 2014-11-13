package com.nyasama.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * Created by oxyflour on 2014/11/13.
 * REF: http://www.jb51.net/article/39023.htm
 */
public class FullGridView extends GridView {
    public FullGridView(Context context) {
        super(context);
    }
    public FullGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSepc = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSepc);
    }
}
