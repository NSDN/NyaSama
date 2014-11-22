package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
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
    private final int MAX_TRIMSTR_LENGTH = 30;
    private final String TAG = "PostList";

    private CommonListFragment<Post> mListFragment;
    private SparseArray<List<Comment>> mComments = new SparseArray<List<Comment>>();

    public void replyThread(final String text) {
        if (!text.isEmpty()) {
            Discuz.execute("sendreply", new HashMap<String, Object>() {{
                put("tid", getIntent().getStringExtra("tid"));
                put("replysubmit", "yes");
            }}, new HashMap<String, Object>() {{
                put("message", text);
            }}, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject data) {
                    if (data.has(Discuz.VOLLEY_ERROR)) {
                        Helper.toast(R.string.network_error_toast);
                    }
                    else {
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
    }

    public void quickReply() {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("QuickReply")
                .setMessage("Type Something")
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        replyThread(input.getText().toString());
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //
            }
        }).show();
    }

    static Pattern msgPathPattern = Pattern.compile("<img[^>]* file=\"(.*?)\"");
    static CallbackMatcher msgMatcher = new CallbackMatcher("<ignore_js_op>(.*?)</ignore_js_op>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    // this function compiles the message to display in android TextViews
    String compileMessage(String message) {
        return msgMatcher.replaceMatches(message, new CallbackMatcher.Callback() {
                    @Override
                    public String foundMatch(MatchResult matchResult) {
                        Matcher pathMatcher = msgPathPattern.matcher(matchResult.group(1));
                        return !pathMatcher.find() ? "" :
                                "<img src=\"" + pathMatcher.group(1) + "\" />";
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        String title = getIntent().getStringExtra("title");
        if (title != null) setTitle(title);

        mListFragment = new CommonListFragment<Post>();
        Bundle bundle = new Bundle();
        bundle.putInt(CommonListFragment.ARG_LIST_LAYOUT, R.layout.fragment_post_list);
        bundle.putInt(CommonListFragment.ARG_ITEM_LAYOUT, R.layout.fragment_post_item);
        bundle.putInt(CommonListFragment.ARG_LIST_VIEW_ID, R.id.list);
        bundle.putInt(CommonListFragment.ARG_PAGE_SIZE, PAGE_SIZE_COUNT);
        mListFragment.setArguments(bundle);
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
            quickReply();
        }
        else if (id == R.id.action_reply) {
            final String tid = getIntent().getStringExtra("tid");
            startActivityForResult(new Intent(this, NewPostActivity.class) {{
                putExtra("tid", tid);
                putExtra("thread_title", "Re: "+getTitle());
            }}, Discuz.REQUEST_CODE_REPLY);
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
    private String getTrimstr(Post post, String tid) {
        // Note: see Discuz source net/discuz/source/PostSender.java
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

    @Override
    public void onItemClick(CommonListFragment fragment, View view, final int position, long id) {
        final Post post = mListFragment.getData(position);
        final String tid = getIntent().getStringExtra("tid");
        startActivityForResult(new Intent(this, NewPostActivity.class) {{
            putExtra("tid", tid);
            putExtra("thread_title", "Re: #" + position + " (" + post.author + ")");
            putExtra("notice_trimstr", getTrimstr(post, tid));
        }}, Discuz.REQUEST_CODE_REPLY);
    }

    Map<String, Bitmap> imageCache = new HashMap<String, Bitmap>();
    @Override
    public void onConvertView(CommonListFragment fragment, CommonListAdapter.ViewHolder viewHolder, Post item) {
        String avatar_url = Discuz.DISCUZ_URL +
                "uc_server/avatar.php?uid="+item.authorId+"&size=small";
        ((NetworkImageView) viewHolder.getView(R.id.avatar))
                .setImageUrl(avatar_url, ThisApp.imageLoader);
        viewHolder.setText(R.id.author, item.author);
        viewHolder.setText(R.id.date, Html.fromHtml(item.dateline));
        viewHolder.setText(R.id.index, "#"+item.number);
        TextView textView = (TextView) viewHolder.getView(R.id.message);
        textView.setText(Html.fromHtml(compileMessage(item.message),
                new HtmlImageGetter(textView, Discuz.DISCUZ_URL, imageCache), null));
        // load comments
        List<Comment> comments = mComments.get(Integer.parseInt(item.id));
        if (comments != null) {
            AbsListView commentList = (AbsListView) viewHolder.getView(R.id.comment_list);
            commentList.setAdapter(new CommonListAdapter<Comment>(comments, R.layout.fragment_comment_item) {
                @Override
                public void convert(ViewHolder viewHolder, Comment item) {
                    viewHolder.setText(R.id.author, item.author);
                    viewHolder.setText(R.id.comment, item.comment);
                }
            });
        }
        // TODO: display attachments
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment,
                              final int position, final int page, final List listData) {
        Discuz.execute("viewthread", new HashMap<String, Object>() {{
            put("tid", getIntent().getStringExtra("tid"));
            put("ppp", PAGE_SIZE_COUNT);
            put("page", page + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = 0;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else if (data.has("Message")) {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        listData.clear();
                        new AlertDialog.Builder(PostListActivity.this)
                                .setTitle("There is sth wrong...")
                                .setMessage(message.getString("messagestr"))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        finish();
                                    }
                                })
                                .show();
                    }
                    // TODO: remove these
                    catch (JSONException e) { /**/ }
                    catch (NullPointerException e) { /**/ }
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
                            listData.add(new Post(postData));
                        }
                        JSONObject thread = var.getJSONObject("thread");
                        // comments
                        if (var.has("comments")) {
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
                            }
                        }
                        // Note: in x2 there is only "replies"
                        total = Integer.parseInt(thread.has("replies") ?
                                thread.getString("replies") : thread.getString("allreplies")) + 1;
                        setTitle(thread.getString("subject"));
                    } catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                    // TODO: remove these
                    catch (NullPointerException e) { /**/ }
                }
                mListFragment.loadMoreDone(total);
            }
        });
    }
}
