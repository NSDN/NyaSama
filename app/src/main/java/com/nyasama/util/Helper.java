package com.nyasama.util;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.nyasama.ThisApp;

/**
 * Created by oxyflour on 2014/11/13.
 *
 */
public class Helper {
    public static void toast(String text) {
        if (ThisApp.getContext() != null)
            Toast.makeText(ThisApp.getContext(), text, Toast.LENGTH_SHORT).show();
    }
    public static void toast(int stringId) {
        toast(ThisApp.getContext().getString(stringId));
    }
    public static void updateVisibility(View view, boolean show) {
        if (view != null)
            view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
