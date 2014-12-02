package com.nyasama.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.activity.ThreadListActivity;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.Forum;
import com.nyasama.util.Discuz.ForumCatalog;
import com.nyasama.util.Helper;

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
public class ForumIndexFragment extends android.support.v4.app.Fragment {

    private List<Object> mForumListData = new ArrayList<Object>();
    private BroadcastReceiver mLoginReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadForums();
        }
    };

    public void loadForums() {
        Discuz.execute("forumindex", new HashMap<String, Object>(), null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else if (!data.isNull("Variables")) {
                    try {
                        JSONObject var = data.getJSONObject("Variables");
                        JSONArray forumlist = var.getJSONArray("forumlist");
                        final JSONObject forums = new JSONObject();
                        for (int i = 0; i < forumlist.length(); i++) {
                            JSONObject forum = forumlist.getJSONObject(i);
                            forums.put(forum.getString("fid"), forum);
                        }

                        JSONArray catlist = var.getJSONArray("catlist");
                        mForumListData.clear();
                        for (int i = 0; i < catlist.length(); i++) {
                            JSONObject cat = catlist.getJSONObject(i);
                            ForumCatalog forumCatalog = new ForumCatalog();
                            forumCatalog.name = cat.getString("name");
                            mForumListData.add(forumCatalog);

                            final JSONArray forumIds = cat.getJSONArray("forums");
                            for (int j = 0; j < forumIds.length(); j++) {
                                String id = forumIds.getString(j);
                                JSONObject forum = forums.getJSONObject(id);
                                mForumListData.add(new Forum(forum));
                            }
                        }
                    } catch (JSONException e) {
                        Log.d("Discuz", "Load Forum Index Failed (" + e.getMessage() + ")");
                    }
                }
                displayForums();
            }
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_forum_index, container, false);
        mListView = (ListView) rootView.findViewById(R.id.list);
        LocalBroadcastManager.getInstance(ThisApp.context)
                .registerReceiver(mLoginReceiver, new IntentFilter("login"));
        loadForums();
        return rootView;
    }

    private ListView mListView;
    public void displayForums() {
        mListView.setAdapter(new CommonListAdapter<Object>(mForumListData,
                R.layout.fragment_forum_cat_item) {

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                return mForumListData.get(position) instanceof ForumCatalog ? 0 : 1;
            }

            @Override
            public View createView(ViewGroup parent, int position) {
                int layout = mForumListData.get(position) instanceof ForumCatalog ?
                        R.layout.fragment_forum_cat_item :
                        R.layout.fragment_forum_item;
                return LayoutInflater.from(parent.getContext())
                        .inflate(layout, parent, false);
            }

            @Override
            public void convert(ViewHolder viewHolder, Object obj) {
                if (obj instanceof ForumCatalog) {
                    ForumCatalog item = (ForumCatalog) obj;
                    viewHolder.setText(R.id.forum_cat_title, item.name);
                }
                else {
                    Forum item = (Forum) obj;
                    viewHolder.setText(R.id.title, item.name);
                    viewHolder.setText(R.id.sub,
                            "threads:"+item.threads+"  posts:"+item.todayPosts+"/"+item.posts);
                    ((NetworkImageView) viewHolder.getView(R.id.icon))
                            .setImageUrl(item.icon, ThisApp.imageLoader);
                }
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Object obj = mForumListData.get(i);
                if (obj instanceof Forum) {
                    Forum item = (Forum) obj;
                    Intent intent = new Intent(view.getContext(), ThreadListActivity.class);
                    intent.putExtra("fid", item.id);
                    intent.putExtra("title", item.name);
                    startActivity(intent);
                }
            }
        });
    }
}
