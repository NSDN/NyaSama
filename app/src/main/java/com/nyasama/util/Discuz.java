package com.nyasama.util;

import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;
import com.android.volley.toolbox.StringRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Oxyflour on 2014/11/13.
 * utils to handle Discuz data
 */
public class Discuz {
    public static String DISCUZ_URL = "http://10.98.106.71:10080/bbs/api/mobile/index.php";
    public static String DISCUZ_ENC = "utf-8";
    public static String VOLLEY_ERROR = "volleyError";
    public static String sFormHash;
    public static JSONObject sUserInfo;

    static RequestQueue sQueue;
    static {
        CookieHandler.setDefault(new CookieManager());
        Cache cache = new NoCache();
        Network network = new BasicNetwork(new HurlStack());
        sQueue = new RequestQueue(cache, network);
        sQueue.start();
    }

    private static List<NameValuePair> map2list(Map<String, Object> map) {
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        for (Map.Entry<String, Object> e : map.entrySet())
            list.add(new BasicNameValuePair(e.getKey(), e.getValue().toString()));
        return list;
    }

    public static Request execute(String module,
                                  final Map<String, Object> params,
                                  final Map<String, Object> body,
                                  final Response.Listener<JSONObject> callback) {
        //
        if (module.equals("forumdisplay")) {
            if (params.get("fid") == null)
                throw new RuntimeException("fid is required for forumdisplay");
        }
        else if (module.equals("newthread") || module.equals("sendreply")) {
            body.put("formhash", sFormHash);
            body.put("mobiletype", 2);
        }
        params.put("module", module);
        //
        Request request =  new StringRequest(
            body == null ? Request.Method.GET : Request.Method.POST,
            DISCUZ_URL + "?" + URLEncodedUtils.format(map2list(params), DISCUZ_ENC),
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject = new JSONObject(response);
                    }
                    catch (JSONException e) {
                        //
                    }
                    if (jsonObject.optJSONObject("Variables").has("formhash"))
                        sFormHash = jsonObject.optJSONObject("Variables").optString("formhash");
                    callback.onResponse(jsonObject);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    JSONObject jsonObject = new JSONObject();
                    // NOTE: getMessage may return null
                    String msg = volleyError.getMessage();
                    try {
                        jsonObject.put(VOLLEY_ERROR, msg);
                    }
                    catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("VolleyError", msg != null ? msg : "Unknown");
                    callback.onResponse(jsonObject);
                }
            }) {
            @Override
            protected Map<String, String> getParams() {
                HashMap<String, String> params = new HashMap<String, String>();
                for (Map.Entry<String, Object> e : body.entrySet())
                    params.put(e.getKey(), e.getValue().toString());
                return params;
            }
        };
        sQueue.add(request);
        return request;
    }

    public static void login(final String username, final String password,
                             final Response.Listener<JSONObject> callback) {
        execute("login", new HashMap<String, Object>() {{
            put("loginsubmit", "yes");
            put("loginfield", "auto");
        }}, new HashMap<String, Object>() {{
            put("username", username);
            put("password", password);
            put("formhash", sFormHash);
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject.has("Variables"))
                    sUserInfo = jsonObject.optJSONObject("Variables");
                callback.onResponse(jsonObject);
            }
        });
    }

    static {
        login("admin", "mdd9xx950", new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
            }
        });
    }
}
