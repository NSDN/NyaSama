package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
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

public class PostListActivity extends Activity
    implements AbsListView.OnScrollListener {

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
            }}, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    if (jsonObject.has("volleyError")) {
                        Helper.toast(getApplicationContext(), R.string.network_error_toast);
                    }
                    else if (jsonObject.has("Message")) {
                        JSONObject message = jsonObject.optJSONObject("Message");
                        mListData.clear();
                        mListItemCount = 0;
                        new AlertDialog.Builder(PostListActivity.this)
                                .setTitle("There is sth wrong...")
                                .setMessage(message.optString("messagestr"))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        finish();
                                    }
                                })
                                .show();
                    }
                    else {
                        try {
                            JSONObject var = jsonObject.getJSONObject("Variables");
                            JSONArray postlist = var.getJSONArray("postlist");
                            for (int i = 0; i < postlist.length(); i ++) {
                                final JSONObject post = postlist.getJSONObject(i);
                                mListData.add(new Post() {{
                                    this.id = post.optString("pid");
                                    this.author = post.optString("author");
                                    this.message = post.optString("message");
                                }});
                            }
                            mListItemCount = Integer.parseInt(
                                    var.getJSONObject("thread").getString("allreplies"));
                            mListAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            Log.e(TAG, "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                            Helper.toast(getApplicationContext(), R.string.load_failed_toast);
                        }
                    }
                    Helper.updateVisibility(findViewById(R.id.loading), mIsLoading = false);
                }
            });
            Helper.updateVisibility(findViewById(R.id.loading), mIsLoading = true);
        }
        return mIsLoading;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null) {
            ListView listView = (ListView) findViewById(R.id.post_list);

            View footer = LayoutInflater.from(this)
                    .inflate(R.layout.fragment_list_loading, null, false);
            listView.addFooterView(footer);

            listView.setAdapter(mListAdapter = new CommonListAdapter<Post>(mListData, R.layout.fragment_post_item) {
                @Override
                public void convert(ViewHolder viewHolder, Post item) {
                    viewHolder.setText(R.id.author, item.author);
                    viewHolder.setText(R.id.message, Html.fromHtml(item.message));
                }
            });
            listView.setOnScrollListener(this);

            loadMore();
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
}
