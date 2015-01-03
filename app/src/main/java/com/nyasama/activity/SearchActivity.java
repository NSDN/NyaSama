package com.nyasama.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.View;
import android.widget.SearchView;

import com.android.volley.Response;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.nyasama.R;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;
import com.nyasama.util.Discuz.Thread;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SearchActivity extends BaseThemedActivity
    implements CommonListFragment.OnListFragmentInteraction<Object> {

    private static final int PAGE_SIZE_COUNT = 25;
    private CommonListFragment<Object> mListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_framelayout);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

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

        String query = getIntent().getStringExtra(SearchManager.QUERY);
        if (query != null) searchView.setQuery(query, false);

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
            intent.putExtra("title", Html.fromHtml(thread.title).toString());
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
            put("tpp", PAGE_SIZE_COUNT);
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.there_is_something_wrong);
                } else if (data.has("Message")) {
                    new AccentAlertDialog.Builder(SearchActivity.this)
                            .setTitle(R.string.there_is_something_wrong)
                            .setMessage(data.optJSONObject("Message").optString("messagestr"))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                } else if (data.has("Variables")) {
                    JSONObject var = data.optJSONObject("Variables");
                    Helper.setListLength(listData, page * PAGE_SIZE_COUNT);
                    // TODO: update search.php and parse more results here
                    JSONObject list = var.optJSONObject("threadlist");
                    if (list != null) for (Iterator<String> iter = list.keys(); iter.hasNext(); ) {
                        String key = iter.next();
                        listData.add(new Thread(list.optJSONObject(key)));
                    }
                    // update total
                    total = Helper.toSafeInteger(var.optString("count"), listData.size());
                }
                fragment.loadMoreDone(total);
            }
        });
    }
}
