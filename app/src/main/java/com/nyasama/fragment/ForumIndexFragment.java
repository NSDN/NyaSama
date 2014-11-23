package com.nyasama.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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

import org.json.JSONObject;

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
        if (Discuz.sForumCatalogs.size() > 0) {
            displayForums();
        }
        else Discuz.loadForums(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                displayForums();
            }
        });
        return rootView;
    }

    private ListView mListView;
    public void displayForums() {
        mListView.setAdapter(new CommonListAdapter<ForumCatalog>(Discuz.sForumCatalogs,
                R.layout.fragment_forum_cat_item) {
            @Override
            public void convert(ViewHolder viewHolder, ForumCatalog item) {
                viewHolder.setText(R.id.forum_cat_title, item.name);
                // bind the grid view
                final List<Forum> forums = item.forums;
                AbsListView listView = (AbsListView)viewHolder.getView(R.id.forum_list);
                listView.setAdapter(new CommonListAdapter<Forum>(forums,
                        R.layout.fragment_forum_item) {
                    @Override
                    public void convert(ViewHolder viewHolder, final Forum item) {
                        viewHolder.setText(R.id.title, item.name);
                        viewHolder.setText(R.id.sub,
                                "threads:"+item.threads+"  posts:"+item.todayPosts+"/"+item.posts);
                        ((NetworkImageView) viewHolder.getView(R.id.icon))
                                .setImageUrl(item.icon, ThisApp.imageLoader);
                    }
                });
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Forum item = forums.get(i);
                        Intent intent = new Intent(view.getContext(), ThreadListActivity.class);
                        intent.putExtra("fid", item.id);
                        intent.putExtra("title", item.name);
                        startActivity(intent);
                    }
                });
            }
        });
    }
}
