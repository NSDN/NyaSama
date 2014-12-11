package com.nyasama.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.Attachment;
import com.nyasama.util.Discuz.Post;
import com.nyasama.util.Helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.senab.photoview.PhotoViewAttacher;

public class AttachmentViewer extends FragmentActivity {

    private static String TAG = "ImageViewer";

    private ViewPager mPager;
    private TextView mTitle;
    private FragmentPagerAdapter mPageAdapter;
    private List<Attachment> mAttachmentList = new ArrayList<Attachment>();
    private Map<String, Bitmap> mBitmapCache = new HashMap<String, Bitmap>();

    public void showAttachmentList() {
        ListView listView = new ListView(this);
        List<String> names = new ArrayList<String>();
        for (Attachment attachment : mAttachmentList)
            names.add(attachment.name);
        listView.setAdapter(new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                names));
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Attachments")
                .setView(listView)
                .show();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mPager.setCurrentItem(i);
                dialog.cancel();
            }
        });
    }

    public void updatePagerTitle(int position) {
        mTitle.setVisibility(position >= 0 && mAttachmentList.size() > 0 ?
                View.VISIBLE : View.GONE);
        if (position >= 0) {
            int len = mAttachmentList.size();
            Attachment item = mAttachmentList.get(position);
            String text = item.name;
            if (len > 1)
                text += " (" + (position+1) + "/" + mAttachmentList.size() + ")";
            mTitle.setText(text);
        }
    }

    static Pattern msgPathPattern = Pattern.compile("<img[^>]* file=\"(.*?)\"");
    static Pattern msgMatcher = Pattern.compile("<ignore_js_op>(.*?)</ignore_js_op>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    static List<Attachment> compileAttachments(String message, final List<Attachment> attachments) {
        List<Attachment> newAttachments = new ArrayList<Attachment>();

        Map<String, Attachment> map = new HashMap<String, Attachment>();
        for (Attachment attachment : attachments) {
            map.put(attachment.src, attachment);
            if (!attachment.isImage)
                newAttachments.add(attachment);
        }

        Matcher matcher = msgMatcher.matcher(message);
        while (matcher.find()) {
            Matcher pathMatcher = msgPathPattern.matcher(matcher.group(1));
            if (pathMatcher.find()) {
                Attachment attachment = map.get(pathMatcher.group(1));
                if (attachment != null) newAttachments.add(attachment);
            }
        }

        return newAttachments;
    }

    public void loadAttachments() {
        Discuz.execute("viewthread", new HashMap<String, Object>() {{
            put("ppp", 1);
            put("tid", getIntent().getIntExtra("tid", 0));
            put("page", getIntent().getIntExtra("index", 0) + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                int position = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else if (data.opt("Message") instanceof JSONObject) {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        mAttachmentList.clear();
                        new AlertDialog.Builder(ThisApp.context)
                                .setTitle(R.string.there_is_something_wrong)
                                .setMessage(message.getString("messagestr"))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        finish();
                                    }
                                })
                                .show();
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load Post Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                else {
                    try {
                        JSONObject var = data.getJSONObject("Variables");
                        JSONArray postlist = var.getJSONArray("postlist");
                        mAttachmentList.clear();
                        if (postlist.length() >= 1) {
                            Post post = new Post(postlist.getJSONObject(0));
                            mAttachmentList = compileAttachments(post.message, post.attachments);
                        }
                        mPageAdapter.notifyDataSetChanged();
                        position = 0;
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                updatePagerTitle(position);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attachment_viewer);

        mPager = (ViewPager) findViewById(R.id.view_pager);
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int position) {
                updatePagerTitle(position);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        mPager.setAdapter(mPageAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(final int i) {
                return new Fragment() {
                    @Override
                    public View onCreateView(LayoutInflater inflater,
                                             ViewGroup container, Bundle savedInstanceState) {
                        Attachment attachment = mAttachmentList.get(i);
                        if (attachment.isImage) {
                            final ImageView imageView = new ImageView(container.getContext());
                            final String url = Discuz.DISCUZ_URL + attachment.src;
                            Bitmap bitmap = mBitmapCache.get(url);
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap);
                                new PhotoViewAttacher(imageView);
                            } else {
                                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                                ImageRequest imageRequest = new ImageRequest(url, new Response.Listener<Bitmap>() {
                                    @Override
                                    public void onResponse(Bitmap bitmap) {
                                        mBitmapCache.put(url, bitmap);
                                        imageView.setImageBitmap(bitmap);
                                        new PhotoViewAttacher(imageView);
                                    }
                                }, 0, 0, null, null);
                                ThisApp.requestQueue.add(imageRequest);
                            }
                            return imageView;
                        } else {
                            View view = inflater.inflate(R.layout.fragment_attachment_item, container, false);
                            TextView textView = (TextView) view.findViewById(R.id.name);
                            textView.setText(attachment.name + " (" + attachment.size + ")");
                            return view;
                        }
                    }
                };
            }

            @Override
            public int getCount() {
                return mAttachmentList.size();
            }
        });
        mTitle = (TextView) findViewById(R.id.view_title);
        mTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAttachmentList();
            }
        });

        loadAttachments();
    }

}
