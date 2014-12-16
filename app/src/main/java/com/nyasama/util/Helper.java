package com.nyasama.util;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Toast;

import com.nyasama.ThisApp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
    public static void disableDialog(AlertDialog dialog) {
        dialog.setCancelable(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
    }
    public static int toSafeInteger(String string, int defValue) {
        try {
            return Integer.parseInt(string);
        }
        catch (NumberFormatException e) {
            return defValue;
        }
    }
    public static String datelineToString(long time, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format == null ? "yyyy-MM-dd HH:mm:ss" : format);
        dateFormat.setTimeZone(TimeZone.getDefault());
        Date date = new Date();
        date.setTime(time * 1000);
        return dateFormat.format(date);
    }

    public static class Size {
        public int width;
        public int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public static Size getFittedSize(Size source, Size target, boolean coverTarget) {
        boolean tooWide = source.width * target.height > source.height * target.width;
        if ((tooWide && !coverTarget) || (!tooWide && coverTarget))
            target.height = source.height * target.width / source.width;
        else
            target.width = source.width * target.height / source.height;
        return target;
    }

    public static Bitmap getFittedBitmap(Bitmap bitmap, int width, int height, boolean coverTarget) {
        Size newSize = getFittedSize(
                new Size(bitmap.getWidth(), bitmap.getHeight()),
                new Size(width, height),
                coverTarget);
        return Bitmap.createScaledBitmap(bitmap, newSize.width, newSize.height, true);
    }
}
