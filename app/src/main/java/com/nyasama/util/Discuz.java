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
import android.util.SparseIntArray;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.jakewharton.disklrucache.DiskLruCache;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
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
 * 
 * 这里是整个客户端的核心，所有论坛操作都由这里控制
 * 大体工作思路仍然是：上传request，然后下载所需数，这个工作流程建立在 ThisApp 中的 requestqueue 基础上
 * 看来Discuz！的工作流程也就是接受JSON数据包（无论来自浏览器还是来自客户端）
 * 然后进行处理并显示
 * 
 * 这里的注释将分别对各个函数进行
 * execute 函数以上的部分，基本上都是工具
 * execute 函数一下的部分，则是一些Discuz！的操作
 * execute 则是核心中的核心
 * 
 * JSON数据包的分析是由OFZ完成的
 */
public class Discuz {
    public static final String DISCUZ_HOST = "http://v.ofr.me";
    public static final String DISCUZ_URL = DISCUZ_HOST + "/dz3/";
    public static final String DISCUZ_API = DISCUZ_URL + "api/mobile/index.php";
    public static final String DISCUZ_ENC = "gbk";

    public static final int MAX_COMMENT_LENGTH = 200;

    public static final String VOLLEY_ERROR = "volleyError";

    public static final int NOTIFICATION_ID = 1;

