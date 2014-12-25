package com.nyasama.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.activity.ThreadListActivity;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.Forum;
import com.nyasama.util.Discuz.ForumCatalog;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Created by oxyflour on 2014/11/18.
 *
 */
public class DiscuzForumIndexFragment extends CommonListFragment
    implements CommonListFragment.OnListFragmentInteraction<Object> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        // hide the divider
        ((ListView) view.findViewById(R.id.list)).setDividerHeight(0);
        return view;
    }

    @Override
    public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
        return new CommonListAdapter() {

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                Object obj = DiscuzForumIndexFragment.this.getData(position);
                return obj instanceof ForumCatalog ? 0 : 1;
            }

            @Override
            public View createView(ViewGroup parent, int position) {
                Object obj = DiscuzForumIndexFragment.this.getData(position);
                int layout = obj instanceof ForumCatalog ?
                        R.layout.fragment_forum_cat_item :
                        R.layout.fragment_forum_item;
                return LayoutInflater.from(parent.getContext())
                        .inflate(layout, parent, false);
            }

            @Override
            public void convertView(ViewHolder viewHolder, Object obj) {
                if (obj instanceof ForumCatalog) {
                    ForumCatalog item = (ForumCatalog) obj;
                    viewHolder.setText(R.id.forum_cat_title, item.name);
                }
                else {
                    Forum item = (Forum) obj;
                    viewHolder.setText(R.id.title, item.name);
                    viewHolder.setText(R.id.sub,
                            getString(R.string.forum_index_threads)+":"+item.threads+"  "+
                            getString(R.string.forum_index_posts)+":"+item.todayPosts);
                    NetworkImageView icon = ((NetworkImageView) viewHolder.getView(R.id.icon));
                    icon.setDefaultImageResId(R.drawable.default_board_icon);
                    icon.setImageUrl(item.icon, ThisApp.imageLoader);
                }
            }
        };
    }

    @Override
    public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
        Object obj = getData(position);
        if (obj instanceof Forum) {
            Forum item = (Forum) obj;
            Intent intent = new Intent(view.getContext(), ThreadListActivity.class);
            intent.putExtra("fid", item.id);
            intent.putExtra("title", item.name);
            startActivity(intent);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoadingMore(CommonListFragment fragment, final List listData) {
        Discuz.execute("forumindex", new HashMap<String, Object>(), null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int total = -1;
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
                        listData.clear();
                        for (int i = 0; i < catlist.length(); i++) {
                            JSONObject cat = catlist.getJSONObject(i);
                            ForumCatalog forumCatalog = new ForumCatalog(cat.getString("name"));
                            listData.add(forumCatalog);

                            final JSONArray forumIds = cat.getJSONArray("forums");
                            for (int j = 0; j < forumIds.length(); j++) {
                                String id = forumIds.getString(j);
                                JSONObject forum = forums.getJSONObject(id);
                                listData.add(new Forum(forum));
                            }
                        }
                        total = listData.size();
                    } catch (JSONException e) {
                        Log.d("Discuz", "Load Forum Index Failed (" + e.getMessage() + ")");
                    }
                }
                loadMoreDone(total);
            }
        });

    }
}
