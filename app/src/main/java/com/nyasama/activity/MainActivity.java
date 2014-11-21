package com.nyasama.activity;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Response;
import com.nyasama.fragment.SimpleLayoutFragment;
import com.nyasama.fragment.ForumIndexFragment;
import com.nyasama.fragment.NavigationDrawerFragment;
import com.nyasama.R;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends FragmentActivity
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
                    startActivityForResult(intent, Discuz.REQUEST_CODE_LOGIN);
                }
            });
        }

        // TODO: move this to Splash Activity
        Discuz.execute("login", new HashMap<String, Object>(), null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject.has(Discuz.VOLLEY_ERROR))
                    Helper.toast("Sth is wrong with the server...");
                else
                    updateUserInfo();
            }
        });

        /*
        TODO: enable this
        PushAgent mPushAgent = PushAgent.getInstance(this);
        mPushAgent.enable();
        Log.d("DEVICETOKEN", UmengRegistrar.getRegistrationId(this));
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserInfo();
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

    public void updateUserInfo() {
        if (Discuz.sAuth != null && Discuz.sUsername != null) {
            TextView username = (TextView) findViewById(R.id.drawer_username);
            username.setText(Discuz.sUsername);
        }
        findViewById(R.id.show_logined).setVisibility(Discuz.sAuth != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.hide_logined).setVisibility(Discuz.sAuth != null ? View.GONE : View.VISIBLE);
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
                pager.setAdapter(new FragmentPagerAdapter(mActivity.getSupportFragmentManager()) {
                    @Override
                    public android.support.v4.app.Fragment getItem(int i) {
                        if (i == 0)
                            return new ForumIndexFragment();
                        else
                            // TODO: remove this
                            return new SimpleLayoutFragment();
                    }
                    @Override
                    public int getCount() {
                        return 3;
                    }
                    @Override
                    public CharSequence getPageTitle(int position) {
                        return position == 0 ?
                                mActivity.getString(R.string.title_main_home) :
                                // TODO: rename this
                                "Blank "+position;
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
