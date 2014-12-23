package com.nyasama.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.activity.PostListActivity;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;
import com.nyasama.util.Discuz.Thread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Created by oxyflour on 2014/11/18.
 *
 */
public class DiscuzTopListFragment extends DiscuzThreadListFragment {

    public static DiscuzTopListFragment getNewFragment(int fid) {
        DiscuzTopListFragment fragment = new DiscuzTopListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_LIST_LAYOUT, R.layout.fragment_simple_list);
        bundle.putInt(ARG_ITEM_LAYOUT, R.layout.fragment_thread_item);
        bundle.putInt("fid", fid);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        Intent intent = new Intent(ThisApp.context, PostListActivity.class);
        Thread thread = getData(position);
        intent.putExtra("tid", thread.id);
        intent.putExtra("title", thread.title);
        startActivity(intent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final List listData) {
        Discuz.execute("toplist", new HashMap<String, Object>() {{
            put("fid", getArguments().getInt("fid"));
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = -1;
                listData.clear();
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else if (!data.isNull("Variables")) {
                    try {
                        JSONObject var = data.getJSONObject("Variables");
                        JSONArray threads = var.getJSONArray("forum_threadlist");
                        for (int i = 0; i < threads.length(); i++) {
                            listData.add(new Thread(threads.getJSONObject(i)));
                        }
                        total = listData.size();
                    } catch (JSONException e) {
                        Log.d("Discuz", "Load Top List Failed (" + e.getMessage() + ")");
                    }
                }
                loadMoreDone(total);
            }
        });
    }
}
