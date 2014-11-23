package com.nyasama;

import android.app.Application;
import android.content.Context;
import android.webkit.WebView;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.nyasama.util.BitmapLruCache;
import com.nyasama.util.Discuz;
import com.nyasama.util.PersistenceCookieStore;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * Created by oxyflour on 2014/11/15.
 *
 */
public class ThisApp extends Application {
    public static Context context;
    public static RequestQueue requestQueue;
    public static ImageLoader imageLoader;
    public static PersistenceCookieStore cookieStore;
    public static WebView webView;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();

        File cacheFile = new File(getCacheDir(), "NyasamaVolleyCache");
        Cache cache = new DiskBasedCache(cacheFile, 1024*1024*4);
        Network network = new BasicNetwork(new HurlStack());
        requestQueue = new RequestQueue(cache, network);
        requestQueue.start();

        ImageLoader.ImageCache imgCache = new BitmapLruCache();
        imageLoader = new ImageLoader(requestQueue, imgCache);

        cookieStore = new PersistenceCookieStore(context);
        CookieHandler.setDefault(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL));

        webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);

        // TODO: move this into splash activity
        Discuz.getSmileies();
    }
}
