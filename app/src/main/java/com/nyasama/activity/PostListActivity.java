package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class PostListActivity extends Activity
    implements AbsListView.OnScrollListener, AbsListView.OnItemClickListener {

    private final String TAG = "PostList";

    private class Post {
        public String id;
        public String author;
        public String message;
    }

    private CommonListAdapter<Post> mListAdapter;
    private List<Post> mListData = new ArrayList<Post>();
    private int mListItemCount = Integer.MAX_VALUE;
    private boolean mIsLoading = false;

    public boolean loadMore() {
        if (mListData.size() < mListItemCount && !mIsLoading) {
            Discuz.execute("viewthread", new HashMap<String, Object>() {{
                put("tid", getIntent().getStringExtra("tid"));
                put("ppp", 10);
                put("page", Math.round(Math.floor(mListData.size() / 10 + 1)));
            }}, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject data) {
                    if (data.has(Discuz.VOLLEY_ERROR)) {
                        Helper.toast(getApplicationContext(), R.string.network_error_toast);
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
                        catch (JSONException e) { }
                        catch (NullPointerException e) { }
                    }
                    else {
                        try {
                            JSONObject var = data.getJSONObject("Variables");
                            JSONArray postlist = var.getJSONArray("postlist");
                            for (int i = 0; i < postlist.length(); i ++) {
                                final JSONObject post = postlist.getJSONObject(i);
                                mListData.add(new Post() {{
                                    this.id = post.optString("pid");
                                    this.author = post.optString("author");
                                    this.message = post.optString("message");
                                }});
                            }
                            JSONObject thread = var.getJSONObject("thread");
                            // Note: in x2 there is only "replies"
                            mListItemCount = Integer.parseInt(thread.has("replies") ?
                                    thread.getString("replies") : thread.getString("allreplies")) + 1;
                            mListAdapter.notifyDataSetChanged();
                            setTitle(thread.getString("subject"));
                        } catch (JSONException e) {
                            Log.e(TAG, "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                            Helper.toast(getApplicationContext(), R.string.load_failed_toast);
                        }
                        catch (NullPointerException e) { }
                    }
                    Helper.updateVisibility(findViewById(R.id.loading), mIsLoading = false);
                }
            });
            Helper.updateVisibility(findViewById(R.id.loading), mIsLoading = true);
        }
        return mIsLoading;
    }

    public boolean reload() {
        mListData.clear();
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
                        Helper.toast(PostListActivity.this, R.string.network_error_toast);
                    }
                    else {
                        JSONObject message = data.optJSONObject("Message");
                        String messageval = message.optString("messageval");
                        if ("post_reply_succeed".equals(messageval)) {
                            reload();
                        }
                        else
                            Helper.toast(PostListActivity.this, message.optString("messagestr"));
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null) {
            ListView listView = (ListView) findViewById(R.id.post_list);

            View footer = LayoutInflater.from(this)
                    .inflate(R.layout.fragment_list_loading, listView, false);
            listView.addFooterView(footer, null, false);

            listView.setAdapter(mListAdapter = new CommonListAdapter<Post>(mListData, R.layout.fragment_post_item) {
                @Override
                public void convert(ViewHolder viewHolder, Post item) {
                    viewHolder.setText(R.id.author, item.author);
                    viewHolder.setText(R.id.message, Html.fromHtml(item.message));
                }
            });
            listView.setOnScrollListener(this);
            listView.setOnItemClickListener(this);

            loadMore();
        }
    }

    private final int REQUEST_CODE_REPLY = 1;
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_REPLY) {
            if (resultCode > 0)
                reload();
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
        else if (id == R.id.action_reply) {
            quickReply();
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

    private Pattern patt1 = Pattern.compile("\\<span style=\"display:none\"\\>.*?\\</span\\>", Pattern.DOTALL);
    private Pattern patt2 = Pattern.compile("\\<.+quote\\>.+div\\>", Pattern.DOTALL);
    private Pattern patt3 = Pattern.compile("<[^<>]*>", Pattern.DOTALL);
    private String getTrimstr(Post post, String tid) {
        // Note: see Discuz source net/discuz/source/PostSender.java
        String message = post.message;
        message = patt1.matcher(message).replaceAll("");
        message = Html.fromHtml(message).toString();
        message = patt2.matcher(message).replaceAll("");
        message = patt3.matcher(message).replaceAll("");
        return "[quote]"+
            "[size=2]"+
                "[color=#999999]"+post.author+" at "+" <some time> "+"[/color] "+
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
        }}, REQUEST_CODE_REPLY);
    }
}
