package com.nyasama.activity;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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

import org.json.JSONObject;

import java.util.List;

public class SearchActivity extends FragmentActivity
    implements CommonListFragment.OnListFragmentInteraction<Object> {

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
                android.R.layout.simple_list_item_1,
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
            public void convertView(ViewHolder viewHolder, Object item) {
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
    }

    @Override
    public void onLoadingMore(final CommonListFragment fragment, List listData) {
        Discuz.search(getIntent().getStringExtra(SearchManager.QUERY), "", new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.there_is_something_wrong);
                } else if (data.has("Message")) {
                    Helper.toast(data.optJSONObject("Message").optString("messagestr"));
                    total = 0;
                } else if (data.has("Variables")) {
                    // TODO: implement this
                    ;
                }
                fragment.loadMoreDone(total);
            }
        });
    }
}
