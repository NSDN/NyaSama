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
 * 把cookiestore中的数据存入sharedpreference
 * 把sharedpreference中的数据取出放入cookiestore
 * 
 * 在Discuz中见到其调用，具体目的，用法还不明确
 * InMemoryCookieStore 文档：http://www.docjar.com/docs/api/sun/net/www/protocol/http/InMemoryCookieStore.html
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

    public void save() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.clear();
        for (URI uri : getURIs()) {
            Set<String> cookieStrs = new HashSet<String>();
            for (HttpCookie ck : get(uri))
                cookieStrs.add(ck.toString());
            editor.putStringSet(uri.toString(), cookieStrs);
        }
        editor.apply();
    }
}
