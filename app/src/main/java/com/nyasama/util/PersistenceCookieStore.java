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

    public PersistenceCookieStore(Context context) {
        mPrefs = context.getSharedPreferences("PersistenceCookiePref", Context.MODE_PRIVATE);
    }

    public void save() {
        SharedPreferences.Editor editor = mPrefs.edit();
        for (URI uri : getURIs()) {
            Set<String> cookieStrs = new HashSet<String>();
            for (HttpCookie cookie : get(uri))
                cookieStrs.add(cookie.toString());
            editor.putStringSet(uri.toString(), cookieStrs);
        }
        editor.apply();
    }

    @SuppressWarnings("unchecked")
    public void restore() {
        Map<String, ?> saved = mPrefs.getAll();
        for (Map.Entry<String, ?> entry : saved.entrySet()) {
            URI uri = URI.create(entry.getKey());
            for (String cookieStr : (Set<String>) entry.getValue())
                for (HttpCookie cookie : HttpCookie.parse(cookieStr))
                    add(uri, cookie);
        }
    }
}
