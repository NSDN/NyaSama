package com.nyasama.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.activity.PostListActivity;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;
import com.nyasama.util.Discuz.Thread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by oxyflour on 2014/11/18.
 *
 */
public class TopListFragment extends android.support.v4.app.Fragment {

    private List<Thread> mListData = new ArrayList<Thread>();

    public static TopListFragment getNewFragment(int fid) {
        TopListFragment fragment = new TopListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("fid", fid);
        fragment.setArguments(bundle);
        return fragment;
    }

    public void displayError(boolean show) {
        View rootView = getView();
        if (rootView != null)
            Helper.updateVisibility(rootView.findViewById(R.id.error), show);
    }
    public void loadList() {
        displayError(false);
        Discuz.execute("toplist", new HashMap<String, Object>() {{
            put("fid", getArguments().getInt("fid"));
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                mListData.clear();
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                    displayError(true);
                } else if (!data.isNull("Variables")) {
                    try {
                        JSONObject var = data.getJSONObject("Variables");
                        JSONArray threads = var.getJSONArray("forum_threadlist");
                        for (int i = 0; i < threads.length(); i++) {
                            mListData.add(new Thread(threads.getJSONObject(i)));
                        }
                    } catch (JSONException e) {
                        Log.d("Discuz", "Load Top List Failed (" + e.getMessage() + ")");
                    }
                }
                displayList();
            }
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_simple_list, container, false);
        mListView = (ListView) rootView.findViewById(R.id.list);

        Button button = (Button) rootView.findViewById(R.id.reload);
        if (button != null) button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadList();
            }
        });

        loadList();
        return rootView;
    }

    private ListView mListView;
    public void displayList() {
        mListView.setAdapter(new CommonListAdapter<Thread>(mListData,
                R.layout.fragment_thread_item) {

            @Override
            public void convertView(ViewHolder viewHolder, Thread item) {
                viewHolder.setText(R.id.title, Html.fromHtml(item.title));
                viewHolder.setText(R.id.sub,
                        Html.fromHtml(item.author + " " + item.lastpost));
                viewHolder.setText(R.id.inf, item.replies + "/" + item.views);
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent(ThisApp.context, PostListActivity.class);
                Thread thread = mListData.get(position);
                intent.putExtra("tid", thread.id);
                intent.putExtra("title", thread.title);
                startActivity(intent);
            }
        });
    }
}
