package com.nyasama;

import android.app.Application;
import android.content.Context;

import com.nyasama.util.PersistenceCookieStore;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * Created by oxyflour on 2014/11/15.
 *
 */
public class ThisApp extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
