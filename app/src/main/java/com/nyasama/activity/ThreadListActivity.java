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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.ThisApp;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.R;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.fragment.TopListFragment;
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

/*
 * Note: this activity handles both forum threads and user threads
 */

public class ThreadListActivity extends FragmentActivity
    implements CommonListFragment.OnListFragmentInteraction<Object> {

    private final String TAG = "ThreadList";
    private final int PAGE_SIZE_COUNT = 20;

    private CommonListFragment<Thread> mListFragment;
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
            // REF: http://stackoverflow.com/questions/8785221/retrieve-a-fragment-from-a-viewpager
            @Override
            @SuppressWarnings("unchecked")
            public Object instantiateItem(ViewGroup container, int position) {
                Object item = super.instantiateItem(container, position);
                if (position == 0)
                    mListFragment = (CommonListFragment) item;
                return item;
            }

            @Override
            public Fragment getItem(int i) {
                if (i == 0) {
                    return CommonListFragment.getNewFragment(
                            Thread.class,
                            R.layout.fragment_simple_list,
                            R.layout.fragment_thread_item,
                            R.id.list);
                }
                else if (i == 1) {
                    return TopListFragment.getNewFragment(getIntent().getIntExtra("fid", 0));
                }
                else {
                    return new Fragment() {
                        @Override
                        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                                 Bundle savedInstanceState) {
                            // use forum index again
                            View rootView = inflater.inflate(R.layout.fragment_forum_index, container, false);
                            AbsListView listView = (AbsListView) rootView.findViewById(R.id.list);
                            listView.setAdapter(new CommonListAdapter<Forum>(mSubList, R.layout.fragment_forum_item) {
                                @Override
                                public void convertView(ViewHolder viewHolder, Forum item) {
                                    viewHolder.setText(R.id.title, item.name);
                                    viewHolder.setText(R.id.sub,
                                            "threads:" + item.threads + "  posts:" + item.todayPosts + "/" + item.posts);
                                    ((NetworkImageView) viewHolder.getView(R.id.icon))
                                            .setImageUrl(item.icon, ThisApp.imageLoader);
                                }
                            });
                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    Forum item = mSubList.get(i);
                                    Intent intent = new Intent(view.getContext(), ThreadListActivity.class);
                                    intent.putExtra("fid", item.id);
                                    intent.putExtra("title", item.name);
                                    startActivity(intent);
                                }
                            });
                            return rootView;
                        }
                    };
                }
            }

            @Override
            public int getCount() {
                if (getIntent().getIntExtra("fid", 0) > 0)
                    return mSubList.size() > 0 ? 3 : 2;
                else
                    return 1;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                String[] titles = {
                        getString(R.string.thread_list_threads_title),
                        getString(R.string.thread_list_toplist_title),
                        getString(R.string.thread_list_subforum_title)
                };
                return titles[position];
            }
        });

        if (mPageAdapter.getCount() == 1)
            Helper.updateVisibility(findViewById(R.id.view_strip), false);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == Discuz.REQUEST_CODE_NEW_THREAD) {
            if (resultCode > 0 && mListFragment != null)
                mListFragment.reloadAll();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_thread_list, menu);
        menu.findItem(R.id.action_new_post).setVisible(getIntent().getIntExtra("fid", 0) > 0);
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
    public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
        return new CommonListAdapter<Thread>() {
            @Override
            public void convertView(ViewHolder viewHolder, Thread item) {
                viewHolder.setText(R.id.title, Html.fromHtml(item.title));
                viewHolder.setText(R.id.sub,
                        Html.fromHtml(item.author + " " + item.lastpost));
                viewHolder.setText(R.id.inf, item.replies + "/" + item.views);
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        Intent intent = new Intent(this, PostListActivity.class);
        Thread thread = mListFragment.getData(position);
        intent.putExtra("tid", thread.id);
        intent.putExtra("title", thread.title);
        startActivity(intent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final List listData) {
        final int page = (int) Math.round(Math.floor(listData.size() / PAGE_SIZE_COUNT));
        final int position = page * PAGE_SIZE_COUNT;

        Intent intent = getIntent();
        final int fid = intent.getIntExtra("fid", 0);
        final int uid = intent.getIntExtra("uid", Discuz.sUid);
        if (fid == 0 && uid == 0)
            throw new RuntimeException("fid or uid is required!");

        String module = fid > 0 ? "forumdisplay" : "mythread";
        Discuz.execute(module, new HashMap<String, Object>() {{
            if (fid > 0)
                put("fid", fid);
            else
                put("uid", uid);
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

                        JSONArray threads;
                        if (var.opt("forum_threadlist") instanceof JSONArray)
                            threads = var.getJSONArray("forum_threadlist");
                        else if (var.opt("data") instanceof JSONArray)
                            threads = var.getJSONArray("data");
                        else
                            threads = new JSONArray();
                        for (int i = 0; i < threads.length(); i++) {
                            final JSONObject thread = threads.getJSONObject(i);
                            listData.add(new Thread(thread));
                        }

                        if (var.opt("forum") instanceof JSONObject) {
                            JSONObject forum = var.getJSONObject("forum");
                            setTitle(forum.getString("name"));
                        }
                        // if we don't know the number of items
                        // just keep loading until there is no more
                        if (threads.length() < PAGE_SIZE_COUNT)
                            total = listData.size();
                        else
                            total = Integer.MAX_VALUE;

                        // save subforums to sublist
                        if (var.opt("sublist") instanceof JSONArray) {
                            JSONArray sublist = var.getJSONArray("sublist");
                            if (sublist.length() > 0 && mSubList.size() == 0) {
                                for (int i = 0; i < sublist.length(); i ++) {
                                    final JSONObject subforum = sublist.getJSONObject(i);
                                    mSubList.add(new Forum(subforum));
                                }
                                mPageAdapter.notifyDataSetChanged();
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load Thread List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                mListFragment.loadMoreDone(total);
            }
        });
    }

}
