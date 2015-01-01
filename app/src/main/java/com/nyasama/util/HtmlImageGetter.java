package com.nyasama.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.text.Html;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.nyasama.ThisApp;

/**
 * Created by oxyflour on 2014/11/19.
 *
 */
public class HtmlImageGetter implements Html.ImageGetter {

    private TextView container;
    private ImageLoader.ImageCache cache;
    private int jobs;
    private int maxWidth;
    private int maxHeight;

    public HtmlImageGetter(TextView container, ImageLoader.ImageCache cache, int maxWidth, int maxHeight) {
        this.container = container;
        this.cache = cache;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    @Override
    public Drawable getDrawable(String s) {
        if (s == null) return null;

        final String url = Discuz.getSafeUrl(s);
        final LevelListDrawable drawable = new LevelListDrawable();

        // for smilies, we will cache it in Discuz
        boolean isSmileyUrl = Discuz.isSmileyUrl(url);
        final ImageLoader.ImageCache imageCache = isSmileyUrl ? Discuz.getSmileyCache() : this.cache;
        final int imageWidth = isSmileyUrl ? 0 : maxWidth;
        final int imageHeight = isSmileyUrl ? 0 : maxHeight;

        final Resources resources = ThisApp.context.getResources();
        Bitmap cachedImage = imageCache != null ?
                imageCache.getBitmap(url) :
                null;
        Drawable empty = cachedImage != null ?
                new BitmapDrawable(resources, cachedImage) :
                resources.getDrawable(android.R.drawable.ic_menu_gallery);
        drawable.addLevel(0, 0, empty);
        drawable.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());

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
