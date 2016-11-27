package com.nyasama.util;

import android.support.v7.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.nyasama.ThisApp;

import org.json.JSONObject;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by oxyflour on 2014/11/13.
 *
 * 这是一个纯粹的工具类，本类提供了以下方法：
 * toast显示 String , int， （toast）;
 * 使调试用的文字消失   （updateVisibility） 用例见 DiscuzForumIndexFragment.java;
 * 字符转整数       （toSafeInteger）;
 * 字符转double      (toSafeDouble) ;
 * 使对话框的按钮失效(估计是在启动下载后用的吧)  (disableDialog);
 * 毫秒时间转规范时间      (datelineToString);
 * 删掉过长链表的队尾，以缩减至要求长度     (setListLength);
 * 检查Map中是否已有数据，没有则填入       (putIfNull);
 * 处理menu 中的选项,用例见MainActivity       (handleOption)  ;
 * size 类，getfittedsize 和 getfittedbitmap 都是用户 imageview 和 bitmap 尺寸不符时，调整 bitmap 大小的;
 * 具体调整方法见下面的注释
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
    public static void toast(String text, int gravity, double fx, double fy) {
        if (ThisApp.context != null) {
            Toast toast = Toast.makeText(ThisApp.context, text, Toast.LENGTH_SHORT);
            WindowManager manager =
                    (WindowManager)ThisApp.context.getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            toast.setGravity(gravity, (int)(size.x * fx), (int)(size.y * fy));
            toast.show();
        }
    }

    public static void updateVisibility(View view, boolean show) {
        if (view != null)
            view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    public static void updateVisibility(View view, int id, boolean show) {
        if (view != null)
            updateVisibility(view.findViewById(id), show);
    }

    public static void enableDialog(AlertDialog dialog, boolean enabled) {
        dialog.setCancelable(enabled);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(enabled);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(enabled);
    }

    public static int toSafeInteger(String string, int defValue) {
        if (string == null)
            return defValue;
        try {
            return Integer.parseInt(string);
        }
        catch (NumberFormatException e) {
            return defValue;
        }
    }
    public static double toSafeDouble(String string, double defValue) {
        if (string == null)
            return defValue;
        try {
            return Double.parseDouble(string);
        }
        catch (NumberFormatException e) {
            return defValue;
        }
    }

    public static String toSafeMD5(String input) {
        String output;
        try {
            byte[] buffer = MessageDigest.getInstance("MD5").digest(input.getBytes());
            output = String.format("%032x", new BigInteger(1, buffer));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    public static String datelineToString(long time, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format == null ? "yyyy-MM-dd HH:mm:ss" : format, Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
        Date date = new Date();
        date.setTime(time * 1000);
        return dateFormat.format(date);
    }

    public static JSONObject optJSONObject(JSONObject data, String... path) {
        for (String name : path) {
            if (data != null)
                data = data.optJSONObject(name);
            else
                return null;
        }
        return data;
    }

    public static void setListLength(List list, int size) {
        if (size < list.size())
            list.subList(size, list.size()).clear();
    }
//@SuppressWarnings("unchecked") 这句是说：用不着报告类型检查的warning 了
    @SuppressWarnings("unchecked")
    public static boolean putIfNull(Map map, Object key, Object value) {
        if (map != null && map.get(key) == null) {
            map.put(key, value);
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
/*
source.width * target.height > source.height * target.width  
=> source.width / source.height > target.width / target.height
这个条件判断是否过宽，

在182行的用例中，converTarget 为true,也就是说，进入 if 的判断条件为 !tooWide：
也就是：没有过宽，调整高度，如果过宽，调整宽度
*/
    
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
    
    /*
    这个方法给CharSequence 加上了点击事件
    调用时直接 Helper.OnSpanClickListener(){} 并加入 setSpanClickListener
    
    具体加法是，先去除CharSequence 上的装饰（span）,然后创建一个带onclick的 span，再装上去
    但是URLSpan 和 ImageSpan 去除 span 的方法不大一样
    URLSpan 用 removespan，ImageSpan 用getSource
    */
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
                        public void onClick(@NonNull View widget) {
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

    // REF: http://stackoverflow.com/questions/20067508/get-real-path-from-uri-android-kitkat-new-storage-access-framework
    // by bluebrain
    /*
    这个函数把看起来很干净漂亮的逻辑地址转化成真实地址
    
    新android 的 contentprovider 将整个android 机当做一个大数据库管理
    比如： android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI 也就是一个媒体.图片的外存文件表
    
    contentprovider 文档：http://developer.android.com/guide/topics/providers/content-providers.html
    */
    public static String getPathFromUri(Uri uri) {
         //首先通过Uri查到文件名
        ContentResolver resolver = ThisApp.context.getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor == null) {
            return uri.getPath();
        }
        else {
            cursor.moveToFirst();
            String document_id = cursor.getString(0);
            document_id = document_id.substring(document_id.lastIndexOf(":")+1);
            cursor.close();
            //得到文件名后，到媒体主外存中查文件
            cursor = resolver.query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
            cursor.moveToFirst();
            
            /*
            但是这里是个很神奇的语句
            
            文档:http://developer.android.com/reference/android/provider/MediaStore.MediaColumns.html#DATA
            文档和常识都表示，data里放的是该文件的数据流
            用getString 获得这个数据流应该是抛出 exception 的用法
            但不仅没有抛出exception，而且还返回了真实地址，实在是出乎意料
            */
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            cursor.close();
            return path;
        }
    }
}
