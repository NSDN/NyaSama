package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.ThisApp;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.R;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;
import com.nyasama.util.Discuz.Thread;
import com.nyasama.util.Discuz.Forum;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ThreadListActivity extends FragmentActivity
    implements CommonListFragment.OnListFragmentInteraction<Object> {

    private final String TAG = "ThreadList";
    private final int PAGE_SIZE_COUNT = 20;

    private CommonListFragment<Thread> mThreadListFragment;
    private CommonListFragment<Forum> mSubListFragment;
    private List<Forum> mSubList = new ArrayList<Forum>();
    private FragmentPagerAdapter mPageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_list);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        String title = getIntent().getStringExtra("title");
        if (title != null) setTitle(title);

        ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
        pager.setAdapter(mPageAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int i) {
                if (i == 0) {
                    return mThreadListFragment = CommonListFragment.getNewFragment(
                            Thread.class,
                            R.layout.fragment_thread_list,
                            R.layout.fragment_thread_item,
                            R.id.list, PAGE_SIZE_COUNT);
                } else {
                    return mSubListFragment = CommonListFragment.getNewFragment(
                            Forum.class,
                            R.layout.fragment_forum_cat_item,
                            R.layout.fragment_forum_item,
                            R.id.forum_list, PAGE_SIZE_COUNT);
                }
            }

            @Override
            public int getCount() {
                return mSubList.size() > 0 ? 2 : 1;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return position == 0 ?
                        // TODO: rename this
                        getString(R.string.thread_list_threads_title) :
                        getString(R.string.thread_list_subforum_title);
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == Discuz.REQUEST_CODE_NEW_THREAD) {
            if (resultCode > 0)
                mThreadListFragment.reloadAll();
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
                putExtra("fid", ThreadListActivity.this.getIntent().getIntExtra("fid", 0));
            }}, Discuz.REQUEST_CODE_NEW_THREAD);
            return true;
        }
        else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(CommonListFragment fragment,
                            View view, int position, long id) {
        if (fragment == mThreadListFragment) {
            Intent intent = new Intent(view.getContext(), PostListActivity.class);
            Thread thread = mThreadListFragment.getData(position);
            intent.putExtra("tid", thread.id);
            intent.putExtra("title", thread.title);
            startActivity(intent);
        }
        else if (fragment == mSubListFragment) {
            Forum item = mSubList.get(position);
            Intent intent = new Intent(view.getContext(), ThreadListActivity.class);
            intent.putExtra("fid", item.id);
            intent.putExtra("title", item.name);
            startActivity(intent);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment,
                              final int position, final int page, final List listData) {
        if (fragment == mThreadListFragment) Discuz.execute("forumdisplay", new HashMap<String, Object>() {{
            put("fid", getIntent().getIntExtra("fid", 0));
            put("tpp", PAGE_SIZE_COUNT);
            put("page", page + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            @SuppressWarnings("unchecked")
            public void onResponse(JSONObject data) {
                int total = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else if (data.has("Message")) {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        listData.clear();
                        new AlertDialog.Builder(ThreadListActivity.this)
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
                        Log.e(TAG, "JsonError: Load Thread List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                } else {
                    // remove possible duplicated items
                    if (position < listData.size())
                        listData.subList(position, listData.size()).clear();
                    try {
                        JSONObject var = data.getJSONObject("Variables");
                        JSONArray threads = var.getJSONArray("forum_threadlist");
                        for (int i = 0; i < threads.length(); i++) {
                            final JSONObject thread = threads.getJSONObject(i);
                            listData.add(new Thread(thread));
                        }
                        JSONObject forum = var.getJSONObject("forum");
                        total = Integer.parseInt(
                                forum.getString("threads"));
                        setTitle(forum.getString("name"));
                        // save data to sublist
                        if (var.has("sublist")) {
                            JSONArray sublist = var.getJSONArray("sublist");
                            if (sublist.length() > 0 && mSubList.size() == 0) {
                                for (int i = 0; i < sublist.length(); i ++) {
                                    final JSONObject subforum = sublist.getJSONObject(i);
                                    mSubList.add(new Forum(subforum));
                                }
                                // notify view pager
                                Helper.updateVisibility(findViewById(R.id.view_strip), true);
                                mPageAdapter.notifyDataSetChanged();
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load Thread List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                mThreadListFragment.loadMoreDone(total);
            }
        });
        else if (fragment == mSubListFragment) {
            listData.clear();
            for (Forum forum : mSubList)
                listData.add(forum);
            mSubListFragment.loadMoreDone(mSubList.size());
        }
    }

    @Override
    public void onConvertView(CommonListFragment fragment, CommonListAdapter.ViewHolder viewHolder, Object obj) {
        if (fragment == mThreadListFragment) {
            Thread item = (Thread) obj;
            viewHolder.setText(R.id.title, Html.fromHtml(item.title));
            viewHolder.setText(R.id.sub,
                    Html.fromHtml(item.author + " " + item.lastpost));
            viewHolder.setText(R.id.inf, item.replies + "/" + item.views);
        }
        else if (fragment == mSubListFragment) {
            final Forum item = (Forum) obj;
            viewHolder.setText(R.id.title, item.name);
            viewHolder.setText(R.id.sub,
                    "threads:"+item.threads+"  posts:"+item.todayPosts+"/"+item.posts);
            ((NetworkImageView) viewHolder.getView(R.id.icon))
                    .setImageUrl(item.icon, ThisApp.imageLoader);
        }
    }

}
