package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Response;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.R;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;


public class ThreadListActivity extends FragmentActivity
    implements CommonListFragment.OnListFragmentInteraction<ThreadListActivity.Thread> {

    private final String TAG = "ThreadList";

    public class Thread {
        public String id;
        public String title;
        public String sub;
    }

    private CommonListFragment<Thread> mFragment;
    private int mPageSize = 20;

    @Override
    public void onItemClick(View view, int position, long id) {
        Intent intent = new Intent(view.getContext(), PostListActivity.class);
        intent.putExtra("tid", mFragment.getData(position).id);
        startActivity(intent);
    }

    @Override
    public void onLoadingMore(final int position, final int page, final List listData) {
        Discuz.execute("forumdisplay", new HashMap<String, Object>() {{
            put("fid", getIntent().getStringExtra("fid"));
            put("tpp", mPageSize);
            put("page", page + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            @SuppressWarnings("unchecked")
            public void onResponse(JSONObject data) {
                int total = 0;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else if (data.has("Message")) {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        listData.clear();
                        new AlertDialog.Builder(ThreadListActivity.this)
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
                } else {
                    // remove possible duplicated items
                    if (position < listData.size())
                        listData.subList(position, listData.size()).clear();
                    try {
                        JSONObject var = data.getJSONObject("Variables");
                        JSONArray threads = var.getJSONArray("forum_threadlist");
                        for (int i = 0; i < threads.length(); i++) {
                            final JSONObject thread = threads.getJSONObject(i);
                            listData.add(new Thread() {{
                                this.id = thread.getString("tid");
                                this.title = thread.optString("subject");
                                this.sub = thread.optString("author") + " " +
                                        thread.optString("lastpost");
                            }});
                        }
                        JSONObject forum = var.getJSONObject("forum");
                        total = Integer.parseInt(
                                forum.getString("threads"));
                        setTitle(forum.getString("name"));
                        Helper.updateVisibility(findViewById(R.id.empty), total <= 0);
                    } catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load Thread List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                    // TODO: reomve these
                    catch (NullPointerException e) { /**/ }
                }
                mFragment.loadMoreDone(total);
            }
        });
    }

    @Override
    public void onConvertView(CommonListAdapter.ViewHolder viewHolder, Thread item) {
        viewHolder.setText(R.id.title, Html.fromHtml(item.title));
        viewHolder.setText(R.id.sub, Html.fromHtml(item.sub));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_list);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        mFragment = new CommonListFragment<Thread>();
        Bundle bundle = new Bundle();
        bundle.putInt("list_layout", R.layout.fragment_thread_list);
        bundle.putInt("item_layout", R.layout.fragment_thread_item);
        bundle.putInt("page_size", mPageSize);
        mFragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                .replace(R.id.container, mFragment)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == Discuz.REQUEST_CODE_NEW_THREAD) {
            if (resultCode > 0)
                mFragment.reloadAll();
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
        else if (id == R.id.action_new_post) {
            startActivityForResult(new Intent(this, NewPostActivity.class) {{
                putExtra("fid", ThreadListActivity.this.getIntent().getStringExtra("fid"));
            }}, Discuz.REQUEST_CODE_NEW_THREAD);
            return true;
        }
        else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