    public static final String BROADCAST_FILTER_LOGIN = "login";

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
//论坛类
    public static class Forum {
        public int id;
        public String name;
        public String icon;
        public int posts;
        public int threads;
        public int todayPosts;
//从JSON中抽取各个论坛属性
        public Forum(JSONObject data) {
            id = Integer.parseInt(data.optString("fid"));
            name = data.optString("name");
            posts = Integer.parseInt(data.optString("posts"));
            threads = Integer.parseInt(data.optString("threads"));
            todayPosts = Integer.parseInt(data.optString("todayposts"));
             /*
            这里比较令人在意，因为论坛的图标是不经常变化的，所以建议下载图标后放入工程作为本地资源使用
            嘛，不过下载几个图标，也不会太费流量
            */
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
//话题类
    public static class Thread {
        public int id;
        public String title;
        public String author;
        public String lastpost;
        public String dateline;
        public int replies;
        public int views;
        public int attachments;
//从JSON中抽取话题的各个属性
        public Thread(JSONObject data) {
            id = Helper.toSafeInteger(data.optString("tid"), 0);
            title = data.optString("subject");
            author = data.optString("author");
            lastpost = data.optString("lastpost");
            dateline = data.optString("dateline");
            // Discuz may return integer as dateline
            int dateVal = Helper.toSafeInteger(lastpost, 0);
            if (dateVal > 0)
                lastpost = Helper.datelineToString(dateVal, null);
            replies = Helper.toSafeInteger(data.optString("replies"), 0);
            views = Helper.toSafeInteger(data.optString("views"), 0);
            attachments = Helper.toSafeInteger(data.optString("attachment"), 0);
        }
    }
//发言类
    public static class Post {
        public int id;
        public int authorId;
        public int number;
        public String author;
        public String message;
        public String dateline;
        public List<Attachment> attachments;
//从JSON中抽取信息
        public Post(JSONObject data) {
            id = Integer.parseInt(data.optString("pid", "0"));
            author = data.optString("author");
            authorId = Integer.parseInt(data.optString("authorid", "0"));
            number = Integer.parseInt(data.optString("number", "0"));
            message = data.optString("message");
            dateline = data.optString("dateline");

            attachments = new ArrayList<Attachment>();
            JSONObject attachlist = data.optJSONObject("attachments");
            if (attachlist != null) {
                for (Iterator<String> iter = attachlist.keys(); iter.hasNext(); ) {
                    String key = iter.next();
                    JSONObject attachData = attachlist.optJSONObject(key);
                    Attachment attachment = new Attachment(attachData);
                    // Note: have to check this because Discuz may return invalid attachment data =.=
                    if (attachment.id > 0)
                        attachments.add(attachment);
                }
            }
        }
    }
//上面的Post类中用到的 Attachment 类，有图片附件或者String
    public static class Attachment {
        public int id;
        public boolean isImage;
        public String name;
        public String src;
        public String size;

        public Attachment() {
        }
//从JSON中提取附加图片的URL
        public Attachment(JSONObject data) {
            id = Integer.parseInt(data.optString("aid", "0"));
            name = data.optString("filename");
            src = data.optString("url") + data.optString("attachment");
            size = data.optString("attachsize");
            // Note: Discuz may set isimage 1 or -1
            isImage = !"0".equals(data.optString("isimage"));
        }
    }
//点评类
    public static class Comment {
        public int authorId;
        public String author;
        public String comment;
//抽取点评
        public Comment(JSONObject data) {
            authorId = Integer.parseInt(data.optString("authorid"));
            author = data.optString("author");
            comment = data.optString("comment");
        }
//创建点评
        public Comment(int authorId, String author, String comment) {
            this.authorId = authorId;
            this.author = author;
            this.comment = comment;
        }
    }
//私信列表
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
//依然是从JSON中提取私信信息
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
//通知类
    public static class Notice {
        public int id;
        public String type;
        public String note;
        public String dateline;
//抽取通知信息
        public Notice(JSONObject data) {
            id = Helper.toSafeInteger(data.optString("id"), 0);
            type = data.optString("type");
            note = data.optString("note");
            if (data.has("dateline")) dateline = Helper.datelineToString(
                    Integer.parseInt(data.optString("dateline")), null);
        }
    }
//收藏类
    public static class FavItem {
        public int id;
        public String type;
        public int dataId;
        public String title;
        public String dateline;
//抽取信息
        public FavItem(JSONObject data) {
            id = Helper.toSafeInteger(data.optString("favid"), 0);
            type = data.optString("idtype");
            dataId = Helper.toSafeInteger(data.optString("id"), 0);
            title = data.optString("title");
            if (data.has("dateline")) dateline = Helper.datelineToString(
                    Integer.parseInt(data.optString("dateline")), null);
        }
    }
//投票类
    public static class PollOption {
        public int id;
        public String option;
        public int votes;
        public double percent;
//抽取信息并整理
        public PollOption(JSONObject data) {
            id = Helper.toSafeInteger(data.optString("polloptionid"), 0);
            option = data.optString("polloption");
            votes = Helper.toSafeInteger(data.optString("votes"), 0);
            percent = Helper.toSafeDouble(data.optString("percent"), 0);
        }
    }

//表情组
/*
表情获得是一个非常复杂的流程，大概可以描述为：
1.有什么人调用了全局函数LoadSmileies 附带一个 response.listener
2.于是getSmileies 就创建了一个Stringrequest，发到discuz 的 common_smilies_var.js 那里进行处理
3.common_smilies_var.js 理解这边的意图后，发回来一个String，里面装着smilies_type ; smilies_array 两个信息
  实质上也包含了所有表情的名称和URL，但是返回的String 还要经过编码转化（gb18030）才能用
4.得到字符串并完成编码转化后，调用parseSmileyString进行字符串的解析
5.字符串的解析过程是用ThisApp 中的webview，调用javascript 完成的，之所以这么做是想用webview 来显示表情
  于是在webview中，把这个字符串转化成了JSON
6.然而JSON的解析则要回到java中完成，于是创建了JSInterface 类以便让javascript 调用其中的setSmilies 函数
7.但是setSmilies 本身不解析JSON，它把解析JSON 的工作交给parseSmilies 完成
8.parseSmilies 就完全是一般的JSON 解析函数了，它解析之后把信息放入事先定义的sSmilies 中
9.以上全部完成之后，setSmilies会通过调用onResponse函数通知调用getSmileies 的组件，说载入表情已经完成
  都装在sSmilies 里，用getSmilies 获得

  具体见函数注释
*/
    public static class Smiley {
        public String code;
        public String image;

        // cache
        private static BitmapLruCache smileyCache = new BitmapLruCache();
        public static BitmapLruCache getCache() {
            return smileyCache;
        }

        // helper
        public static boolean isSmileyUrl(String url) {
            return url.startsWith(DISCUZ_URL + "static/image/smiley/");
        }
    }

    public static class SmileyGroup {
        public String name;
        public String path;
        public List<Smiley> list;

        // saved list
        private static List<SmileyGroup> sSmilies;
        private static Response.Listener<List<SmileyGroup>> mSmiliesCallback = null;

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
                    if (mSmiliesCallback != null)
                        mSmiliesCallback.onResponse(null);
                }
            }) {
                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    try {
                        return Response.success(new String(response.data, DISCUZ_ENC), getCacheEntry());
                    } catch (UnsupportedEncodingException e) {
                        return Response.error(new VolleyError("decode failed!"));
                    }
                }
            };
            ThisApp.requestQueue.add(request);
        }

        // helpers
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

        //JS接口类
        private static class JSInterface {
            //这句会使这个方法暴露给javascript代码，使其能被调用
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

        //content中含有smilies_type 和smilies_array 两个域
        //因此才能在javascript中调用
        //调用JSinterface 的语句位于443行
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

    public static class ForumThreadInfo {
        public String name;
        public ThreadTypes types;

        public ForumThreadInfo(JSONObject data) {
            name = data.optString("name");
            JSONObject threadtypes = data.optJSONObject("threadtypes");
            if (threadtypes != null && !threadtypes.isNull("types"))
                types = new ThreadTypes(threadtypes.optJSONObject("types"));
        }

        private static SparseArray<ForumThreadInfo> sForumThreadInfo;

        public static SparseArray<ForumThreadInfo> getInfo() {
            return sForumThreadInfo;
        }

        public static void loadInfo(final Response.Listener<SparseArray<ForumThreadInfo>> callback) {
            if (sForumThreadInfo != null) {
                callback.onResponse(sForumThreadInfo);
                return;
            }
            Discuz.execute("forumnav", new HashMap<String, Object>(), null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject data) {
                            JSONObject var = data.optJSONObject("Variables");
                            if (var == null) return;
                            sForumThreadInfo = new SparseArray<ForumThreadInfo>();
                            JSONArray forums = var.optJSONArray("forums");
                            for (int i = 0; i < forums.length(); i++) {
                                JSONObject forum = forums.optJSONObject(i);
                                int fid = Helper.toSafeInteger(forum.optString("fid"), 0);
                                sForumThreadInfo.put(fid, new ForumThreadInfo(forum));
                            }
                            callback.onResponse(sForumThreadInfo);
                        }
                    });
        }
    }

    public static String sFormHash = "";
    public static String sUploadHash = "";
    public static String sUsername = "";
    public static String sGroupName = "";
    public static int sUid = 0;
    public static int sGid = 0;
    public static int sNewMessages = 0;
    public static int sNewPrompts = 0;
    public static boolean sHasLogined;
    public static boolean sIsModerator;

    // REF: Discuz\src\net\discuz\json\helper\x25\ViewThreadParseHelperX25.java
    //根据Discuz API 定义的获得缩略图URL 的函数
    public static String getAttachmentThumb(int attachmentId, String size) {
        String key = Helper.toSafeMD5(attachmentId + "|" + size.replace('x', '|'));
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("module", "forumimage");
        params.put("aid", attachmentId);
        params.put("version", 2);
        params.put("size", size);
        params.put("key", key);
        params.put("type", "fixnone");
        return DISCUZ_API + "?" + URLEncodedUtils.format(map2list(params), DISCUZ_ENC);
    }

    //根据Discuz API 定义的获得缩略图URL 的函数
    public static String getThreadCoverThumb(int threadId) {
        return DISCUZ_API + "?module=threadcover&tid=" + threadId + "&version=2";
    }

    //检查并修改URL 格式
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

    static SparseIntArray maxUploadSize = new SparseIntArray() {{
        put(1, 2048*1024);
        put(2, 500*1024);
        put(3, 1024*1024);
        put(38, 1024*1024);
        put(32, 16*1024*1024);
        put(43, 500*1024);
        put(47, 999*1024);

        put(22, 2048*1024);
        put(42, 1024*1024);
        put(45, 500*1024);

        put(11, 500*1024);
        put(11, 700*1024);
        put(12, 900*1024);
        put(13, 1170*1024);
        put(14, 1460*1024);
        put(15, 1760*1024);
        put(20, 2048*1024);
        put(21, 2048*1024);
        put(39, 2048*1024);
    }};
    public static int getMaxUploadSize() {
        return maxUploadSize.get(sGid);
    }

    private static class ResponseListener implements Response.Listener<String> {
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
                sUid = Helper.toSafeInteger(var.optString("member_uid", "0"), 0);
                sGid = Helper.toSafeInteger(var.optString("groupid", "0"), 0);
                sIsModerator = !"0".equals(var.optString("ismoderator"));
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

    private static class ResponseErrorListener implements Response.ErrorListener {
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

/*
核心中的核心，execute 函数
还是比较朴素的
module决定调用哪种操作
根据body是否为空决定GET还是POST
如果是POST方法，其参数放在body当中
然后对接受到的JSON进行解析，并返回数据
具体用途得参见用例才能明确
不过具体的param和body到底有哪些职责，哪些域，怎样解析，恐怕只有OFZ知道了
*/
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
        } else if (module.equals("topicadmin")) {
            Helper.putIfNull(params, "modsubmit", "yes");
            Helper.putIfNull(body, "formhash", sFormHash);
            Helper.putIfNull(body, "modsubmit", "true");
        } else if (module.equals("friendcp")) {
            Helper.putIfNull(body, "formhash", sFormHash);
            Helper.putIfNull(body, "add2submit", "yes");
        } else if (module.equals("buythread")) {
            Helper.putIfNull(body, "formhash", sFormHash);
            Helper.putIfNull(body, "paysubmit", "yes");
        } else if (module.equals("plugin")) {
            Helper.putIfNull(params, "version", 4);
        }
        params.put("module", module);
        Helper.putIfNull(params, "submodule", "checkpost");

        Request request;
        String url = DISCUZ_API + "?" + URLEncodedUtils.format(map2list(params), DISCUZ_ENC);
        if (module.equals("editpost") && body != null) {
            Map<String, ContentBody> contentBody = new HashMap<String, ContentBody>();
            try {
                //把body中的参数转入一个新的params map中，返回
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
            //创建Stringrequest ，详细文档：http://afzaln.com/volley/
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

                //这个getparam函数用来获得当方法为POST时，需要的参数
                @Override
                protected Map<String, String> getParams() {
                    HashMap<String, String> params = new HashMap<String, String>();
                    //把body中的参数转入一个新的params map中，返回
                    if (body != null)
                        for (Map.Entry<String, Object> e : body.entrySet())
                            if (e.getValue() != null)
                                params.put(e.getKey(), e.getValue().toString());
                    return params;
                }

                @Override
                protected String getParamsEncoding() {
                    //返回参数的编码方式
                    return DISCUZ_ENC;
                }
            };
            // tell volley NOT TO RETRY POST request (solve #60)
            // REF: http://stackoverflow.com/questions/26264942/android-volley-makes-2-requests-to-the-server-when-retry-policy-is-set-to-0
            if (body != null)
                request.setRetryPolicy(new DefaultRetryPolicy(30000, 0,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
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

    public interface DownloadProgressListener {
        boolean onResponse(int progress);
    }

    public static InputStream getCache(final String cacheKey) {
        try {
            if (cacheKey.startsWith("file:")) {
                return new FileInputStream(cacheKey.substring("file:".length()));
            }
            else {
                DiskLruCache.Snapshot snapshot = ThisApp.fileDiskCache.get(cacheKey);
                if (snapshot == null) return null;
                return snapshot.getInputStream(0);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void download(final String url,
                              final String cacheKey,
                              final Response.Listener<String> callback,
                              final DownloadProgressListener process) {
        AsyncTask task = new AsyncTask<Object, Integer, String>() {
            boolean mCanceled = false;
            @Override
            protected String doInBackground(Object... objects) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(30000);

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
                        return "http get failed";
                    if (mCanceled) // check this because it takes long to go on
                        return "canceled";

                    int contentLength = conn.getContentLength();
                    if (mCanceled) // check this because it takes long to go on
                        return "canceled";

                    OutputStream output;
                    DiskLruCache.Editor editor = null;
                    File tmpFile = null;
                    File saveFile = null;
                    if (cacheKey.startsWith("file:")) {
                        tmpFile = File.createTempFile("nyasama_", "downloading", ThisApp.context.getExternalCacheDir());
                        saveFile = new File(cacheKey.substring("file:".length()));
                        output = new FileOutputStream(tmpFile);
                        saveFile.getParentFile().mkdirs();
                    }
                    else {
                        editor = ThisApp.fileDiskCache.edit(cacheKey);
                        if (editor == null)
                            return "open cache failed";
                        output = editor.newOutputStream(0);
                    }

                    InputStream input = conn.getInputStream();
                    int count, recvd = 0;
                    byte data[] = new byte[16 * 1024];
                    while (!mCanceled && (count = input.read(data)) >= 0) {
                        recvd += count;
                        publishProgress(recvd * 100 / contentLength);
                        output.write(data, 0, count);
                    }

                    input.close();
                    output.close();
                    conn.disconnect();

                    if (editor != null) {
                        if (mCanceled)
                            editor.abort();
                        else
                            editor.commit();
                    }
                    if (tmpFile != null) {
                        if (mCanceled)
                            tmpFile.delete();
                        else
                            tmpFile.renameTo(saveFile);
                    }

                    return mCanceled ? "calceled" : null;
                }
                catch (Throwable e) {
                    return e.getMessage();
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                mCanceled = process.onResponse(values[0]);
            }

            @Override
            protected void onPostExecute(String s) {
                callback.onResponse(s);
            }
        };
        task.execute();
    }

    static Pattern searchMessagePatt = Pattern.compile("<div id=\"messagetext\" class=\"alert_info\">\\s*<p>(.*?)<",
            Pattern.DOTALL | Pattern.MULTILINE);
    public static void search(final String text,
                              final Map<String, Object> params,
                              final Response.Listener<JSONObject> callback) {
        final Request request = new StringRequest(
                Request.Method.POST,
                // set #noredirect# so volley will throw error 302
                DISCUZ_URL + "search.php?searchsubmit=yes##hurlstack:noredirect#",
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
//login函数是基于execute 定义的，因为body不为空，所以是POST方法
    public static void login(final String username, final String password,
                             final int questionId, final String answer,
                             final String sechash, final String seccode,
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
            if (sechash != null) {
                put("seccodehash", sechash);
                put("seccodeverify", seccode);
            }
        }}, callback);
    }

    // Note: "Logout" is not found in the api source =.=
    //因为api中没有logout，于是就清除一下本地数据就行了
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

//signin 并没有调用execute，而是自己创建了一个request 加入requestqueuue
//返回的String 看起来是HTML代码，于是用正则匹配"<p>(.*?)</p>" 来找到是否申请成功
    static Pattern signinMessagePattern = Pattern.compile("showDialog\\('(.*?)'");
    public static void signin(final Response.Listener<String> callback) {
        String url = DISCUZ_URL + "plugin.php?id=dsu_amupper&ppersubmit=true&formhash="+sFormHash+"&infloat=yes&handlekey=dsu_amupper&inajax=1&ajaxtarget=fwin_content_dsu_amupper";
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

    //
    // helpers
    //
    //把hashmap转化成 name value pair 链表的函数
    //是为了方便做成JSON数据包而定义的，下面的函数有用到
    private static List<NameValuePair> map2list(Map<String, Object> map) {
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        for (Map.Entry<String, Object> e : map.entrySet())
            list.add(new BasicNameValuePair(e.getKey(),
                    e.getValue() != null ? e.getValue().toString() : ""));
        return list;
    }

    //发送通知
    private static void notifyNewMessage() {
        //这里决定跳到哪个界面
        Class activityClass = UserProfileActivity.class;
        if (sNewMessages > 0 && sNewPrompts == 0)
            activityClass = PMListActivity.class;
        else if (sNewMessages == 0 && sNewPrompts > 0)
            activityClass = NoticeActivity.class;

        Context context = ThisApp.context;
        Intent intents[] = {new Intent(context, activityClass)};
        //pendingintent 是自带操作的intent，这里是打开activity操作
        PendingIntent pendingIntent = PendingIntent.getActivities(context,
                0,
                intents,
                PendingIntent.FLAG_CANCEL_CURRENT);
        String text = context.getString(R.string.prompt_1) + " " +
                (sNewMessages > 0 ? sNewMessages + " " + context.getString(R.string.prompt_2) : "") +
                (sNewPrompts > 0 ? (sNewMessages > 0 ? " " + context.getString(R.string.prompt_3) + " " : "") + sNewPrompts + " " + context.getString(R.string.prompt_4) : "");
        //notification 在android 3.0 之后的推荐用法：用Builder创建
        //详细文档：http://developer.android.com/guide/topics/ui/notifiers/notifications.html
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
}
