package com.nyasama.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.ThisApp;
import com.nyasama.fragment.DiscuzThreadListFragment;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.R;
import com.nyasama.fragment.DiscuzTopListFragment;
import com.nyasama.util.Helper;
import com.nyasama.util.Discuz.Forum;

import java.util.ArrayList;
import java.util.List;

/*
 * Note: this activity handles both forum threads and user threads
 */

public class ThreadListActivity extends BaseThemedActivity implements
        DiscuzThreadListFragment.OnThreadListInteraction {

    public final int REQUEST_CODE_NEW_THREAD = 1;

    // These forums has no top list
    public static List<Integer> FORUMS_NO_TOPLIST = new ArrayList<Integer>() {{
        add(92);
        add(113);
    }};

    private DiscuzThreadListFragment mThreadListFragment;
    private SubForumFragment mSubListFragment;
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
        boolean noTopList = FORUMS_NO_TOPLIST.indexOf(getIntent().getIntExtra("fid", 0)) >= 0;
        // only return two pages
        // SubForum & ThreadList
        if (noTopList) pager.setAdapter(mPageAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            // REF: http://stackoverflow.com/questions/8785221/retrieve-a-fragment-from-a-viewpager
            @Override
            @SuppressWarnings("unchecked")
            public Object instantiateItem(ViewGroup container, int position) {
                Object item = super.instantiateItem(container, position);
                if (item instanceof DiscuzThreadListFragment)
                    mThreadListFragment = (DiscuzThreadListFragment) item;
                else if (item instanceof SubForumFragment)
                    mSubListFragment = (SubForumFragment) item;
                return item;
            }

            @Override
            public Fragment getItem(int position) {
                if (position == 0)
                    return new SubForumFragment();
                else
                    return DiscuzThreadListFragment.getNewFragment(
                        getIntent().getIntExtra("fid", 0),
                        getIntent().getIntExtra("uid", 0),
                        20);
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                String[] titles = {
                        getString(R.string.thread_list_subforum_title),
                        getString(R.string.thread_list_threads_title)
                };
                return titles[position];
            }
        });
        // return two or three pages
        // ThreadList & TopList [& SubList]
        else pager.setAdapter(mPageAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            // REF: http://stackoverflow.com/questions/8785221/retrieve-a-fragment-from-a-viewpager
            @Override
            @SuppressWarnings("unchecked")
            public Object instantiateItem(ViewGroup container, int position) {
                Object item = super.instantiateItem(container, position);
                if (item instanceof DiscuzThreadListFragment)
                    mThreadListFragment = (DiscuzThreadListFragment) item;
                else if (item instanceof SubForumFragment)
                    mSubListFragment = (SubForumFragment) item;
                return item;
            }

            @Override
            public Fragment getItem(int i) {
                if (i == 0)
                    return DiscuzThreadListFragment.getNewFragment(
                            getIntent().getIntExtra("fid", 0),
                            getIntent().getIntExtra("uid", 0),
                            20);
                else if (i == 1)
                    return DiscuzTopListFragment.getNewFragment(
                            getIntent().getIntExtra("fid", 0));
                else
                    return new SubForumFragment();
            }

            @Override
            public int getCount() {
                if (getIntent().getIntExtra("fid", 0) > 0)
                    return mSubList != null && mSubList.size() > 0 ? 3 : 2;
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
        if (requestCode == REQUEST_CODE_NEW_THREAD) {
            if (resultCode > 0 && mThreadListFragment != null)
                mThreadListFragment.reloadAll();
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
        int id = item.getItemId();
        if (id == R.id.action_new_post) {
            startActivityForResult(new Intent(this, NewPostActivity.class) {{
                putExtra("fid", ThreadListActivity.this.getIntent().getIntExtra("fid", 0));
            }}, REQUEST_CODE_NEW_THREAD);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onGetThreadData(DiscuzThreadListFragment fragment) {
        if (!(fragment instanceof DiscuzTopListFragment)) {
            mSubList = fragment.getSubList();
            mPageAdapter.notifyDataSetChanged();
            if (mSubListFragment != null)
                mSubListFragment.updateSubList(mSubList);
        }
    }

    public static class SubForumFragment extends Fragment {
        private List<Forum> mSubList = new ArrayList<Forum>();
        private CommonListAdapter mListAdaptor;

        public void updateSubList(List<Forum> subList) {
            mSubList.clear();
            for (Forum forum : subList)
                mSubList.add(forum);
            if (mListAdaptor != null)
                mListAdaptor.notifyDataSetChanged();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_simple_list, container, false);
            ListView listView = (ListView) rootView.findViewById(R.id.list);
            listView.setDividerHeight(0);
            listView.setAdapter(mListAdaptor = new CommonListAdapter<Forum>(mSubList, R.layout.fragment_forum_item) {
                @Override
                public void convertView(ViewHolder viewHolder, Forum item) {
                    viewHolder.setText(R.id.title, item.name);
                    viewHolder.setText(R.id.sub,
                            getString(R.string.forum_index_threads)+":"+item.threads+"  "+
                                    getString(R.string.forum_index_posts)+":"+item.todayPosts);
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

            SwipeRefreshLayout refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh);
            refreshLayout.setEnabled(false);

            if (getActivity() != null)
                updateSubList(((ThreadListActivity) getActivity()).mSubList);

            rootView.setBackgroundColor(getResources().getColor(R.color.background_light_gray));
            return rootView;
        }
    }

}
