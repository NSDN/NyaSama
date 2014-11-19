package com.nyasama.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.View;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.nyasama.ThisApp;

/**
 * Created by oxyflour on 2014/11/19.
 *
 */
public class HtmlDrawable extends Drawable {

    protected Drawable drawable;

    public HtmlDrawable(final View view, String url) {
        ImageRequest request = new ImageRequest(url, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap bitmap) {
                drawable = new BitmapDrawable(ThisApp.context.getResources(), bitmap);
                DisplayMetrics metrics = ThisApp.context.getResources().getDisplayMetrics();
                drawable.setBounds(0, 0, bitmap.getScaledWidth(metrics), bitmap.getScaledHeight(metrics));
                view.invalidate();
            }
        }, 0, 0, null, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                drawable = ThisApp.context.getResources().getDrawable(android.R.drawable.ic_menu_gallery);
                view.invalidate();
            }
        });
        ThisApp.requestQueue.add(request);
    }

    @Override
    public void draw(Canvas canvas) {
        if (drawable != null) {
            drawable.draw(canvas);
        }
    }

    @Override
    public void setAlpha(int i) {
        if (drawable != null)
            drawable.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (drawable != null)
            drawable.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        if (drawable != null)
            return drawable.getOpacity();
        return 1;
    }

    public static class ImageGetter implements Html.ImageGetter {

        private String baseUrl;
        private View container;

        public ImageGetter(View view, String url) {
            container = view;
            baseUrl = url;
        }

        @Override
        public Drawable getDrawable(String s) {
            return new HtmlDrawable(container, baseUrl + s);
        }
    }

}
