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
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.R;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ThreadListActivity extends Activity
    implements AbsListView.OnScrollListener {

    private class Thread {
        public String id;
        public String title;
        public String sub;
    }

    private final String TAG = "ThreadList";

    private CommonListAdapter<Thread> mListAdapter;
    private ListView mListView;
    private View mListFooter;

    private List<Thread> mThreads = new ArrayList<Thread>();
    private int mTotalThreadCount = Integer.MAX_VALUE;
    private boolean mIsLoading = false;

    public boolean loadMore() {
        if (mThreads.size() < mTotalThreadCount && !mIsLoading) {
            Discuz.execute("forumdisplay", new HashMap<String, Object>() {{
                put("fid", getIntent().getStringExtra("fid"));
                put("tpp", 20);
                put("page", Math.round(Math.floor(mThreads.size() / 20.0) + 1));
            }}, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    if (jsonObject.has("volleyError")) {
                        Helper.toast(getApplicationContext(), R.string.network_error_toast);
                    }
                    else if (jsonObject.has("Message")) {
                        JSONObject message = jsonObject.optJSONObject("Message");
                        mThreads.clear();
                        mTotalThreadCount = 0;
                        new AlertDialog.Builder(ThreadListActivity.this)
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
                            JSONArray threads = var.getJSONArray("forum_threadlist");
                            for (int i = 0; i < threads.length(); i++) {
                                final JSONObject thread = threads.getJSONObject(i);
                                mThreads.add(new Thread() {{
                                    this.id = thread.getString("tid");
                                    this.title = thread.getString("subject");
                                    this.sub = thread.optString("author") + " " +
                                            thread.optString("lastpost");
                                }});
                            }
                            mTotalThreadCount = Integer.parseInt(
                                    var.getJSONObject("forum").getString("threads"));
                            // NOTE: only update when there is data
                            mListAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            Log.e(TAG, "JsonError: Load Thread List Failed ("+e.getMessage()+")");
                            Helper.toast(getApplicationContext(), R.string.load_failed_toast);
                        }
                    }
                    mIsLoading = false;
                    if (mListFooter != null)
                        mListFooter.setVisibility(View.GONE);
                }
            });
            mIsLoading = true;
            if (mListFooter != null)
                mListFooter.setVisibility(View.VISIBLE);
        }
        return mIsLoading;
    }

    /*
    public boolean reload() {
        mThreads.clear();
        return loadMore();
    }
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_list);
        if (savedInstanceState == null) {
            mListView = (ListView) findViewById(R.id.thread_list);

            mListFooter = LayoutInflater.from(this)
                    .inflate(R.layout.fragment_list_loading, null, false);
            mListView.addFooterView(mListFooter);

            mListView.setAdapter(mListAdapter = new CommonListAdapter<Thread>(mThreads, R.layout.fragment_thread_item) {
                @Override
                public void convert(ViewHolder viewHolder, Thread item) {
                    viewHolder.setText(R.id.thread_title, item.title);
                    viewHolder.setText(R.id.thread_sub, Html.fromHtml(item.sub));
                }
            });
            mListView.setOnScrollListener(this);

            loadMore();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_thread_list, menu);
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
