package com.nyasama.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.FavItem;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class FavListActivity extends FragmentActivity
    implements CommonListFragment.OnListFragmentInteraction<FavItem> {

    final static int PAGE_SIZE_COUNT = 20;
    public static String TAG = "FavList";

    private CommonListFragment<FavItem> mListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_list);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        mListFragment = CommonListFragment.getNewFragment(
                FavItem.class,
                R.layout.fragment_simple_list,
                R.layout.fragment_fav_item,
                R.id.list);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mListFragment).commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fav_list, menu);
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
    public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
        return new CommonListAdapter<FavItem>() {
            @Override
            public void convertView(ViewHolder viewHolder, FavItem item) {
                viewHolder.setText(R.id.title, item.title);
                viewHolder.setText(R.id.date, item.dateline);
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        FavItem item = mListFragment.getData(position);
        if ("tid".equals(item.type)) {
            Intent intent = new Intent(this, PostListActivity.class);
            intent.putExtra("tid", item.dataId);
            intent.putExtra("title", item.title);
            startActivity(intent);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final List listData) {
        final int page = (int) Math.round(Math.floor(listData.size() / PAGE_SIZE_COUNT));
        final int position = page * PAGE_SIZE_COUNT;
        Discuz.execute("myfavthread", new HashMap<String, Object>() {{
            put("page", page + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else {
                    // remove possible duplicated items
                    if (position < listData.size())
                        listData.subList(position, listData.size()).clear();
                    try {
                        JSONObject var = data.getJSONObject("Variables");

                        JSONArray list = var.getJSONArray("list");
                        for (int i = 0; i < list.length(); i++) {
                            listData.add(new FavItem(list.getJSONObject(i)));
                        }

                        total = Helper.toSafeInteger(var.getString("count"), listData.size());

                    } catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load PM Lists Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                mListFragment.loadMoreDone(total);
            }
        });
    }
}
