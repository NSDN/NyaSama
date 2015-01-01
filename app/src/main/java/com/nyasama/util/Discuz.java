package com.nyasama.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.activity.NoticeActivity;
import com.nyasama.activity.PMListActivity;
import com.nyasama.activity.UserProfileActivity;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Oxyflour on 2014/11/13.
 * utils to handle Discuz data
 */
public class Discuz {
    public static String DISCUZ_HOST = "http://bbs.nyasama.com";
    public static String DISCUZ_URL = DISCUZ_HOST + "/";
    public static String DISCUZ_API = DISCUZ_URL + "api/mobile/index.php";
    public static String DISCUZ_ENC = "gbk";

    public static String VOLLEY_ERROR = "volleyError";

    public static int NOTIFICATION_ID = 1;

    public static String BROADCAST_FILTER_LOGIN = "login";

    public static class JSONVolleyError extends JSONObject {
        public JSONVolleyError(String message) {
            super();
            try {
                put(VOLLEY_ERROR, message);
            }
            catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        public JSONVolleyError(VolleyError e) {
            this(e.getMessage());
        }
    }

    public static class Forum {
        public int id;
        public String name;
        public String icon;
        public int posts;
        public int threads;
        public int todayPosts;

        public Forum(JSONObject data) {
            id = Integer.parseInt(data.optString("fid"));
            name = data.optString("name");
            posts = Integer.parseInt(data.optString("posts"));
            threads = Integer.parseInt(data.optString("threads"));
            todayPosts = Integer.parseInt(data.optString("todayposts"));
            if (!data.isNull("icon")) {
                String html = data.optString("icon");
                Matcher matcher = Pattern.compile(" src=\"([^\"]+)\"").matcher(html);
                if (matcher.find())
                    icon = DISCUZ_URL + matcher.group(1);
            }
            if (icon == null)
                icon = DISCUZ_URL + "static/image/common/" + (todayPosts > 0 ? "forum_new.gif" : "forum.gif");
        }
    }

    public static class ForumCatalog {
        public String name;

        public ForumCatalog(String name) {
            this.name = name;
        }
    }

    public static class Thread {
        public int id;
        public String title;
        public String author;
        public String lastpost;
        public String dateline;
        public int replies;
        public int views;
        public int attachments;

        public Thread(JSONObject data) {
            id = Integer.parseInt(data.optString("tid"));
            title = data.optString("subject");
            author = data.optString("author");
            lastpost = data.optString("lastpost");
            dateline = data.optString("dateline");
            // Discuz may return integer as dateline
            int dateVal = Helper.toSafeInteger(lastpost, 0);
            if (dateVal > 0)
                lastpost = Helper.datelineToString(dateVal, null);
            replies = Integer.parseInt(data.optString("replies"));
            views = Integer.parseInt(data.optString("views"));
            attachments = Helper.toSafeInteger(data.optString("attachment"), 0);
        }
    }

    public static class Post {
        public int id;
        public int authorId;
        public int number;
        public String author;
        public String message;
        public String dateline;
        public List<Attachment> attachments;

        public Post(JSONObject data) {
            id = Integer.parseInt(data.optString("pid"));
            author = data.optString("author");
            authorId = Integer.parseInt(data.optString("authorid"));
            number = Integer.parseInt(data.optString("number"));
            message = data.optString("message");
            dateline = data.optString("dateline");

            attachments = new ArrayList<Attachment>();
            JSONObject attachlist = data.optJSONObject("attachments");
            if (attachlist != null) {
                for (Iterator<String> iter = attachlist.keys(); iter.hasNext(); ) {
                    String key = iter.next();
                    JSONObject attachData = attachlist.optJSONObject(key);
                    Attachment attachment = new Attachment(attachData);
                    attachments.add(attachment);
                }
            }
        }
    }

    public static class Attachment {
        public int id;
        public boolean isImage;
        public String name;
        public String src;
        public String size;

        public static Attachment newImageAttachment(String src) {
            Attachment attachment = new Attachment();
            attachment.isImage = true;
            attachment.name = src;
            attachment.src = src;
            attachment.size = "0kb";
            return attachment;
        }

