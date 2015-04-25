package com.nyasama.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.text.Html;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.nyasama.ThisApp;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oxyflour on 2014/11/19.
 *
 */
public class HtmlImageGetter implements Html.ImageGetter {

    public static class HtmlImageCache {
        public ImageLoader.ImageCache images;
        public Map<String, Point> size;
        public HtmlImageCache(ImageLoader.ImageCache cache) {
            this.images = cache;
            this.size = new HashMap<String, Point>();
        }
    }

    private TextView container;
    private HtmlImageCache cache;
    private Point maxSize;
    private int jobs;

    public HtmlImageGetter(TextView container, HtmlImageCache cache, Point maxSize) {
        this.container = container;
        this.cache = cache;
        this.maxSize = maxSize;
    }

    @Override
    public Drawable getDrawable(String s) {
        if (s == null) return null;

        final String url = Discuz.getSafeUrl(s);
        final LevelListDrawable drawable = new LevelListDrawable();

        // for smilies, we will cache it in Discuz
        boolean isSmileyUrl = Discuz.Smiley.isSmileyUrl(url);
        final ImageLoader.ImageCache imageCache = isSmileyUrl ? Discuz.Smiley.getCache() : cache.images;
        final Map<String, Point> imageSize = cache.size;
        final int imageWidth = isSmileyUrl ? 0 : maxSize.x;
        final int imageHeight = isSmileyUrl ? 0 : maxSize.y;

        final Resources resources = ThisApp.context.getResources();
        Bitmap cachedImage = imageCache != null ?
                imageCache.getBitmap(url) :
                null;
        Drawable empty = cachedImage != null ?
                new BitmapDrawable(resources, cachedImage) :
                resources.getDrawable(android.R.drawable.ic_menu_gallery);
        Point size = imageSize.containsKey(url) ? imageSize.get(url) : null;
        drawable.addLevel(0, 0, empty);
        drawable.setBounds(0, 0,
                size != null ? size.x : empty.getIntrinsicWidth(),
                size != null ? size.y : empty.getIntrinsicHeight());

        if (cachedImage == null && imageWidth >= 0 && imageHeight >= 0) {
            jobs ++;
            ImageRequest request = new ImageRequest(url, new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(final Bitmap bitmap) {
                    drawable.addLevel(1, 1, new BitmapDrawable(resources, bitmap));
                    drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    drawable.setLevel(1);
                    // save to cache
                    if (imageCache != null)
                        imageCache.putBitmap(url, bitmap);
                    if (imageSize != null)
                        imageSize.put(url, new Point(bitmap.getWidth(), bitmap.getHeight()));
                    // refresh layout
                    jobs --;
                    if (jobs == 0) {
                        // TODO: find a better way to refresh the layout
                        container.setText(container.getText());
                    }
                }
            }, imageWidth, imageHeight, null, null);
            ThisApp.requestQueue.add(request);
        }

        return drawable;
    }
}
