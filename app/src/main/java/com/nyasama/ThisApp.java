package com.nyasama;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.webkit.WebView;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.nyasama.activity.SplashActivity;
import com.nyasama.util.BitmapLruCache;
import com.nyasama.util.PersistenceCookieStore;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Locale;

/**
 * Created by oxyflour on 2014/11/15.
 */
public class ThisApp extends Application {
    public static Context context;
    public static Cache volleyCache;
    public static RequestQueue requestQueue;
    public static ImageLoader imageLoader;
    public static PersistenceCookieStore cookieStore;
    public static WebView webView;

    private static Locale getLocale(SharedPreferences preferences) {
        String[] values = context.getResources().getStringArray(R.array.language_preference);
        String language = preferences.getString("language", values[0]);
        if (language.equals(values[0])) return Locale.getDefault();
        if (language.equals(values[1])) return Locale.SIMPLIFIED_CHINESE;
        if (language.equals(values[2])) return Locale.ENGLISH;
        return Locale.getDefault();
    }

    // REF: http://aleung.github.io/blog/2012/10/06/change-locale-in-android-application/
    private static void loadLocaleFromPreference(SharedPreferences preferences) {
        Locale locale = getLocale(preferences);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();

        File cacheFile = new File(getCacheDir(), "NyasamaVolleyCache");
        volleyCache = new DiskBasedCache(cacheFile, 1024 * 1024 * 32);

        Network network = new BasicNetwork(new HurlStack());
        requestQueue = new RequestQueue(volleyCache, network);
        requestQueue.start();

        ImageLoader.ImageCache imgCache = new BitmapLruCache();
        imageLoader = new ImageLoader(requestQueue, imgCache);

        cookieStore = new PersistenceCookieStore(context);
        CookieHandler.setDefault(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL));

        webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);

        loadLocaleFromPreference(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public static void restart() {
        // REF: http://stackoverflow.com/questions/6609414/howto-programatically-restart-android-app
        Intent intent = new Intent(context, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Intent[] intents = {intent};
        PendingIntent pendingIntent = PendingIntent.getActivities(ThisApp.context, 0,
                intents,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager mgr = (AlarmManager) ThisApp.context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
        System.exit(2);
    }
}