        public Attachment() {
        }

        public Attachment(JSONObject data) {
            id = Integer.parseInt(data.optString("aid", "0"));
            name = data.optString("filename");
            src = data.optString("url") + data.optString("attachment");
            size = data.optString("attachsize");
            // Note: Discuz may set isimage 1 or -1
            isImage = !"0".equals(data.optString("isimage"));
        }
    }

    public static class Comment {
        public int authorId;
        public String author;
        public String comment;

        public Comment(JSONObject data) {
            authorId = Integer.parseInt(data.optString("authorid"));
            author = data.optString("author");
            comment = data.optString("comment");
        }

        public Comment(int authorId, String author, String comment) {
            this.authorId = authorId;
            this.author = author;
            this.comment = comment;
        }
    }

    public static class PMList {
        public boolean isNew;
        public String author;
        public int authorId;
        public String fromUser;
        public int fromUserId;
        public String toUser;
        public int toUserId;
        public String message;
        public int number;
        public String lastdate;

        public PMList(JSONObject data) {
            isNew = "1".equals(data.optString("isnew"));
            author = data.optString("author");
            authorId = Integer.parseInt(data.optString("authorid"));
            // Note: anonymous user may send you message =.=
            fromUser = data.optString("msgfrom", "anonymous");
            fromUserId = Integer.parseInt(data.optString("msgfromid", "0"));
            toUser = data.optString("tousername");
            toUserId = Integer.parseInt(data.optString("touid"));
            message = data.optString("message");

            String dateString = "";
            if (data.has("lastdateline"))
                dateString = data.optString("lastdateline");
            else if (data.has("dateline"))
                dateString = data.optString("dateline");
            if (!dateString.isEmpty()) lastdate = Helper.datelineToString(
                    Integer.parseInt(dateString), null);
            if (data.has("pmnum"))
                number = Integer.parseInt(data.optString("pmnum"));
        }
    }

    public static class Notice {
        public int id;
        public String type;
        public String note;
        public String dateline;

        public Notice(JSONObject data) {
            id = Helper.toSafeInteger(data.optString("id"), 0);
            type = data.optString("type");
            note = data.optString("note");
            if (data.has("dateline")) dateline = Helper.datelineToString(
                    Integer.parseInt(data.optString("dateline")), null);
        }
    }

    public static class FavItem {
        public int id;
        public String type;
        public int dataId;
        public String title;
        public String dateline;

        public FavItem(JSONObject data) {
            id = Helper.toSafeInteger(data.optString("favid"), 0);
            type = data.optString("idtype");
            dataId = Helper.toSafeInteger(data.optString("id"), 0);
            title = data.optString("title");
            if (data.has("dateline")) dateline = Helper.datelineToString(
                    Integer.parseInt(data.optString("dateline")), null);
        }
    }

    public static class PollOption {
        public int id;
        public String option;
        public int votes;
        public double percent;

        public PollOption(JSONObject data) {
            id = Helper.toSafeInteger(data.optString("polloptionid"), 0);
            option = data.optString("polloption");
            votes = Helper.toSafeInteger(data.optString("votes"), 0);
            percent = Helper.toSafeDouble(data.optString("percent"), 0);
        }
    }

    public static String sFormHash = "";
    public static String sUploadHash = "";
    public static String sUsername = "";
    public static String sGroupName = "";
    public static int sUid = 0;
    public static int sNewMessages = 0;
    public static int sNewPrompts = 0;
    public static boolean sHasLogined;

    private static List<NameValuePair> map2list(Map<String, Object> map) {
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        for (Map.Entry<String, Object> e : map.entrySet())
            list.add(new BasicNameValuePair(e.getKey(),
                    e.getValue() != null ? e.getValue().toString() : ""));
        return list;
    }

