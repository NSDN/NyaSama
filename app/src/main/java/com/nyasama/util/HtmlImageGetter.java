package com.nyasama.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.text.Html;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.nyasama.ThisApp;

import java.util.Map;

/**
 * Created by oxyflour on 2014/11/19.
 *
 */
public class HtmlImageGetter implements Html.ImageGetter {

    private TextView container;
    private Map<String, Bitmap> cache;

    public HtmlImageGetter(TextView container, Map<String, Bitmap> cache) {
        this.container = container;
        this.cache = cache;
    }

    @Override
    public Drawable getDrawable(String s) {
        if (s == null) return null;

        final String url = Discuz.getSafeUrl(s);
        final LevelListDrawable drawable = new LevelListDrawable();

        Bitmap cachedImage = cache == null ? null : cache.get(url);
        Resources resources = ThisApp.context.getResources();
        Drawable empty = cachedImage == null ?
                resources.getDrawable(android.R.drawable.ic_menu_gallery):
                new BitmapDrawable(resources, cachedImage);
        drawable.addLevel(0, 0, empty);
        drawable.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());

        if (cachedImage == null) {
            ImageRequest request = new ImageRequest(url, new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(final Bitmap bitmap) {
                    drawable.addLevel(1, 1, new BitmapDrawable(ThisApp.context.getResources(), bitmap));
                    drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    drawable.setLevel(1);
                    // save to cache
                    if (cache != null)
                        cache.put(url, bitmap);
                    // there should be a better way to refresh the layout
                    container.setText(container.getText());
                }
            }, 0, 0, null, null);
            ThisApp.requestQueue.add(request);
        }

        return drawable;
    }
}
