package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.CallbackMatcher;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.Post;
import com.nyasama.util.Discuz.Comment;
import com.nyasama.util.Discuz.Attachment;

import com.nyasama.util.Helper;
import com.nyasama.util.HtmlImageGetter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostListActivity extends FragmentActivity
    implements CommonListFragment.OnListFragmentInteraction<Post> {

    private final int PAGE_SIZE_COUNT = 10;
    private final int COMMENT_PAGE_SIZE = 10;
    private final int MAX_TRIMSTR_LENGTH = 30;
    private final String TAG = "PostList";

    private CommonListFragment<Post> mListFragment;
    private SparseArray<List<Comment>> mComments = new SparseArray<List<Comment>>();
    private SparseArray<Integer> mCommentCount = new SparseArray<Integer>();

    private AlertDialog mReplyDialog;
    private AlertDialog mCommentDialog;

    public void doReply(final String text, final String trimstr) {
        Discuz.execute("sendreply", new HashMap<String, Object>() {{
            put("tid", getIntent().getIntExtra("tid", 0));
            put("replysubmit", "yes");
        }}, new HashMap<String, Object>() {{
            put("message", text);
            if (trimstr != null)
                put("noticetrimstr", trimstr);
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (mReplyDialog != null)
                    mReplyDialog.dismiss();
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else if (data.opt("Message") instanceof JSONObject) {
                    JSONObject message = data.optJSONObject("Message");
                    String messageval = message.optString("messageval");
                    if ("post_reply_succeed".equals(messageval)) {
                        mListFragment.reloadLast();
                    }
                    else if ("replyperm_login_nopermission//1".equals(messageval)) {
                        startActivity(new Intent(PostListActivity.this, LoginActivity.class));
                    }
                    else
                        Helper.toast(message.optString("messagestr"));
                }
            }
        });
    }

    public void doComment(final int pid, final String comment) {
        Discuz.execute("addcomment", new HashMap<String, Object>() {{
            put("tid", getIntent().getIntExtra("tid", 0));
            put("pid", pid);
        }}, new HashMap<String, Object>() {{
            put("message", comment);
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                mCommentDialog.dismiss();
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else if (data.opt("Message") instanceof JSONObject) {
                    JSONObject message = data.optJSONObject("Message");
                    String messageval = message.optString("messageval");
                    if ("comment_add_succeed".equals(messageval)) {
                        List<Comment> comments = mComments.get(pid);
                        if (comments == null)
                            mComments.put(pid, comments = new ArrayList<Comment>());
                        comments.add(0, new Comment(Discuz.sUid, Discuz.sUsername, comment));
                        mCommentCount.put(pid, comments.size());
                        mListFragment.getListAdapter().notifyDataSetChanged();
                    }
                    else
                        Helper.toast(message.optString("messagestr"));
                }
            }
        });
    }

    public void doLoadComment(final int pid, final int page) {
        Discuz.execute("morecomment", new HashMap<String, Object>() {{
            put("tid", getIntent().getIntExtra("tid", 0));
            put("pid", pid);
            put("page", page + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                mCommentCount.put(pid, Integer.MAX_VALUE);
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else if (data.opt("Variables") instanceof JSONObject) {
                    JSONObject var = data.optJSONObject("Variables");
                    if (var.opt("comments") instanceof JSONArray) {
                        JSONArray commentList = var.optJSONArray("comments");
                        List<Comment> comments = mComments.get(pid);
                        // remove duplicated items
                        if (page * COMMENT_PAGE_SIZE < comments.size())
                            comments.subList(page * COMMENT_PAGE_SIZE, comments.size()).clear();
                        for (int i = 0; i < commentList.length(); i ++) {
                            comments.add(new Comment(commentList.optJSONObject(i)));
                        }
                        mCommentCount.put(pid, Helper.toSafeInteger(var.optString("count"), 0));
                        mListFragment.getListAdapter().notifyDataSetChanged();
                    }
                }
            }
        });
    }

    public void doMarkFavourite() {
        Discuz.execute("favthread", new HashMap<String, Object>() {{
            put("id", getIntent().getIntExtra("tid", 0));
        }}, new HashMap<String, Object>() {{
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else if (data.opt("Message") instanceof JSONObject) {
                    JSONObject message = data.optJSONObject("Message");
                    Helper.toast(message.optString("messagestr"));
                }
            }
        });
    }

    public void quickReply(final Post item) {
        final EditText input = new EditText(this);
        mReplyDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.diag_quick_reply_title)
                .setMessage(R.string.diag_hint_type_something)
                .setView(input)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        mReplyDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                mReplyDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String text = input.getText().toString();
                            if (!text.isEmpty()) {
                                Helper.disableDialog(mReplyDialog);
                                doReply(text, item == null ? null : getTrimstr(item));
                            }
                        }
                    });
            }
        });
        mReplyDialog.show();
    }

    public void addComment(Post item) {
        final int pid = item.id;
        final EditText input = new EditText(this);
        mCommentDialog = new AlertDialog.Builder(this)
                .setTitle("AddComment")
                .setMessage("Type Something")
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        mCommentDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                mCommentDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String text = input.getText().toString();
                            if (!text.isEmpty()) {
                                Helper.disableDialog(mCommentDialog);
                                doComment(pid, text);
                            }
                        }
                    });
            }
        });
        mCommentDialog.show();
    }

    public void loadComment(Post item) {
        int count = mCommentCount.get(item.id);
        // give up if it's loading now
        if (count < 0)
            return;

        final int pid = item.id;
        List<Comment> comments = mComments.get(pid);
        if (comments.size() >= count)
            return;

        // set count to negative
        mCommentCount.put(pid, -1);
        doLoadComment(pid, (int)Math.floor(comments.size()/10));
    }

    public void gotoReply(final Post item) {
        startActivityForResult(new Intent(PostListActivity.this, NewPostActivity.class) {{
            putExtra("tid", PostListActivity.this.getIntent().getIntExtra("tid", 0));
            if (item != null) {
                putExtra("thread_title", "Re: " + item.author + " #" + mListFragment.getIndex(item));
                putExtra("notice_trimstr", getTrimstr(item));
            }
            else {
                putExtra("thread_title", "Re: " + getTitle());
            }
        }}, Discuz.REQUEST_CODE_REPLY);
    }

    public void showMenu(View view, final Post item) {
        PopupMenu menu = new PopupMenu(PostListActivity.this, view);
        menu.getMenuInflater().inflate(R.menu.menu_post_item, menu.getMenu());

        boolean showLoadCommentMenu = mComments.get(item.id) != null &&
                mComments.get(item.id).size() >= COMMENT_PAGE_SIZE &&
                mComments.get(item.id).size() < mCommentCount.get(item.id);
        menu.getMenu().findItem(R.id.action_more_comment).setVisible(showLoadCommentMenu);

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int action = menuItem.getItemId();
                if (action == R.id.action_comment) {
                    addComment(item);
                }
                else if (action == R.id.action_more_comment) {
                    loadComment(item);
                }
                else if (action == R.id.action_quick_reply) {
                    quickReply(item);
                }
                else if (action == R.id.action_reply) {
                    gotoReply(item);
                }
                return true;
            }
        });
        menu.show();
    }

    static Pattern msgPathPattern = Pattern.compile("<img[^>]* file=\"(.*?)\"");
    static CallbackMatcher msgMatcher = new CallbackMatcher("<ignore_js_op>(.*?)</ignore_js_op>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    // this function compiles the message to display in android TextViews
    String compileMessage(String message, final List<Attachment> attachments) {

        final Map<String, Attachment> srcAttachMap = new HashMap<String, Attachment>();
        for (Attachment attachment : attachments)
            srcAttachMap.put(attachment.src, attachment);

        message = msgMatcher.replaceMatches(message, new CallbackMatcher.Callback() {
            @Override
            public String foundMatch(MatchResult matchResult) {
                Matcher pathMatcher = msgPathPattern.matcher(matchResult.group(1));
                if (pathMatcher.find()) {
                    String src = pathMatcher.group(1);
                    Attachment attachment = srcAttachMap.get(src);
                    if (attachment != null)
                        return "<img src=\"" + Discuz.getImageThumbUrl(attachment.id) + "\" />";
                }
                Log.w(TAG, "attachment image not found (" + matchResult.group(1) + ")");
                return "";
            }
        });

        message = message.replaceAll(" file=\"(.*?)\"", " src=\"$1\"");

        return message;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        String title = getIntent().getStringExtra("title");
        if (title != null) setTitle(title);

        mListFragment = CommonListFragment.getNewFragment(
                Post.class,
                R.layout.fragment_simple_list,
                R.layout.fragment_post_item,
                R.id.list);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mListFragment)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == Discuz.REQUEST_CODE_REPLY) {
            if (resultCode > 0)
                mListFragment.reloadLast();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_post_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == R.id.action_quick_reply) {
            quickReply(null);
            return true;
        }
        else if (id == R.id.action_reply) {
            gotoReply(null);
            return true;
        }
        else if (id == R.id.action_mark_fav) {
            doMarkFavourite();
            return true;
        }
        else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Pattern patt1 = Pattern.compile("<span style=\"display:none\">.*?</span>", Pattern.DOTALL);
    private Pattern patt2 = Pattern.compile("<.+quote>.+div>", Pattern.DOTALL);
    private Pattern patt3 = Pattern.compile("<[^<>]*>", Pattern.DOTALL);
    private String getTrimstr(Post post) {
        // Note: see Discuz source net/discuz/source/PostSender.java
        int tid = getIntent().getIntExtra("tid", 0);
        String message = post.message;
        message = patt1.matcher(message).replaceAll("");
        message = Html.fromHtml(message).toString();
        message = patt2.matcher(message).replaceAll("");
        message = patt3.matcher(message).replaceAll("");
        if (message.length() > MAX_TRIMSTR_LENGTH)
            message = message.substring(0, MAX_TRIMSTR_LENGTH - 3) + "...";
        return "[quote]"+
            "[size=2]"+
                "[color=#999999]"+post.author+" at "+Html.fromHtml(post.dateline)+"[/color] "+
                "[url=forum.php?mod=redirect&goto=findpost&pid="+post.id+"&ptid="+tid+
                    "][img]static/image/common/back.gif[/img][/url]"+
            "[/size]\n"+
            message+
        "[/quote]";
    }

    final Map<String, Bitmap> imageCache = new HashMap<String, Bitmap>();
    final SparseArray<List<View>> commentsCache = new SparseArray<List<View>>();
    @Override
    public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
        return new CommonListAdapter<Post>() {
            @Override
            public void convertView(ViewHolder viewHolder, final Post item) {
                String avatar_url = Discuz.DISCUZ_URL +
                        "uc_server/avatar.php?uid="+item.authorId+"&size=small";
                NetworkImageView avatar = (NetworkImageView) viewHolder.getView(R.id.avatar);
                avatar.setImageUrl(avatar_url, ThisApp.imageLoader);
                avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(PostListActivity.this, UserProfileActivity.class) {{
                            putExtra("uid", item.authorId);
                        }});
                    }
                });

                viewHolder.setText(R.id.author, item.author);
                viewHolder.setText(R.id.date, Html.fromHtml(item.dateline));
                viewHolder.setText(R.id.index, "#"+item.number);

                viewHolder.getView(R.id.menu).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showMenu(view, item);
                    }
                });

                TextView messageText = (TextView) viewHolder.getView(R.id.message);
                messageText.setText(Html.fromHtml(item.message,
                        new HtmlImageGetter(messageText, imageCache), null));
                messageText.setMovementMethod(LinkMovementMethod.getInstance());

                // load comments
                LinearLayout commentList = (LinearLayout) viewHolder.getView(R.id.comment_list);
                commentList.removeAllViews();
                List<View> cachedViews = commentsCache.get(item.id);
                if (cachedViews == null) {
                    cachedViews = new ArrayList<View>();
                    commentsCache.put(item.id, cachedViews);
                }
                List<Comment> comments = mComments.get(item.id);
                if (comments != null) {
                    LayoutInflater inflater = getLayoutInflater();
                    for (int i = 0; i < comments.size(); i ++) {
                        Comment comment = comments.get(i);
                        View commentView;
                        if (i < cachedViews.size()) {
                            commentView = cachedViews.get(i);
                        }
                        else {
                            commentView = inflater
                                .inflate(R.layout.fragment_comment_item, commentList, false);
                            cachedViews.add(commentView);
                        }
                        ((TextView) commentView.findViewById(R.id.author)).setText(comment.author);
                        ((TextView) commentView.findViewById(R.id.comment)).setText(comment.comment);
                        commentList.addView(commentView);
                    }
                }

                // show attachments
                TextView attachments = (TextView) viewHolder.getView(R.id.attachment_list);
                Helper.updateVisibility(attachments, item.attachments.size() > 0);
                attachments.setText(getString(R.string.text_view_attachments) +
                        " (" + item.attachments.size() + ")");
                attachments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(ThisApp.context, AttachmentViewer.class) {{
                            putExtra("tid", PostListActivity.this.getIntent().getIntExtra("tid", 0));
                            putExtra("index", mListFragment.getIndex(item));
                        }});
                    }
                });
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        // TODO:
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final List listData) {
        final int page = (int) Math.round(Math.floor(listData.size() / PAGE_SIZE_COUNT));
        final int position = page * PAGE_SIZE_COUNT;
        Discuz.execute("viewthread", new HashMap<String, Object>() {{
            put("tid", getIntent().getIntExtra("tid", 0));
            put("ppp", PAGE_SIZE_COUNT);
            put("page", page + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else if (data.opt("Message") instanceof JSONObject) {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        listData.clear();
                        new AlertDialog.Builder(PostListActivity.this)
                                .setTitle(R.string.there_is_something_wrong)
                                .setMessage(message.getString("messagestr"))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        finish();
                                    }
                                })
                                .show();
                        total = 0;
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                else {
                    // remove possible duplicated items
                    if (position < listData.size())
                        listData.subList(position, listData.size()).clear();
                    try {
                        JSONObject var = data.getJSONObject("Variables");

                        JSONArray postlist = var.getJSONArray("postlist");
                        for (int i = 0; i < postlist.length(); i ++) {
                            JSONObject postData = postlist.getJSONObject(i);
                            Post post = new Post(postData);
                            // Note: replace attachments
                            post.message = compileMessage(post.message, post.attachments);
                            listData.add(post);
                        }

                        // Note: in x2 there is only "replies"
                        JSONObject thread = var.getJSONObject("thread");
                        total = Integer.parseInt(thread.has("replies") ?
                                thread.getString("replies") : thread.getString("allreplies")) + 1;
                        setTitle(thread.getString("subject"));

                        // comments
                        if (var.opt("comments") instanceof JSONObject) {
                            JSONObject comments = var.getJSONObject("comments");
                            for(Iterator<String> iter = comments.keys(); iter.hasNext(); ) {
                                String key = iter.next();
                                JSONArray commentListData = comments.getJSONArray(key);
                                int pid = Integer.parseInt(key);
                                List<Comment> commentList = new ArrayList<Comment>();
                                for (int i = 0; i < commentListData.length(); i ++) {
                                    JSONObject commentData = commentListData.getJSONObject(i);
                                    commentList.add(new Comment(commentData));
                                }
                                mComments.put(pid, commentList);
                                mCommentCount.put(pid, Integer.MAX_VALUE);
                            }
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                mListFragment.loadMoreDone(total);
            }
        });
    }
}
