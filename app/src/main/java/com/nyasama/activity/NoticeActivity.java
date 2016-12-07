package com.nyasama.activity;

import android.support.v4.view.PagerTabStrip;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.fragment.DiscuzNoticeListFragment;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.fragment.CommonListFragment;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NoticeActivity extends AppCompatActivity {

    static class NoticeItem {
        int viewIndex;
        int typeIndex;
        String title1;
        String title2;
    }

    final static String[] NOTICE_VIEWS = {
        "mypost",
        "interactive",
        "system",
        "manage",
        "app"
    };
    final static String[][] NOTICE_TYPES = {
        {"post", "pcomment", "activity", "reward", "goods", "at"},
        {"poke", "friend", "wall", "comment", "click", "sharenotice"},
        {"system"},
        {"manage"},
        {"app"},
    };

    // TODO: load strings from resource
    static String[] sViewNames = {
            "我的帖子",
            "坛友互动",
            "系统提醒",
            "管理工作",
            "应用提醒",
    };
    static String[][] sTypeNames = {
            {"帖子", "点评", "活动", "悬赏", "商品", "提到我的"},
            {"打招呼", "好友", "留言", "评论", "挺你", "分享"},
            {"系统提醒"},
            {"管理工作"},
            {"应用提醒"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_framelayout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_action_nya);
        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                finish();
            }
        });

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;

        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        List<String> spinnerTexts = new ArrayList<String>(){{
            add(getString(R.string.new_prompts));
            for (String name : sViewNames)
                add(name);
        }};
        final ArrayAdapter spinnerAdapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
                R.layout.fragment_spinner_item_2, android.R.id.text1, spinnerTexts) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view.findViewById(android.R.id.text2)).setText(getTitle());
                return view;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        actionBar.setListNavigationCallbacks(spinnerAdapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(final int i, long l) {
                Fragment fragment;
                if (i == 0)
                    fragment = new NewNoticeListFragment();
                else if (NOTICE_TYPES[i - 1].length <= 1)
                    fragment = DiscuzNoticeListFragment.getNewFragment(NOTICE_VIEWS[i - 1], "");
                else
                    fragment = ViewPagerFragment.getNewFragment(i - 1);

                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();

                return true;
            }
        });

        int viewIndex = getIntent().getIntExtra("viewIndex", -1);
        if (viewIndex >= 0) {
            actionBar.setSelectedNavigationItem(viewIndex + 1);
            getIntent().removeExtra("viewIndex");
        }
    }

    public static class NewNoticeListFragment extends CommonListFragment<NoticeItem>
            implements CommonListFragment.OnListFragmentInteraction<NoticeItem> {
        @Override
        public CommonListAdapter getListViewAdaptor(CommonListFragment fragment) {
            return new CommonListAdapter<NoticeItem>() {
                @Override
                public View createView(ViewGroup parent, int position) {
                    return LayoutInflater.from(parent.getContext())
                            .inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                @Override
                public void convertView(ViewHolder viewHolder, NoticeItem item) {
                    ((TextView) viewHolder.getView(android.R.id.text1)).setText(item.title1);
                    ((TextView) viewHolder.getView(android.R.id.text2)).setText(item.title2);
                }
            };
        }
        @Override
        public void onItemClick(CommonListFragment fragment, View view, int position, long id) {
            NoticeItem item = (NoticeItem) fragment.getData(position);
            Intent intent = getActivity().getIntent();
            intent.putExtra("viewIndex", item.viewIndex);
            intent.putExtra("typeIndex", item.typeIndex);
            startActivity(intent);
        }
        @Override
        public void onLoadingMore(final CommonListFragment fragment, final List data) {
            Discuz.execute("profile", new HashMap<String, Object>(), null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if (response.has(Discuz.VOLLEY_ERROR)) {
                        Helper.toast(R.string.network_error_toast);
                    }
                    else {
                        JSONObject var = response.optJSONObject("Variables");
                        JSONObject notifyViewNumbers = var != null ? var.optJSONObject("notice_number") : null;
                        final JSONObject notifyTypeNumbers = var != null ? var.optJSONObject("prompt_number") : null;
                        if (notifyViewNumbers != null) for (int i = 0; i < NOTICE_VIEWS.length; i ++) {
                            if (notifyTypeNumbers != null && notifyViewNumbers.has(NOTICE_VIEWS[i])) for (int j = 0; j < NOTICE_TYPES[i].length; j ++) {
                                final int finalI = i;
                                final int finalJ = j;
                                if (notifyTypeNumbers.has(NOTICE_TYPES[i][j])) data.add(new NoticeItem(){{
                                    viewIndex = finalI;
                                    typeIndex = finalJ;
                                    int number = notifyTypeNumbers.optInt(NOTICE_TYPES[finalI][finalJ]);
                                    title1 = sTypeNames[viewIndex][typeIndex];
                                    title2 = String.format(getString(R.string.click_to_view_prompts), number);
                                }});
                            }
                        }
                    }
                    fragment.loadMoreDone(data.size());
                }
            });
        }
    }

    public static class ViewPagerFragment extends Fragment {
        static ViewPagerFragment getNewFragment(int index) {
            ViewPagerFragment fragment = new ViewPagerFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("viewIndex", index);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final int index = getArguments().getInt("viewIndex");
            final String viewId = NOTICE_VIEWS[index];
            final String[] typeIds = NOTICE_TYPES[index];
            final String[] typeNames = sTypeNames[index];
            View rootView = inflater.inflate(R.layout.fragment_main_home, container, false);
            ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
            viewPager.setAdapter(new FragmentStatePagerAdapter(getActivity().getSupportFragmentManager()) {
                @Override
                public Fragment getItem(int position) {
                    return DiscuzNoticeListFragment.getNewFragment(viewId, typeIds[position]);
                }
                @Override
                public int getCount() {
                    return typeIds.length;
                }
                @Override
                public CharSequence getPageTitle(int position) {
                    return typeNames[position];
                }
            });

            Intent intent = getActivity().getIntent();
            int typeIndex = intent.getIntExtra("typeIndex", -1);
            if (typeIndex > 0) {
                viewPager.setCurrentItem(typeIndex, false);
                intent.removeExtra("typeIndex");
            }

            PagerTabStrip tab = (PagerTabStrip) rootView.findViewById(R.id.view_pager_tab);
            ((ViewPager.LayoutParams) tab.getLayoutParams()).isDecor = true;

            return rootView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_notice, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
