package com.nyasama.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.fragment.DiscuzComicListFragment;
import com.nyasama.fragment.DiscuzForumIndexFragment;
import com.nyasama.fragment.DiscuzThreadListFragment;
import com.nyasama.fragment.NavigationDrawerFragment;
import com.nyasama.fragment.SimpleLayoutFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends BaseThemedActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks,
        DiscuzThreadListFragment.OnThreadListInteraction {

    public void loadUserInfo() {
        // TODO: find a better way to refresh group name
        Discuz.execute("forumindex", new HashMap<String, Object>(),
                null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        if (Discuz.sHasLogined) {
                            String avatar_url = Discuz.DISCUZ_URL +
                                    "uc_server/avatar.php?uid=" + Discuz.sUid + "&size=medium";
                            ((NetworkImageView) findViewById(R.id.drawer_avatar))
                                    .setImageUrl(avatar_url, ThisApp.imageLoader);
                            ((TextView) findViewById(R.id.drawer_username)).setText(Discuz.sUsername);
                            ((TextView) findViewById(R.id.drawer_group)).setText(Discuz.sGroupName);
                        }
                        findViewById(R.id.show_logined).setVisibility(Discuz.sHasLogined ? View.VISIBLE : View.GONE);
                        findViewById(R.id.hide_logined).setVisibility(Discuz.sHasLogined ? View.GONE : View.VISIBLE);
                    }
                });
    }

    public void gotoLogin(View view) {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivityForResult(intent, LoginActivity.REQUEST_CODE_LOGIN);
    }

    public void gotoProfile(View view) {
        startActivity(new Intent(MainActivity.this, UserProfileActivity.class));
    }

    public void gotoDonate(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://thwiki.cc/THBWiki:%E6%8D%90%E6%AC%BE")));
    }

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    // REF: http://stackoverflow.com/questions/8430805/android-clicking-twice-the-back-button-to-exit-activity
    private boolean doubleBackToExitPressedOnce;
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            finish();
            return;
        }

        doubleBackToExitPressedOnce = true;
        Helper.toast(getString(R.string.toast_click_again_to_exit));

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    BroadcastReceiver mLoginReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadUserInfo();
        }
    };

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(ThisApp.context).unregisterReceiver(mLoginReceiver);
        super.onDestroy();
    }

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
                (DrawerLayout) findViewById(R.id.drawer_layout),
                new ArrayList<String>() {{
                    add(getString(R.string.title_section1));
                    add(getString(R.string.title_section4));
                    add(getString(R.string.title_section5));
                    add(getString(R.string.title_section2));
                    add(getString(R.string.title_section3));
                }});

        /*
        TODO: enable this to use umeng
        PushAgent mPushAgent = PushAgent.getInstance(this);
        mPushAgent.enable();
        Log.d("DEVICETOKEN", UmengRegistrar.getRegistrationId(this));
        */

        loadUserInfo();

        LocalBroadcastManager.getInstance(ThisApp.context)
                .registerReceiver(mLoginReceiver, new IntentFilter(Discuz.BROADCAST_FILTER_LOGIN));
    }

    @Override
    public boolean onNavigationDrawerItemSelected(int position) {
        // return >=0 to prevent item from checked
        if (position == 1) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        else if (position == 2) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        else if (position == 3) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://thwiki.cc")));
            return true;
        }
        else if (position == 4) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://thvideo.tv")));
            return true;
        }
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
        return false;
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            /**case 4:
                mTitle = getString(R.string.title_section2);
                break;
            case 5:
                mTitle = getString(R.string.title_section3);
                break;**/
            case 2:
                mTitle = getString(R.string.title_section4);
                break;
            case 3:
                mTitle = getString(R.string.title_section5);
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
        int id = item.getItemId();
        if (id == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        }
        else if (id == android.R.id.home) {
            mNavigationDrawerFragment.onOptionsItemSelected(item);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onGetThreadData(DiscuzThreadListFragment fragment) {
        if (fragment.getMessage() != null) {
            new AccentAlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.error_no_internet)
                    .setMessage(fragment.getMessage())
                    .setPositiveButton(android.R.string.yes, null)
                    .show();
        }
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
        public MainActivity mActivity;

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
            if (getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
                View rootView = inflater.inflate(R.layout.fragment_main_home, container, false);
                ViewPager pager = (ViewPager) rootView.findViewById(R.id.view_pager);
                pager.setAdapter(new FragmentStatePagerAdapter(mActivity.getSupportFragmentManager()) {
                    @Override
                    public android.support.v4.app.Fragment getItem(int i) {
                        if (i == 0)
                            return new DiscuzForumIndexFragment();
                        else if (i == 1)
                            // Note: Discuz returns 50 hot threads by default.
                            // we set the page size to be 60 so that it will not load more
                            return DiscuzThreadListFragment.getNewFragment(0, 0, 60);
                        else if (i == 2)
                            return DiscuzComicListFragment.getNewFragment();
                        else
                            return new SimpleLayoutFragment();
                    }
                    @Override
                    public int getCount() {
                        return 3;
                    }
                    @Override
                    public CharSequence getPageTitle(int position) {
                        int[] titles = {
                                R.string.title_main_home,
                                R.string.title_main_hot_threads,
                                R.string.title_main_translated,
                        };
                        return position < titles.length ? getString(titles[position]) : "Blank "+position;
                    }
                });
                return rootView;
            }
            return inflater.inflate(R.layout.fragment_blank, container, false);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mActivity = (MainActivity) activity;
            mActivity.onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
