package com.nyasama.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.activity.ThreadListActivity;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;
import com.nyasama.util.Discuz.Forum;
import com.nyasama.util.Discuz.ForumCatalog;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_forum_index, container, false);
        mListView = (ListView) rootView.findViewById(R.id.forum_cat_list);
        loadForums();
        return rootView;
    }

    private List<ForumCatalog> mForumCatalogs;
    private ListView mListView;
    public void displayForums() {
        mListView.setAdapter(new CommonListAdapter<ForumCatalog>(mForumCatalogs,
                R.layout.fragment_forum_cat_item) {
            @Override
            public void convert(ViewHolder viewHolder, ForumCatalog item) {
                viewHolder.setText(R.id.forum_cat_title, item.name);
                // bind the grid view
                GridView gridView = (GridView)viewHolder.getView(R.id.forum_list);
                gridView.setAdapter(new CommonListAdapter<Forum>(item.forums,
                        R.layout.fragment_forum_item) {
                    @Override
                    public void convert(ViewHolder viewHolder, Forum item) {
                        Button btn = (Button) viewHolder.getView(R.id.button);
                        btn.setText(item.name);
                        final String fid = item.id;
                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(view.getContext(), ThreadListActivity.class);
                                intent.putExtra("fid", fid);
                                startActivity(intent);
                            }
                        });
                    }
                });
            }
        });
    }
    public void loadForums() {
        Discuz.execute("forumindex", new HashMap<String, Object>(), null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else {
                    try {
                        JSONObject var = data.getJSONObject("Variables");
                        JSONArray forumlist = var.getJSONArray("forumlist");
                        final JSONObject forums = new JSONObject();
                        for (int i = 0; i < forumlist.length(); i++) {
                            JSONObject forum = forumlist.getJSONObject(i);
                            forums.put(forum.getString("fid"), forum);
                        }

                        JSONArray catlist = var.getJSONArray("catlist");
                        mForumCatalogs = new ArrayList<ForumCatalog>();
                        for (int i = 0; i < catlist.length(); i++) {
                            JSONObject cat = catlist.getJSONObject(i);
                            ForumCatalog forumCatalog = new ForumCatalog();
                            forumCatalog.name = cat.getString("name");

                            final JSONArray forumIds = cat.getJSONArray("forums");
                            forumCatalog.forums = new ArrayList<Forum>();
                            for (int j = 0; j < forumIds.length(); j++) {
                                final int idx = j;
                                forumCatalog.forums.add(new Forum() {{
                                    this.id = forumIds.getString(idx);
                                    this.name = forums.getJSONObject(this.id).getString("name");
                                }});
                            }
                            mForumCatalogs.add(forumCatalog);
                        }

                        // show it
                        displayForums();
                    } catch (JSONException e) {
                        Log.d("ForumList", "Load Forum Index Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                    // TODO: remove these
                    catch (NullPointerException e) { /**/ }
                }
            }
        });
    }

}