    // REF: Discuz\src\net\discuz\json\helper\x25\ViewThreadParseHelperX25.java
    public static String getAttachmentThumb(int attachmentId, String size) {
        String str = attachmentId + "|" + size.replace('x', '|');
        String key;
        try {
            byte[] buffer = MessageDigest.getInstance("MD5").digest(str.getBytes());
            key = String.format("%032x", new BigInteger(1, buffer));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("module", "forumimage");
        params.put("aid", attachmentId);
        params.put("version", 2);
        params.put("size", size);
        params.put("key", key);
        params.put("type", "fixnone");
        return DISCUZ_API + "?" + URLEncodedUtils.format(map2list(params), DISCUZ_ENC);
    }

    public static String getThreadCoverThumb(int threadId) {
        return DISCUZ_API + "?module=threadcover&tid=" + threadId + "&version=2";
    }

    public static boolean isSmileyUrl(String url) {
        return url.startsWith(DISCUZ_URL + "static/image/smiley/");
    }

    private static BitmapLruCache smileyCache = new BitmapLruCache();
    public static BitmapLruCache getSmileyCache() {
        return smileyCache;
    }

    public static String getSafeUrl(String url) {
        if (url == null)
            return "";
        url = url.replace(" ", "%20");
        if (url.startsWith("http://") || url.startsWith("https://"))
            return url;
        else if (url.startsWith("/"))
            return DISCUZ_HOST + url;
        else
            return DISCUZ_URL + url;
    }

    private static void notifyNewMessage() {
        Class activityClass = UserProfileActivity.class;
        if (sNewMessages > 0 && sNewPrompts == 0)
            activityClass = PMListActivity.class;
        else if (sNewMessages == 0 && sNewPrompts > 0)
            activityClass = NoticeActivity.class;
        Context context = ThisApp.context;
        Intent intents[] = {new Intent(context, activityClass)};
        PendingIntent pendingIntent = PendingIntent.getActivities(context,
                0,
                intents,
                PendingIntent.FLAG_CANCEL_CURRENT);
        String text = context.getString(R.string.prompt_1) + " " +
                (sNewMessages > 0 ? sNewMessages + " " + context.getString(R.string.prompt_2) : "") +
                (sNewPrompts > 0 ? (sNewMessages > 0 ? " " + context.getString(R.string.prompt_3) + " " : "") + sNewPrompts + " " + context.getString(R.string.prompt_4) : "");
        Notification notification = new NotificationCompat.Builder(ThisApp.context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setContentTitle(context.getString(R.string.notify_title_text))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, notification);
    }

    public static class Smiley {
        public String code;
        public String image;
    }

    public static class SmileyGroup {
        public String name;
        public String path;
        public List<Smiley> list;
    }

    private static List<SmileyGroup> parseSmilies(JSONArray data) {
        List<SmileyGroup> smileyGroups = new ArrayList<SmileyGroup>();
        for (int i = 0; i < data.length(); i++) {
            final JSONObject jsonData = data.optJSONObject(i);
            final JSONArray jsonList = jsonData.optJSONArray("list");
            final List<Smiley> smileyList = new ArrayList<Smiley>();
            for (int j = 0; j < jsonList.length(); j++) {
                final JSONArray jsonSmiley = jsonList.optJSONArray(j);
                smileyList.add(new Smiley() {{
                    code = jsonSmiley.optString(1);
                    image = jsonSmiley.optString(2);
                }});
            }
            smileyGroups.add(new SmileyGroup() {{
                name = jsonData.optString("name");
                path = jsonData.optString("path");
                list = smileyList;
            }});
        }
        return smileyGroups;
    }

    private static List<SmileyGroup> sSmilies;
    private static Response.Listener<List<SmileyGroup>> mSmiliesCallback = null;

    private static class JSInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void setSmilies(String json) {
            try {
                if (mSmiliesCallback != null)
                    mSmiliesCallback.onResponse(sSmilies = parseSmilies(new JSONArray(json)));
                Log.d("Discuz", "got smilies!");
            } catch (JSONException e) {
                Log.e("Discuz", "load smilies failed");
            }
        }
    }

