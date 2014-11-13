package com.nyasama.util;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NoCache;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by Oxyflour on 2014/11/13.
 * utils to handle Discuz data
 */
public class Discuz {
    static final String discuzUrl = "";
    static RequestQueue volleyQueue;
    static {
        Cache cache = new NoCache();
        Network network = new BasicNetwork(new HurlStack());
        volleyQueue = new RequestQueue(cache, network);
        volleyQueue.start();
    }

    public static Request execute(String module,
                                  Map<String, Object> params,
                                  final Response.Listener<JSONObject> response) {
        //
        if (module.equals("forumdisplay")) {
            if (params.get("fid") == null)
                throw new RuntimeException("fid is required for forumdisplay");
        }
        params.put("module", module);
        //
        return new JsonObjectRequest(discuzUrl, null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    response.onResponse(jsonObject);
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("volleyError", volleyError.getMessage());
                    }
                    catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    response.onResponse(jsonObject);
                }
            });
    }
}
