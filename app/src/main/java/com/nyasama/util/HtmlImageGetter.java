package com.nyasama.util;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.text.Html;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.nyasama.ThisApp;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oxyflour on 2014/11/19.
 *
 */
public class HtmlImageGetter implements Html.ImageGetter {

    private String baseUrl;
    private TextView container;

    class Size {
        public int width;
        public int height;
    }
    static Map<String, Size> cachedImageSize = new HashMap<String, Size>();

    public HtmlImageGetter(TextView container,String baseUrl) {
        this.container = container;
        this.baseUrl = baseUrl;
    }

    @Override
    public Drawable getDrawable(String s) {

        final String url = baseUrl + s;
        final LevelListDrawable drawable = new LevelListDrawable();
        Drawable empty = ThisApp.context.getResources()
                .getDrawable(android.R.drawable.ic_menu_gallery);
        drawable.addLevel(0, 0, empty);
        Size size = cachedImageSize.get(url);
        drawable.setBounds(0, 0,
                // use cached size
                size != null ? size.width : empty.getIntrinsicWidth(),
                size != null ? size.height : empty.getIntrinsicHeight());

        ImageRequest request = new ImageRequest(url, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(final Bitmap bitmap) {
                drawable.addLevel(1, 1, new BitmapDrawable(ThisApp.context.getResources(), bitmap));
                drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                drawable.setLevel(1);
                // save to cache
                cachedImageSize.put(url, new Size() {{
                    width = bitmap.getWidth();
                    height = bitmap.getHeight();
                }});
                // there should be a better way to refresh the layout
                container.setText(container.getText());
            }
        }, 0, 0, null, null);
        ThisApp.requestQueue.add(request);

        return drawable;
    }
}
