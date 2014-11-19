package com.nyasama.util;

import android.view.View;
import android.widget.Toast;

import com.nyasama.ThisApp;

/**
 * Created by oxyflour on 2014/11/13.
 *
 */
public class Helper {
    public static void toast(String text) {
        if (ThisApp.context != null)
            Toast.makeText(ThisApp.context, text, Toast.LENGTH_SHORT).show();
    }
    public static void toast(int stringId) {
        if (ThisApp.context != null)
            toast(ThisApp.context.getString(stringId));
    }
    public static void updateVisibility(View view, boolean show) {
        if (view != null)
            view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
