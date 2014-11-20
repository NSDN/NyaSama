package com.nyasama.util;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * Created by oxyflour on 2014/11/20.
 * REF: http://stackoverflow.com/questions/16797468/how-to-send-a-multipart-form-data-post-in-android-with-volley
 */
public class MultipartRequest extends Request<String> {

    private MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE,
            "----------"+UUID.randomUUID().toString().replace("-", ""),
            Charset.defaultCharset());

    private Response.Listener mListener;

    public MultipartRequest(String url, LinkedHashMap<String, ContentBody> body,
                            Response.Listener<String> onSuccess, Response.ErrorListener onError) {
        super(Method.POST, url, onError);
        mListener = onSuccess;
        for (LinkedHashMap.Entry<String, ContentBody> entry : body.entrySet())
            entity.addPart(entry.getKey(), entry.getValue());
    }

    @Override
    public String getBodyContentType() {
        return entity.getContentType().getValue();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            entity.writeTo(stream);
        }
        catch (IOException e) {
            VolleyLog.e("IOException writing to ByteArrayOutputStream (" + e.getMessage() + ")");
        }
        return stream.toByteArray();
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse networkResponse) {
        // TODO:
        return Response.success(new String(networkResponse.data), getCacheEntry());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void deliverResponse(String s) {
        mListener.onResponse(s);
    }
}
