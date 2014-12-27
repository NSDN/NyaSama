package com.nyasama.activity;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;
import com.nyasama.util.Discuz.Thread;

import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SearchActivity extends FragmentActivity
    implements CommonListFragment.OnListFragmentInteraction<Object> {

    private static final int PAGE_SIZE_COUNT = 20;
    private CommonListFragment<Object> mListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mListFragment = CommonListFragment.getNewFragment(
                Object.class,
                R.layout.fragment_simple_list,
                R.layout.fragment_thread_item,
                R.id.list);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mListFragment)
                .commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setIconified(false);
        searchView.setIconifiedByDefault(false);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                getIntent().putExtra(SearchManager.QUERY, s);
                if (mListFragment != null)
                    mListFragment.reloadAll();
                return false;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return Helper.handleOption(this, item.getItemId()) ||
                super.onOptionsItemSelected(item);
    }

    @Override
    public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
        return new CommonListAdapter() {
            @Override
            public void convertView(ViewHolder viewHolder, Object obj) {
                Thread item = (Thread) obj;
                viewHolder.setText(R.id.title, Html.fromHtml(item.title));
                viewHolder.setText(R.id.sub,
                        Html.fromHtml(item.author + " " + item.lastpost));
                viewHolder.setText(R.id.inf, item.replies + "/" + item.views);
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        Object obj = fragment.getData(position);
        if (obj instanceof Thread) {
            Thread thread = (Thread) obj;
            Intent intent = new Intent(this, PostListActivity.class);
            intent.putExtra("tid", thread.id);
            intent.putExtra("title", thread.title);
            startActivity(intent);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(final CommonListFragment fragment, final List listData) {
        String query = getIntent().getStringExtra(SearchManager.QUERY);
        if (query == null || query.isEmpty()) {
            fragment.loadMoreDone(0);
            return;
        }
        final int page = listData.size() / PAGE_SIZE_COUNT;
        Discuz.search(query, new HashMap<String, Object>() {{
            put("page", page + 1);
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.there_is_something_wrong);
                } else if (data.has("Message")) {
                    Helper.toast(data.optJSONObject("Message").optString("messagestr"));
                    total = 0;
                } else if (data.has("Variables")) {
                    JSONObject var = data.optJSONObject("Variables");
                    // remove possible duplicated items
                    if (page * PAGE_SIZE_COUNT < listData.size())
                        listData.subList(page * PAGE_SIZE_COUNT, listData.size()).clear();
                    // TODO: we only parse threads here
                    JSONObject list = var.optJSONObject("threadlist");
                    if (list != null) for (Iterator<String> iter = list.keys(); iter.hasNext(); ) {
                        String key = iter.next();
                        listData.add(new Thread(list.optJSONObject(key)));
                    }
                    // sort by id
                    Collections.sort(listData, new Comparator() {
                        @Override
                        public int compare(Object o, Object o2) {
                            return ((Thread) o).id - ((Thread) o2).id;
                        }
                    });
                    // total
                    total = Helper.toSafeInteger(var.optString("count"), listData.size());
                }
                fragment.loadMoreDone(total);
            }
        });
    }
}
