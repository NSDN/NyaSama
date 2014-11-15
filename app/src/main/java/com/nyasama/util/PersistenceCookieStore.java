package com.nyasama.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.HttpCookie;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by oxyflour on 2014/11/15.
 *
 */
public class PersistenceCookieStore extends InMemoryCookieStore {

    private SharedPreferences mPrefs;

    @SuppressWarnings("unchecked")
    public PersistenceCookieStore(Context context) {
        super();
        mPrefs = context.getSharedPreferences("PersistenceCookiePref", Context.MODE_PRIVATE);
        Map<String, ?> saved = mPrefs.getAll();
        for (Map.Entry<String, ?> entry : saved.entrySet()) {
            URI uri = URI.create(entry.getKey());
            for (String cookieStr : (Set<String>) entry.getValue())
                for (HttpCookie cookie : HttpCookie.parse(cookieStr))
                    add(uri, cookie);
        }
    }

    @Override
    public void add(URI uri, HttpCookie cookie) {
        super.add(uri, cookie);
        SharedPreferences.Editor editor = mPrefs.edit();
        Set<String> cookieStrs = new HashSet<String>();
        for (HttpCookie ck : get(uri))
            cookieStrs.add(ck.toString());
        editor.putStringSet(uri.toString(), cookieStrs);
        editor.apply();
    }
}
