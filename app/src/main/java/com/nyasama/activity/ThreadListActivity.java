package com.nyasama.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.R;
import com.nyasama.util.Discuz;

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
    }

    private final String TAG = "ThreadList";

    private CommonListAdapter<Thread> mListAdapter;
    private ListView mListView;

    private List<Thread> mThreads = new ArrayList<Thread>();
    private int mTotalThreadCount = Integer.MAX_VALUE;
    private boolean mIsLoading = false;

    public boolean loadMore() {
        if (mThreads.size() < mTotalThreadCount && !mIsLoading) {
            Discuz.execute("forumdisplay", new HashMap<String, Object>() {{
                put("fid", 10);
                put("tpp", 20);
                put("page", Math.floor(mThreads.size() / 20.0) + 1);
            }}, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    if (jsonObject.optString("volleyError").equals("")) {
                        try {
                            JSONObject var = jsonObject.getJSONObject("Variables");
                            JSONArray threads = var.getJSONArray("forum_threadlist");
                            for (int i = 0; i < threads.length(); i++) {
                                final JSONObject thread = threads.getJSONObject(i);
                                mThreads.add(new Thread() {{
                                    this.id = thread.getString("tid");
                                    this.title = thread.getString("subject");
                                }});
                            }
                            mTotalThreadCount = Integer.parseInt(
                                    var.getJSONObject("forum").getString("threads"));
                        } catch (JSONException e) {
                            Log.e(TAG, "JsonError: Load Thread List Failed ("+e.getMessage()+")");
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.load_failed_toast),
                                    Toast.LENGTH_SHORT).show();
                        }
                        mListAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(TAG, "VolleyError: " + jsonObject.optString("volleyError"));
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.network_error_toast),
                                Toast.LENGTH_SHORT).show();
                    }
                    mIsLoading = false;
                }
            });
            mIsLoading = true;
        }
        return mIsLoading;
    }

    public boolean reload() {
        mThreads.clear();
        return loadMore();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_list);
        if (savedInstanceState == null) {
            mListView = (ListView) findViewById(R.id.thread_list);
            mListView.setAdapter(mListAdapter = new CommonListAdapter<Thread>(mThreads, R.layout.fragment_thread_item) {
                @Override
                public void convert(ViewHolder viewHolder, Thread item) {
                    ((TextView) viewHolder.getView(R.id.thread_title)).setText(item.title);
                    ((TextView) viewHolder.getView(R.id.thread_sub)).setText(item.id);
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
