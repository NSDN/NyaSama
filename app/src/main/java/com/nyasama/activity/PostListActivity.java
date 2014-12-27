package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.BitmapLruCache;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.CallbackMatcher;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.Post;
import com.nyasama.util.Discuz.Comment;
import com.nyasama.util.Discuz.Attachment;
import com.nyasama.util.Discuz.PollOption;

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

    public static final int REQUEST_CODE_EDIT_POST = 1;
    public static final int REQUEST_CODE_REPLY_THREAD = 2;

    private static final int PAGE_SIZE_COUNT = 10;
    private static final int COMMENT_PAGE_SIZE = 10;
    private static final int MAX_TRIMSTR_LENGTH = 30;

    private CommonListFragment<Post> mListFragment;
    Map<String, Attachment> mAttachmentMap = new HashMap<String, Attachment>();
    private SparseArray<List<Comment>> mComments = new SparseArray<List<Comment>>();
    private SparseArray<Integer> mCommentCount = new SparseArray<Integer>();
    private List<PollOption> mPollOptions = new ArrayList<PollOption>();
    private int mForumId;

    private AlertDialog mReplyDialog;
    private AlertDialog mCommentDialog;
    private AlertDialog mVoteDialog;

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

    public void doPollVote(final int vid) {
        // TODO: finish this
        mVoteDialog.dismiss();
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
                        Helper.setListLength(comments, page * COMMENT_PAGE_SIZE);
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
                } else if (data.opt("Message") instanceof JSONObject) {
                    JSONObject message = data.optJSONObject("Message");
                    Helper.toast(message.optString("messagestr"));
                }
            }
        });
    }

    public void editPost(Post item) {
        Intent intent = new Intent(this, NewPostActivity.class);
        intent.putExtra("tid", getIntent().getIntExtra("tid", 0));
        intent.putExtra("pid", item.id);
        startActivityForResult(intent, REQUEST_CODE_EDIT_POST);
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
                .setTitle(R.string.action_comment)
                .setMessage(R.string.diag_hint_type_something)
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
        doLoadComment(pid, (int) Math.floor(comments.size() / 10));
    }

    public void gotoReply(final Post item) {
        startActivityForResult(new Intent(PostListActivity.this, NewPostActivity.class) {{
            putExtra("tid", PostListActivity.this.getIntent().getIntExtra("tid", 0));
            if (item != null) {
                putExtra(NewPostActivity.ARG_POST_TITLE, "Re: " + item.author + " #" + mListFragment.getIndex(item));
                putExtra(NewPostActivity.ARG_POST_TRIMSTR, getTrimstr(item));
            }
            else {
                putExtra(NewPostActivity.ARG_POST_TITLE, "Re: " + getTitle());
            }
        }}, REQUEST_CODE_REPLY_THREAD);
    }

    public void showMenu(View view, final Post item) {
        PopupMenu menu = new PopupMenu(PostListActivity.this, view);
        menu.getMenuInflater().inflate(R.menu.menu_post_item, menu.getMenu());

        boolean showLoadCommentMenu = mComments.get(item.id) != null &&
                mComments.get(item.id).size() >= COMMENT_PAGE_SIZE &&
                mComments.get(item.id).size() < mCommentCount.get(item.id);
        menu.getMenu().findItem(R.id.action_more_comment).setVisible(showLoadCommentMenu);

        boolean showEditPostMenu = item.author.equals(Discuz.sUsername);
        menu.getMenu().findItem(R.id.action_edit).setVisible(showEditPostMenu);

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int action = menuItem.getItemId();
                if (action == R.id.action_edit) {
                    editPost(item);
                }
                else if (action == R.id.action_comment) {
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

    public void showPollOptions() {
        ListView listView = new ListView(this);
        listView.setAdapter(new CommonListAdapter<PollOption>(mPollOptions,
                android.R.layout.simple_list_item_1) {
            @Override
            public void convertView(ViewHolder viewHolder, PollOption item) {
                ((TextView) viewHolder.getConvertView())
                        .setText(item.option + " " + item.votes + " votes (" + item.percent + "%)");
            }
        });
        mVoteDialog = new AlertDialog.Builder(this)
                .setTitle("Vote Results")
                .setView(listView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                doPollVote(mPollOptions.get(i).id);
            }
        });
        mVoteDialog.show();
    }

    static Pattern msgPathPattern = Pattern.compile("<img[^>]* file=\"(.*?)\"");
    static CallbackMatcher msgMatcher = new CallbackMatcher("<ignore_js_op>(.*?)</ignore_js_op>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    // this function compiles the message to display in android TextViews
    static String compileMessage(String message, final Map<String, Attachment> attachments) {
        message = msgMatcher.replaceMatches(message, new CallbackMatcher.Callback() {
            @Override
            public String foundMatch(MatchResult matchResult) {
                Matcher pathMatcher = msgPathPattern.matcher(matchResult.group(1));
                if (pathMatcher.find()) {
                    String src = pathMatcher.group(1);
                    Attachment attachment = attachments.get(src);
                    if (attachment != null) {
                        String url = Discuz.getAttachmentThumb(attachment.id);
                        attachments.put(url, attachment);
                        return "<img src=\"" + url + "\" />";
                    }
                }
                Log.w(PostListActivity.class.toString(),
                        "attachment image not found (" + matchResult.group(1) + ")");
                return "";
            }
        });

        message = message.replaceAll(" file=\"(.*?)\"", " src=\"$1\"");

        return message;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_framelayout);
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
        if (requestCode == REQUEST_CODE_EDIT_POST) {
            if (resultCode > 0) mListFragment.reloadAll();
        }
        else if (requestCode == REQUEST_CODE_REPLY_THREAD) {
            if (resultCode > 0) mListFragment.reloadLast();
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
        int id = item.getItemId();
        if (id == R.id.action_quick_reply) {
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
        else if (id == R.id.action_goto_forum) {
            if (mForumId > 0 && getIntent().getIntExtra("fid", 0) != mForumId)
                startActivity(new Intent(this, ThreadListActivity.class) {{
                    putExtra("fid", mForumId);
                }});
            finish();
        }
        return Helper.handleOption(this, item.getItemId()) ||
                super.onOptionsItemSelected(item);
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

    final BitmapLruCache imageCache = new BitmapLruCache();
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
                Spannable messageContent = (Spannable) Html.fromHtml(item.message,
                        new HtmlImageGetter(messageText, imageCache, 512, 512), null);
                messageContent = (Spannable) Helper.setSpanClickListener(messageContent,
                        URLSpan.class,
                        new Helper.OnSpanClickListener() {
                            @Override
                            public boolean onClick(View widget, String data) {
                                // TODO: complete these actions
                                final Uri uri = Uri.parse(data);
                                String mod = uri.getQueryParameter("mod");
                                if ("viewthread".equals(mod)) {
                                    startActivity(new Intent(PostListActivity.this, PostListActivity.class) {{
                                        putExtra("tid", Helper.toSafeInteger(uri.getQueryParameter("tid"), 0));
                                    }});
                                    return true;
                                }
                                else if ("post".equals(mod)) {
                                    if ("reply".equals(uri.getQueryParameter("action"))) {
                                        gotoReply(null);
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });
                messageContent = (Spannable) Helper.setSpanClickListener(messageContent,
                        ImageSpan.class,
                        new Helper.OnSpanClickListener() {
                            @Override
                            public boolean onClick(View widget, String src) {
                                Intent intent = new Intent(ThisApp.context, AttachmentViewer.class);
                                intent.putExtra("tid", getIntent().getIntExtra("tid", 0));
                                intent.putExtra("index", mListFragment.getIndex(item));

                                Attachment attachment = mAttachmentMap.get(src);
                                // attachment image
                                if (attachment != null) {
                                    intent.putExtra("src", attachment.src);
                                    startActivity(intent);
                                }
                                // external images
                                else if (!Discuz.getSafeUrl(src).startsWith(Discuz.DISCUZ_HOST)) {
                                    intent.putExtra("src", src);
                                    startActivity(intent);
                                }
                                return false;
                            }
                        });

                messageText.setText(messageContent);
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
                    for (int i = 0; i < comments.size(); i ++) {
                        Comment comment = comments.get(i);
                        View commentView;
                        if (i < cachedViews.size()) {
                            commentView = cachedViews.get(i);
                        }
                        else {
                            commentView = new TextView(PostListActivity.this);
                            commentView.setPadding(32, 0, 0, 0);
                            cachedViews.add(commentView);
                        }
                        ((TextView) commentView).setText(
                                Html.fromHtml("<b>" + comment.author + "</b>&nbsp;&nbsp;" + comment.comment));
                        if (commentView.getParent() != null)
                            ((ViewGroup) commentView.getParent()).removeView(commentView);
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

                // show votes
                TextView votes = (TextView) viewHolder.getView(R.id.votes);
                Helper.updateVisibility(votes, false);
                if (item.number == 1 && mPollOptions.size() > 0) {
                    Helper.updateVisibility(votes, true);
                    votes.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showPollOptions();
                        }
                    });
                }
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final List listData) {
        final int page = listData.size() / PAGE_SIZE_COUNT;
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
                        Log.e(PostListActivity.class.toString(),
                                "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                else {
                    Helper.setListLength(listData, page * PAGE_SIZE_COUNT);
                    try {
                        JSONObject var = data.getJSONObject("Variables");

                        JSONArray postlist = var.getJSONArray("postlist");
                        for (int i = 0; i < postlist.length(); i ++) {
                            JSONObject postData = postlist.getJSONObject(i);
                            Post post = new Post(postData);
                            for (Attachment attachment : post.attachments)
                                mAttachmentMap.put(attachment.src, attachment);
                            post.message = compileMessage(post.message, mAttachmentMap);
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

                        // forum
                        if (var.has("fid"))
                            mForumId = Helper.toSafeInteger(var.optString("fid"), 0);
                        else if (var.opt("forum") instanceof JSONObject)
                            mForumId = Helper.toSafeInteger(var.optJSONObject("forum").optString("fid"), 0);

                        // votes
                        if (var.has("special_poll")) {
                            mPollOptions.clear();
                            JSONObject polloptions = var.getJSONObject("special_poll").getJSONObject("polloptions");
                            for (Iterator<String> iter = polloptions.keys(); iter.hasNext(); ) {
                                String key = iter.next();
                                mPollOptions.add(new PollOption(polloptions.getJSONObject(key)));
                            }
                        }

                    } catch (JSONException e) {
                        Log.e(PostListActivity.class.toString(),
                                "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                mListFragment.loadMoreDone(total);
            }
        });
    }
}
