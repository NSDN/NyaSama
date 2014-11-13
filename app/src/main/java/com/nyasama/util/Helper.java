package com.nyasama.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by oxyflour on 2014/11/13.
 *
 */
public class Helper {
    public static void toast(Context context, int stringId) {
        Toast.makeText(context, context.getString(stringId), Toast.LENGTH_SHORT).show();
    }
}
