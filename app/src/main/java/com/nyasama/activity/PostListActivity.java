package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.Post;

import com.nyasama.util.Helper;
import com.nyasama.util.HtmlImageGetter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PostListActivity extends Activity
    implements AbsListView.OnScrollListener, AbsListView.OnItemClickListener {

    private final String TAG = "PostList";

    private CommonListAdapter<Post> mListAdapter;
    private List<Post> mListData = new ArrayList<Post>();
    private int mPageSize = 10;
    private int mListItemCount = Integer.MAX_VALUE;
    private boolean mIsLoading = false;

    public boolean loadMore() {
        final int loadPage = (int)Math.round(Math.floor(mListData.size() / mPageSize));
        final int loadIndex = loadPage * mPageSize;
        final int currentSize = mListData.size();

        if (currentSize < mListItemCount && !mIsLoading) {
            Discuz.execute("viewthread", new HashMap<String, Object>() {{
                put("tid", getIntent().getStringExtra("tid"));
                put("ppp", mPageSize);
                put("page", loadPage + 1);
            }}, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject data) {
                    if (data.has(Discuz.VOLLEY_ERROR)) {
                        Helper.toast(R.string.network_error_toast);
                    }
                    else if (data.has("Message")) {
                        try {
                            JSONObject message = data.getJSONObject("Message");
                            mListData.clear();
                            mListItemCount = 0;
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
                        if (loadIndex < currentSize)
                            mListData.subList(loadIndex, currentSize).clear();
                        try {
                            JSONObject var = data.getJSONObject("Variables");
                            JSONArray postlist = var.getJSONArray("postlist");
                            for (int i = 0; i < postlist.length(); i ++) {
                                JSONObject postData = postlist.getJSONObject(i);
                                mListData.add(new Post(postData));
                            }
                            JSONObject thread = var.getJSONObject("thread");
                            // Note: in x2 there is only "replies"
                            mListItemCount = Integer.parseInt(thread.has("replies") ?
                                    thread.getString("replies") : thread.getString("allreplies")) + 1;
                            mListAdapter.notifyDataSetChanged();
                            setTitle(thread.getString("subject"));
                        } catch (JSONException e) {
                            Log.e(TAG, "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                            Helper.toast(R.string.load_failed_toast);
                        }
                        // TODO: remove these
                        catch (NullPointerException e) { /**/ }
                    }
                    Helper.updateVisibility(findViewById(R.id.loading), mIsLoading = false);
                }
            });
            Helper.updateVisibility(findViewById(R.id.loading), mIsLoading = true);
        }
        return mIsLoading;
    }

    public boolean reloadLastPage() {
        mListItemCount = Integer.MAX_VALUE;
        return loadMore();
    }

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
                            reloadLastPage();
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

    // this function compiles the message to display in android TextViews
    String compileMessage(String message) {
        return message
                .replaceAll("<a .*?</a>", "")
                .replaceAll("<img([^>]*) src=\"static/image/common/none.gif\"", "<img$1 ")
                .replaceAll("<img([^>]*) file=\"(.*?)\"", "<img$1 src=\"$2\"");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        String title = getIntent().getStringExtra("title");
        if (title != null) setTitle(title);

        ListView listView = (ListView) findViewById(R.id.post_list);
        listView.addFooterView(LayoutInflater.from(this)
                .inflate(R.layout.fragment_list_loading, listView, false), false, false);
        final Map<String, Bitmap> imageCache = new HashMap<String, Bitmap>();
        listView.setAdapter(mListAdapter = new CommonListAdapter<Post>(mListData, R.layout.fragment_post_item) {
            @Override
            public void convert(ViewHolder viewHolder, Post item) {
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
                // TODO: display attachments
            }
        });
        listView.setOnScrollListener(this);
        listView.setOnItemClickListener(this);

        loadMore();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == Discuz.REQUEST_CODE_REPLY) {
            if (resultCode > 0)
                reloadLastPage();
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

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {

    }

    @Override
    public void onScroll(AbsListView absListView,
                         int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem + visibleItemCount >= totalItemCount)
            loadMore();
    }

    private Pattern patt1 = Pattern.compile("<span style=\"display:none\">.*?</span>", Pattern.DOTALL);
    private Pattern patt2 = Pattern.compile("<.+quote>.+div>", Pattern.DOTALL);
    private Pattern patt3 = Pattern.compile("<[^<>]*>", Pattern.DOTALL);
    private int MAX_TRIMSTR_LENGTH = 30;
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
                "[color=#999999]"+post.author+" at "+post.dateline+"[/color] "+
                "[url=forum.php?mod=redirect&goto=findpost&pid="+post.id+"&ptid="+tid+
                    "][img]static/image/common/back.gif[/img][/url]"+
            "[/size]\n"+
            message+
        "[/quote]";
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {
        final Post post = mListData.get(position);
        final String tid = getIntent().getStringExtra("tid");
        startActivityForResult(new Intent(this, NewPostActivity.class) {{
            putExtra("tid", tid);
            putExtra("thread_title", "Re: #" + position + " (" + post.author + ")");
            putExtra("notice_trimstr", getTrimstr(post, tid));
        }}, Discuz.REQUEST_CODE_REPLY);
    }
}
