package com.nyasama.util;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

/**
 * Created by oxyflour on 2014/11/13.
 *
 */
public class Helper {
    public static void toast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
    public static void toast(Context context, int stringId) {
        toast(context, context.getString(stringId));
    }
    public static void updateVisibility(View view, boolean show) {
        if (view != null)
            view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}