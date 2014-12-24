package com.nyasama.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.Toast;

import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.activity.LoginActivity;
import com.nyasama.activity.SettingActivity;
import com.nyasama.activity.UserProfileActivity;

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
    public static void updateVisibility(View view, int id, boolean show) {
        if (view != null)
            updateVisibility(view.findViewById(id), show);
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

    public static boolean handleOption(Activity activity, int id) {
        if (id == android.R.id.home) {
            activity.finish();
            return true;
        }
        else if (id == R.id.action_settings) {
            activity.startActivity(new Intent(activity, SettingActivity.class));
            return true;
        }
        else if (id == R.id.action_my_profile) {
            if (Discuz.sHasLogined)
                activity.startActivity(new Intent(activity, UserProfileActivity.class));
            else activity.startActivityForResult(new Intent(activity, LoginActivity.class),
                    LoginActivity.REQUEST_CODE_LOGIN);
            return true;
        }
        return false;
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

    public static abstract class OnSpanClickListener {
        public abstract boolean onClick(View widget, String data);
    }
    public static CharSequence setSpanClickListener(CharSequence text, Class cls, final OnSpanClickListener onClickListener) {
        if (!(text instanceof Spannable))
            return text;

        Spannable spannable = (Spannable) text;
        Object[] spans = spannable.getSpans(0, spannable.length(), cls);
        if (spans != null && spans.length > 0) {
            for (Object span : spans) {
                int start = spannable.getSpanStart(span);
                int end = spannable.getSpanEnd(span);
                int flag = spannable.getSpanFlags(span);
                if (span instanceof URLSpan) {
                    URLSpan urlSpan = (URLSpan) span;
                    spannable.removeSpan(urlSpan);
                    spannable.setSpan(new URLSpan(urlSpan.getURL()) {
                        @Override
                        public void onClick(View widget) {
                            if (onClickListener.onClick(widget, getURL()))
                                return;
                            try { super.onClick(widget); }
                            catch (Throwable e) { e.printStackTrace(); }
                        }
                    }, start, end, flag);
                }
                else {
                    final String data = span instanceof ImageSpan ? ((ImageSpan) span).getSource() : null;
                    spannable.setSpan(new android.text.style.ClickableSpan() {
                        @Override
                        public void onClick(View view) {
                            onClickListener.onClick(view, data);
                        }
                    }, start, end, flag);
                }
            }
        }

        return text;
    }
}