    private static void parseSmileyString(String content) {
        content = "<script>" + content + "</script>" +
                "<script>" +
                "var list = [];" +
                "var type = smilies_type;" +
                "var array = smilies_array;" +
                "for (var k in type) {" +
                "var d = type[k];" +
                "var i = parseInt(k.substring(1));" +
                " if (array[i]) {" +
                "list.push({ name:d[0], path:d[1], list:array[i][1]})" +
                "}" +
                "}" +
                "JSInterface.setSmilies(JSON.stringify(list))" +
                "</script>";
        ThisApp.webView.getSettings().setJavaScriptEnabled(true);
        ThisApp.webView.addJavascriptInterface(new JSInterface(), "JSInterface");
        ThisApp.webView.loadDataWithBaseURL(null, content, "text/html", "utf-8", null);
    }

    public static List<SmileyGroup> getSmilies() {
        return sSmilies;
    }

    public static void loadSmilies(Response.Listener<List<SmileyGroup>> callback) {
        if (sSmilies != null) {
            callback.onResponse(sSmilies);
            return;
        }
        mSmiliesCallback = callback;
        Request request = new StringRequest(DISCUZ_URL + "data/cache/common_smilies_var.js", new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                parseSmileyString(s);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                parseSmileyString("smilies_type = []; smilies_array = []");
            }
        }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                try {
                    // Note: charset not confirmed in other Discuz versions
                    return Response.success(new String(response.data, "gb18030"), getCacheEntry());
                } catch (UnsupportedEncodingException e) {
                    return Response.error(new VolleyError("decode failed!"));
                }
            }
        };
        ThisApp.requestQueue.add(request);
    }

    public static class ThreadTypes extends HashMap<String, Integer> {
        public ThreadTypes(JSONObject data) {
            for (Iterator<String> iter = data.keys(); iter.hasNext(); ) {
                String key = iter.next();
                put(data.optString(key),
                        Helper.toSafeInteger(key, 0));
            }
        }
    }

    private static SparseArray<ThreadTypes> sThreadTypes;

    public static SparseArray<ThreadTypes> getThreadTypes() {
        return sThreadTypes;
    }

    public static void loadThreadTypes(final Response.Listener<SparseArray<ThreadTypes>> callback) {
        if (sThreadTypes != null) {
            callback.onResponse(sThreadTypes);
            return;
        }
        Discuz.execute("forumnav", new HashMap<String, Object>(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject data) {
                        JSONObject var = data.optJSONObject("Variables");
                        if (var == null) return;
                        sThreadTypes = new SparseArray<ThreadTypes>();
                        JSONArray forums = var.optJSONArray("forums");
                        for (int i = 0; i < forums.length(); i++) {
                            JSONObject forum = forums.optJSONObject(i);
                            JSONObject threadtypes = forum.optJSONObject("threadtypes");
                            if (threadtypes != null && !threadtypes.isNull("types"))
                                sThreadTypes.put(Helper.toSafeInteger(forum.optString("fid"), 0),
                                        new ThreadTypes(threadtypes.optJSONObject("types")));
                        }
                        callback.onResponse(sThreadTypes);
                    }
                });
    }

    public static class ResponseListener implements Response.Listener<String> {
        Response.Listener<JSONObject> callback;

        public ResponseListener(Response.Listener<JSONObject> callback) {
            this.callback = callback;
        }

        @Override
        public void onResponse(String response) {
            JSONObject data = new JSONObject();
            try {
                data = new JSONObject(response);
            } catch (JSONException e) {
                Log.e("JSONError", "Parse JSON failed: " + e.getMessage());
            }
            JSONObject var = data.optJSONObject("Variables");
            if (var != null) {
                sFormHash = var.optString("formhash", "");
                sUsername = var.optString("member_username", "");
                sUid = Integer.parseInt(var.optString("member_uid", "0"));
                if (!var.isNull("allowperm"))
                    sUploadHash = var.optJSONObject("allowperm").optString("uploadhash", "");
                if (!var.isNull("group"))
                    sGroupName = var.optJSONObject("group").optString("grouptitle", "");

                // check if login ok
                boolean hasLogined = !var.isNull("auth");
                if (sHasLogined != hasLogined) {
                    LocalBroadcastManager.getInstance(ThisApp.context)
                            .sendBroadcast(new Intent(BROADCAST_FILTER_LOGIN));
                    sHasLogined = hasLogined;
                }

                // check messages && prompts
                int newMessages = var.isNull("member_pm") ? 0 :
                        Integer.parseInt(var.optString("member_pm"));
                int newPrompts = var.isNull("member_prompt") ? 0 :
                        Integer.parseInt(var.optString("member_prompt"));
                if (newMessages > sNewMessages || newPrompts > sNewPrompts) {
                    sNewMessages = newMessages;
                    sNewPrompts = newPrompts;
                    notifyNewMessage();
                }

            }
            callback.onResponse(data);
            ThisApp.cookieStore.save();
        }
    }

    public static class ResponseErrorListener implements Response.ErrorListener {
        Response.Listener<JSONObject> callback;

        public ResponseErrorListener(Response.Listener<JSONObject> callback) {
            this.callback = callback;
        }

        @Override
        public void onErrorResponse(VolleyError volleyError) {
            String msg = volleyError.getMessage();
            // NOTE: getMessage may return null
            if (msg == null) msg = "Unknown";
            Log.e("VolleyError", msg);
            callback.onResponse(new JSONVolleyError(msg));
        }
    }

    public static Request execute(String module,
                                  final Map<String, Object> params,
                                  final Map<String, Object> body,
                                  final Response.Listener<JSONObject> callback) {
        //
        if (module.equals("forumdisplay")) {
            if (params.get("fid") == null)
                throw new RuntimeException("fid is required for forumdisplay");
        } else if (module.equals("newthread") || module.equals("sendreply")) {
            Helper.putIfNull(body, "formhash", sFormHash);
            Helper.putIfNull(body, "allowphoto", 1);
            Helper.putIfNull(body, "mobiletype", 2);
        } else if (module.equals("addcomment")) {
            Helper.putIfNull(body, "formhash", sFormHash);
            Helper.putIfNull(body, "commentsubmit", "yes");
            Helper.putIfNull(body, "handlekey", "comment");
        } else if (module.equals("sendpm")) {
            Helper.putIfNull(body, "formhash", sFormHash);
            Helper.putIfNull(body, "pmsubmit", "yes");
        } else if (module.equals("favthread")) {
            Helper.putIfNull(body, "formhash", sFormHash);
            Helper.putIfNull(body, "favoritesubmit", "yes");
        } else if (module.equals("editpost")) {
            Helper.putIfNull(body, "formhash", sFormHash);
            Helper.putIfNull(body, "editsubmit", "yes");
        } else if (module.equals("pollvote")) {
            Helper.putIfNull(params, "pollsubmit", "yes");
            Helper.putIfNull(params, "version", "2");
            Helper.putIfNull(body, "formhash", sFormHash);
            Helper.putIfNull(body, "pollsubmit", "true");
        }
        params.put("module", module);
        Helper.putIfNull(params, "submodule", "checkpost");

        Request request;
        String url = DISCUZ_API + "?" + URLEncodedUtils.format(map2list(params), DISCUZ_ENC);
        if (module.equals("editpost") && body != null) {
            Map<String, ContentBody> contentBody = new HashMap<String, ContentBody>();
            try {
                for (Map.Entry<String, Object> entry : body.entrySet())
                    if (entry.getValue() != null) contentBody.put(entry.getKey(),
                            new StringBody(entry.getValue().toString(), Charset.forName(DISCUZ_ENC)));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            request = new MultipartRequest(
                    url, contentBody,
                    new ResponseListener(callback),
                    new ResponseErrorListener(callback));
        }
        else {
            request =  new StringRequest(
                    body == null ? Request.Method.GET : Request.Method.POST, url,
                    new ResponseListener(callback),
                    new ResponseErrorListener(callback)) {
                public void addToPostBody(StringBuilder encodedParams, String key, String value) {
                    String paramsEncoding = getParamsEncoding();
                    try {
                        encodedParams.append(URLEncoder.encode(key, paramsEncoding));
                        encodedParams.append('=');
                        encodedParams.append(URLEncoder.encode(value, paramsEncoding));
                        encodedParams.append('&');
                    }
                    catch (UnsupportedEncodingException uee) {
                        throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
                    }
                }
                // REF: https://android.googlesource.com/platform/frameworks/volley/+/master/src/main/java/com/android/volley/Request.java
                @Override
                public byte[] getBody() throws AuthFailureError {
                    String paramsEncoding = getParamsEncoding();
                    StringBuilder encodedParams = new StringBuilder();
                    if (body != null) for (Map.Entry<String, Object> entry : body.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof List) {
                            for (Object item : (List) entry.getValue())
                                if (item != null)
                                    addToPostBody(encodedParams, entry.getKey(), item.toString());
                        }
                        else if (value != null) {
                            addToPostBody(encodedParams, entry.getKey(), value.toString());
                        }
                    }
                    try {
                        return encodedParams.toString().getBytes(paramsEncoding);
                    } catch (UnsupportedEncodingException uee) {
                        throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
                    }
                }

                @Override
                protected Map<String, String> getParams() {
                    HashMap<String, String> params = new HashMap<String, String>();
                    if (body != null)
                        for (Map.Entry<String, Object> e : body.entrySet())
                            if (e.getValue() != null)
                                params.put(e.getKey(), e.getValue().toString());
                    return params;
                }

                @Override
                protected String getParamsEncoding() {
                    return DISCUZ_ENC;
                }
            };
        }
        ThisApp.requestQueue.add(request);
        return request;
    }

    @SuppressWarnings("unused")
    public static void upload(final Map<String, Object> params,
                              final String filePath,
                              final Response.Listener<String> callback) {

        if (sUploadHash == null || sUid == 0 || filePath == null) {
            callback.onResponse(null);
            return;
        }

        params.put("module", "forumupload");
        params.put("hash", sUploadHash);
        params.put("uid", sUid);

        LinkedHashMap<String, ContentBody> body = new LinkedHashMap<String, ContentBody>();
        try {
            body.put("hash", new StringBody(sUploadHash));
            body.put("uid", new StringBody("" + sUid));
            body.put("Filedata", new FileBody(new File(filePath)));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        Request request = new MultipartRequest(
                DISCUZ_API + "?" + URLEncodedUtils.format(map2list(params), DISCUZ_ENC),
                body,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        callback.onResponse(s);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        callback.onResponse(null);
                    }
                }
        );
        ThisApp.requestQueue.add(request);
    }

    @SuppressWarnings("unchecked")
    public static void upload(final Map<String, Object> params,
                              final String filePath,
                              final Response.Listener<String> callback,
                              final Response.Listener<Integer> process) {

        if (sUploadHash == null || sUid == 0 || filePath == null) {
            callback.onResponse(null);
            return;
        }

        params.put("module", "forumupload");
        params.put("hash", sUploadHash);
        params.put("uid", sUid);

        final String url = DISCUZ_API + "?" + URLEncodedUtils.format(map2list(params), DISCUZ_ENC);
        AsyncTask task = new AsyncTask<Object, Integer, String>() {
            @Override
            protected String doInBackground(Object[] objects) {

                final MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE,
                        "****************" + UUID.randomUUID().toString().replace("-", "").substring(0, 15), null) {
                    {
                        try {
                            addPart("hash", new StringBody(sUploadHash));
                            addPart("uid", new StringBody("" + sUid));
                            String ext = MimeTypeMap.getFileExtensionFromUrl(filePath);
                            FileBody fileBody;
                            if (ext != null) {
                                String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                                fileBody = new FileBody(new File(filePath), type);
                            } else {
                                fileBody = new FileBody(new File(filePath));
                            }
                            addPart("Filedata", fileBody);
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void writeTo(final OutputStream outstream) throws IOException {
                        final int total = (int) getContentLength();
                        // create a stream to count the transferred size
                        super.writeTo(new FilterOutputStream(outstream) {
                            private int transferred = 0;

                            @Override
                            public void write(@Nullable byte[] buffer, int offset, int length) throws IOException {
                                int sent = 0;
                                while (buffer != null && sent < length) {
                                    int toSend = Math.min(64, length - sent);
                                    out.write(buffer, offset + sent, toSend);
                                    out.flush();
                                    publishProgress((transferred += toSend) * 100 / total);
                                    sent += toSend;
                                }
                            }

                            @Override
                            public void write(int oneByte) throws IOException {
                                out.write(oneByte);
                                publishProgress((transferred += 1) * 100 / total);
                            }
                        });
                    }
                };

                HttpPost post = new HttpPost(url) {{
                    setEntity(entity);
                }};
                try {
                    return EntityUtils.toString(new DefaultHttpClient().execute(post).getEntity());
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                process.onResponse(values[0]);
            }

            @Override
            protected void onPostExecute(String s) {
                callback.onResponse(s);
            }
        };
        task.execute();
    }

    public static Pattern searchMessagePatt = Pattern.compile("<div id=\"messagetext\" class=\"alert_info\">\\s*<p>(.*?)<",
            Pattern.DOTALL | Pattern.MULTILINE);
    public static void search(final String text,
                              final Map<String, Object> params,
                              final Response.Listener<JSONObject> callback) {
        final Request request = new StringRequest(
                Request.Method.POST,
                // set #noredirect# so volley will throw error 302
                DISCUZ_URL + "search.php?searchsubmit=yes##noredirect#",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        Matcher matcher = searchMessagePatt.matcher(s);
                        String message = "Unexpected Response";
                        if (matcher.find())
                            message = matcher.group(1);
                        JSONObject data = new JSONObject();
                        try {
                            JSONObject msgData = new JSONObject();
                            msgData.put("messagestr", message);
                            data.put("Message", msgData);
                        }
                        catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        callback.onResponse(data);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        String searchUrl = volleyError.networkResponse.headers.get("location");
                        if (searchUrl == null) {
                            callback.onResponse(new JSONVolleyError(volleyError));
                            return;
                        }
                        final Uri uri = Uri.parse(searchUrl);
                        for (String key : uri.getQueryParameterNames())
                            params.put(key, uri.getQueryParameter(key));
                        Discuz.execute("search", params, null, callback);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                return new HashMap<String, String>() {{
                    put("mod", "forum");
                    put("forumhash", sFormHash);
                    put("srhfid", "");
                    put("srhlocality", "forum:index");
                    put("srchtxt", text);
                    put("searchsubmit", "true");
                }};
            }
            @Override
            protected String getParamsEncoding() {
                return DISCUZ_ENC;
            }
        };
        ThisApp.requestQueue.add(request);
    }

    public static void login(final String username, final String password,
                             final int questionId, final String answer,
                             final Response.Listener<JSONObject> callback) {
        execute("login", new HashMap<String, Object>() {{
            put("loginsubmit", "yes");
            put("loginfield", "auto");
        }}, new HashMap<String, Object>() {{
            put("username", username);
            put("password", password);
            put("formhash", sFormHash);
            if (questionId > 0) {
                put("questionid", questionId);
                put("answer", answer);
            }
        }}, callback);
    }

    // Note: "Logout" is not found in the api source =.=
    public static void logout(final Response.Listener<JSONObject> callback) {
        sUid = 0;
        sNewMessages = 0;
        sNewPrompts = 0;
        sHasLogined = false;
        ThisApp.cookieStore.removeAll();
        ThisApp.cookieStore.save();
        LocalBroadcastManager.getInstance(ThisApp.context)
                .sendBroadcast(new Intent(BROADCAST_FILTER_LOGIN));
        callback.onResponse(new JSONObject());
    }

    static Pattern signinMessagePattern = Pattern.compile("<p>(.*?)</p>");

    public static void signin(final Response.Listener<String> callback) {
        String url = DISCUZ_URL + "plugin.php?id=dsu_amupper:pper&ppersubmit=true&formhash=" + sFormHash + "&mobile=yes";
        Request request = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                Matcher matcher = signinMessagePattern.matcher(s);
                if (matcher.find())
                    callback.onResponse(matcher.group(1));
                else
                    callback.onResponse(null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                callback.onResponse(volleyError.getMessage());
            }
        });
        ThisApp.requestQueue.add(request);
    }

}
