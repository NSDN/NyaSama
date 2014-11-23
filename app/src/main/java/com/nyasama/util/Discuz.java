package com.nyasama.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.activity.UserProfileActivity;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Oxyflour on 2014/11/13.
 * utils to handle Discuz data
 */
public class Discuz {
    public static String DISCUZ_URL = "http://tech.touhou.moe/";
    public static String DISCUZ_API = DISCUZ_URL + "api/mobile/index.php";
    public static String DISCUZ_ENC = "utf-8";
    public static String VOLLEY_ERROR = "volleyError";

    public static int NOTIFICATION_ID = 1;

    public static int REQUEST_CODE_LOGIN = 1;
    public static int REQUEST_CODE_NEW_THREAD = 2;
    public static int REQUEST_CODE_REPLY = 3;

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
        public List<Forum> forums;
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
            replies = Integer.parseInt(data.optString("replies"));
            views = Integer.parseInt(data.optString("views"));
            if (!data.isNull("attachments"))
                attachments = Integer.parseInt(data.optString("attachments"));
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

            JSONObject attachlist = data.optJSONObject("attachments");
            if (attachlist != null) {
                for (Iterator<String> iter = attachlist.keys(); iter.hasNext(); ) {
                    String key = iter.next();
                    JSONObject attachData = attachlist.optJSONObject(key);
                    if (attachments == null)
                        attachments = new ArrayList<Attachment>();
                    Attachment attachment = new Attachment();
                    attachment.name = attachData.optString("filename");
                    attachment.src = attachData.optString("attachment");
                    attachments.add(attachment);
                }
            }
        }
    }
    public static class Attachment {
        public String name;
        public String src;
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

    private static void notifyNewMessage() {
        Context context = ThisApp.context;
        Intent intents[] = {new Intent(context, UserProfileActivity.class)};
        PendingIntent pendingIntent = PendingIntent.getActivities(context,
                0,
                intents,
                PendingIntent.FLAG_CANCEL_CURRENT);
        String text = "you've got " +
                (sNewMessages > 0 ? sNewMessages+" pms" : "") +
                (sNewPrompts > 0 ? (sNewMessages>0 ? " and " : "")+sNewPrompts+" prompts" : "");
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

    public static List<SmileyGroup> sSmilies = new ArrayList<SmileyGroup>();
    private static Response.Listener<JSONObject> mSmiliesCallback = null;
    private static class JSInterface {
        @JavascriptInterface
        public void setSmilies(String json) {
            try {
                parseSmilies(new JSONArray(json));
                if (mSmiliesCallback != null)
                    mSmiliesCallback.onResponse(null);
                Log.d("Discuz", "got smilies!");
            }
            catch (JSONException e) {
                Log.e("Discuz", "load smilies failed");
            }
        }
    }
    public static void getSmileies(Response.Listener<JSONObject> callback) {
        mSmiliesCallback = callback;
        String content = "<script src=\""+DISCUZ_URL+"data/cache/common_smilies_var.js\"></script>"+
            "<script>"+
                "var list = [];" +
                "var type = smilies_type;" +
                "var array = smilies_array;" +
                "for (var k in type) {" +
                    "var d = type[k];"+
                    "var i = parseInt(k.substring(1));"+
                    " if (array[i]) {" +
                        "list.push({ name:d[0], path:d[1], list:array[i][1]})"+
                    "}"+
                "}"+
                "JSInterface.setSmilies(JSON.stringify(list))"+
            "</script>";
        ThisApp.webView.getSettings().setJavaScriptEnabled(true);
        ThisApp.webView.addJavascriptInterface(new JSInterface(), "JSInterface");
        ThisApp.webView.loadDataWithBaseURL(null, content, "text/html", "gb18030", null);
    }
    private static void parseSmilies(JSONArray data) {
        sSmilies.clear();
        for (int i = 0; i < data.length(); i ++) {
            final JSONObject jsonData = data.optJSONObject(i);
            final JSONArray jsonList = jsonData.optJSONArray("list");
            final List<Smiley> smileyList = new ArrayList<Smiley>();
            for (int j = 0; j < jsonList.length(); j ++) {
                final JSONArray jsonSmiley = jsonList.optJSONArray(j);
                smileyList.add(new Smiley() {{
                    code = jsonSmiley.optString(1);
                    image = jsonSmiley.optString(2);
                }});
            }
            sSmilies.add(new SmileyGroup() {{
                name = jsonData.optString("name");
                path = jsonData.optString("path");
                list = smileyList;
            }});
        }
    }

    public static List<ForumCatalog> sForumCatalogs = new ArrayList<ForumCatalog>();

    public static void loadForums(final Response.Listener<JSONObject> callback) {
        Discuz.execute("forumindex", new HashMap<String, Object>(), null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else if (!data.isNull("Variables")) {
                    try {
                        JSONObject var = data.getJSONObject("Variables");
                        JSONArray forumlist = var.getJSONArray("forumlist");
                        final JSONObject forums = new JSONObject();
                        for (int i = 0; i < forumlist.length(); i++) {
                            JSONObject forum = forumlist.getJSONObject(i);
                            forums.put(forum.getString("fid"), forum);
                        }

                        JSONArray catlist = var.getJSONArray("catlist");
                        sForumCatalogs.clear();
                        for (int i = 0; i < catlist.length(); i++) {
                            JSONObject cat = catlist.getJSONObject(i);
                            ForumCatalog forumCatalog = new ForumCatalog();
                            forumCatalog.name = cat.getString("name");

                            final JSONArray forumIds = cat.getJSONArray("forums");
                            forumCatalog.forums = new ArrayList<Forum>();
                            for (int j = 0; j < forumIds.length(); j++) {
                                String id = forumIds.getString(j);
                                JSONObject forum = forums.getJSONObject(id);
                                forumCatalog.forums.add(new Forum(forum));
                            }
                            sForumCatalogs.add(forumCatalog);
                        }
                    } catch (JSONException e) {
                        Log.d("Discuz", "Load Forum Index Failed (" + e.getMessage() + ")");
                    }
                }
                callback.onResponse(data);
            }
        });
    }

    private static int initJobs = 0;

    public static void init(final Runnable callback) {

        Response.Listener<JSONObject> done = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                initJobs --;
                if (initJobs == 0)
                    callback.run();
            }
        };

        initJobs ++;
        getSmileies(done);

        initJobs ++;
        loadForums(done);

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
            if (body.get("allowphoto") == null)
                body.put("allowphoto", 1);
            if (body.get("formhash") == null)
                body.put("formhash", sFormHash);
            if (body.get("mobiletype") == null)
                body.put("mobiletype", 2);
        }
        else if (module.equals("addcomment")) {
            if (body.get("formhash") == null)
                body.put("formhash", sFormHash);
            if (body.get("handlekey") == null)
                body.put("handlekey", "comment");
            if (body.get("commentsubmit") == null)
                body.put("commentsubmit", "yes");
        }
        params.put("module", module);
        if (params.get("submodule") == null)
            params.put("submodule", "checkpost");
        //
        Request request =  new StringRequest(
            body == null ? Request.Method.GET : Request.Method.POST,
            DISCUZ_API + "?" + URLEncodedUtils.format(map2list(params), DISCUZ_ENC),
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    JSONObject data = new JSONObject();
                    try {
                        data = new JSONObject(response);
                    }
                    catch (JSONException e) {
                        Log.e("JSONError", "Parse JSON failed: "+e.getMessage());
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
                        sHasLogined = !var.isNull("auth");

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
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    JSONObject data = new JSONObject();
                    String msg = volleyError.getMessage();
                    // NOTE: getMessage may return null
                    if (msg == null) msg = "Unknown";
                    try {
                        data.put(VOLLEY_ERROR, msg);
                    }
                    catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("VolleyError", msg);
                    callback.onResponse(data);
                }
            }) {
            @Override
            protected Map<String, String> getParams() {
                HashMap<String, String> params = new HashMap<String, String>();
                if (body != null)
                    for (Map.Entry<String, Object> e : body.entrySet())
                        if (e.getValue() != null) params.put(e.getKey(), e.getValue().toString());
                return params;
            }
        };
        ThisApp.requestQueue.add(request);
        return request;
    }

    public static void upload(final Map<String, Object> params,
                              final String filePath,
                              final Response.Listener<String> callback) {

        if (sUploadHash == null || sUid > 0 || filePath == null)
            callback.onResponse(null);

        params.put("module", "forumupload");
        params.put("hash", sUploadHash);
        params.put("uid", sUid);

        LinkedHashMap<String, ContentBody> body = new LinkedHashMap<String, ContentBody>();
        try {
            body.put("hash", new StringBody(sUploadHash));
            body.put("uid", new StringBody(""+sUid));
            if (filePath != null)
                body.put("Filedata", new FileBody(new File(filePath)));
        }
        catch (UnsupportedEncodingException e) {
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

    public static void login(final String username, final String password,
                             final Response.Listener<JSONObject> callback) {
        execute("login", new HashMap<String, Object>() {{
            put("loginsubmit", "yes");
            put("loginfield", "auto");
        }}, new HashMap<String, Object>() {{
            put("username", username);
            put("password", password);
            put("formhash", sFormHash);
        }}, callback);
    }

    // TODO: "Logout" is not found in the api source =.=
    public static void logout(final Response.Listener<JSONObject> callback) {
        sUid = 0;
        sNewMessages = 0;
        sNewPrompts = 0;
        sHasLogined = false;
        ThisApp.cookieStore.removeAll();
        ThisApp.cookieStore.save();
        callback.onResponse(new JSONObject());
    }

}
