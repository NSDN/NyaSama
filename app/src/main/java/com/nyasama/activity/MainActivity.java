package com.nyasama.activity;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.fragment.NavigationDrawerFragment;
import com.nyasama.R;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        View view = mNavigationDrawerFragment.getView();
        if (view != null) {
            Button btn = (Button) view.findViewById(R.id.drawer_login);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_LOGIN);
                }
            });
        }
    }

    private final int REQUEST_CODE_LOGIN = 1;
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_LOGIN) {
            if (Discuz.sUserInfo != null) {
                TextView username = (TextView) findViewById(R.id.drawer_username);
                username.setText(Discuz.sUserInfo.optString("member_username"));
            }
            findViewById(R.id.show_logined).setVisibility(resultCode > 0 ? View.VISIBLE : View.GONE);
            findViewById(R.id.hide_logined).setVisibility(resultCode > 0 ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
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

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            if (getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
                mListView = (ListView)rootView.findViewById(R.id.forum_cat_list);
                loadForums();
            }
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }

        private class Forum {
            public String id;
            public String name;
        }
        private class ForumCatalog {
            public String name;
            public List<Forum> forums;
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
                        Helper.toast(mListView.getContext(), R.string.network_error_toast);
                    } else{
                        try {
                            JSONObject var = data.getJSONObject("Variables");
                            JSONArray forumlist = var.getJSONArray("forumlist");
                            final JSONObject forums = new JSONObject();
                            for (int i = 0; i < forumlist.length(); i ++) {
                                JSONObject forum = forumlist.getJSONObject(i);
                                forums.put(forum.getString("fid"), forum);
                            }

                            JSONArray catlist = var.getJSONArray("catlist");
                            mForumCatalogs = new ArrayList<ForumCatalog>();
                            for (int i = 0; i < catlist.length(); i ++) {
                                JSONObject cat = catlist.getJSONObject(i);
                                ForumCatalog forumCatalog = new ForumCatalog();
                                forumCatalog.name = cat.getString("name");

                                final JSONArray forumIds = cat.getJSONArray("forums");
                                forumCatalog.forums = new ArrayList<Forum>();
                                for (int j = 0; j < forumIds.length(); j ++) {
                                    final int idx = j;
                                    forumCatalog.forums.add(new Forum() {{
                                        this.id = forumIds.getString(idx);
                                        this.name = forums.getJSONObject(this.id).getString("name");
                                    }});
                                }
                                mForumCatalogs.add(forumCatalog);
                            }

                            displayForums();
                        }
                        catch (JSONException e) {
                            Log.d("ForumList", "Load Forum Index Failed (" + e.getMessage() + ")");
                            Helper.toast(mListView.getContext(), R.string.load_failed_toast);
                        }
                        catch (NullPointerException e) { }
                    }
                }
            });
        }
    }

}
