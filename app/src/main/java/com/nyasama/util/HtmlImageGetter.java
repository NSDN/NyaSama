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

/**
 * Created by oxyflour on 2014/11/19.
 *
 */
public class HtmlImageGetter implements Html.ImageGetter {

    private String baseUrl;
    private TextView container;

    public HtmlImageGetter(TextView container,String baseUrl) {
        this.container = container;
        this.baseUrl = baseUrl;
    }

    @Override
    public Drawable getDrawable(String s) {
        final LevelListDrawable drawable = new LevelListDrawable();
        Drawable empty = ThisApp.context.getResources()
                .getDrawable(android.R.drawable.ic_menu_gallery);
        drawable.addLevel(0, 0, empty);
        drawable.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());

        ImageRequest request = new ImageRequest(baseUrl + s, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap bitmap) {
                drawable.addLevel(1, 1, new BitmapDrawable(ThisApp.context.getResources(), bitmap));
                drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                drawable.setLevel(1);
                // there should be a better way to refresh the layout
                container.setText(container.getText());
            }
        }, 0, 0, null, null);
        ThisApp.requestQueue.add(request);

        return drawable;
    }
}
