package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageRequest;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.BitmapLruCache;
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

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class AttachmentViewer extends BaseThemedActivity {

    private static class ExternalImageAttachment extends Attachment {
        public ExternalImageAttachment(String src) {
            this.isImage = true;
            this.name = src;
            this.src = src;
            this.size = "0kb";
        }
    }

    private static class RedirectPostAttachment extends Attachment {
        public RedirectPostAttachment(String name) {
            this.name = name;
        }
    }

    private static float REDIRECT_PAGER_WIDTH = 0.3f;
    private static int MAX_TEXTURE_SIZE = 2048;
    private static int IMAGE_THUMB_SIZE = 128;

    private ViewPager mPager;
    private FragmentStatePagerAdapter mPageAdapter;

    private List<Attachment> mAttachmentList = new ArrayList<Attachment>();
    private BitmapLruCache mBitmapCache = new BitmapLruCache();
    private Map<String, Bitmap> mThumbCache = new HashMap<String, Bitmap>();
    private boolean mHasAttachmentsPrev;
    private boolean mHasAttachmentsNext;

    private RequestQueue mRequestQueue = new RequestQueue(ThisApp.volleyCache, new BasicNetwork(new HurlStack()));

    static int getPageSize(int b, int e) {
        int p = e - b;
        for (; p < e; p ++) {
            int c = b / p;
            if (c * p <= b & c * p + p >= e)
                return p;
        }
        return p;
    }

    public void showAttachmentList() {
        List<String> names = new ArrayList<String>();
        for (Attachment attachment : mAttachmentList)
            names.add(attachment.name);

        final ListView listView = new ListView(this);
        listView.setAdapter(new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                names));
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setItemChecked(mPager.getCurrentItem(), true);
        final AlertDialog dialog = new AccentAlertDialog.Builder(this)
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
        TextView title = (TextView) findViewById(R.id.view_title);
        if (position >= 0 && position < mAttachmentList.size() &&
                !(mAttachmentList.get(position) instanceof RedirectPostAttachment)) {
            title.setVisibility(View.VISIBLE);
            int index = position +
                    (mHasAttachmentsPrev ? 0 : 1 );
            int total = mAttachmentList.size() -
                    (mHasAttachmentsPrev ? 1 : 0) -
                    (mHasAttachmentsNext ? 1 : 0);
            title.setText(index + "/" + total);
        }
        else {
            title.setVisibility(View.GONE);
        }
    }

    static Pattern msgPathPattern = Pattern.compile("<img[^>]* file=\"(.*?)\"");
    static List<Attachment> compileAttachments(String message, final List<Attachment> attachments) {
        List<Attachment> list = new ArrayList<Attachment>();
        Matcher matcher;

        Map<String, Attachment> map = new HashMap<String, Attachment>();
        for (Attachment attachment : attachments)
            map.put(attachment.src, attachment);

        message = message.replaceAll(" src=\"(.*?)\"", " file=\"$1\"");

        matcher = msgPathPattern.matcher(message);
        while (matcher.find()) {
            String src = matcher.group(1);
            // attachment image
            if (map.containsKey(src)) {
                list.add(map.get(src));
                map.remove(src);
            }
            // external images
            else if (!Discuz.getSafeUrl(src).startsWith(Discuz.DISCUZ_HOST)) {
                list.add(new ExternalImageAttachment(src));
            }
        }

        // add the rest attachments
        for (Map.Entry<String, Attachment> entry : map.entrySet())
            list.add(entry.getValue());

        return list;
    }

    public void loadAttachments() {
        Helper.updateVisibility(findViewById(R.id.loading), true);
        final int index = getIntent().getIntExtra("index", 0);
        final int pageSize = getPageSize(index > 0 ? index - 1 : 0, index + 2);
        final int pageIndex = index / pageSize;
        Discuz.execute("viewthread", new HashMap<String, Object>() {{
            put("ppp", pageSize);
            put("page", pageIndex + 1);
            put("tid", getIntent().getIntExtra("tid", 0));
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                Helper.updateVisibility(findViewById(R.id.loading), false);
                int position = -1;
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else if (data.opt("Message") instanceof JSONObject) {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        mAttachmentList.clear();
                        new AccentAlertDialog.Builder(ThisApp.context)
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
                        Log.e(AttachmentViewer.class.toString(),
                                "JsonError: Load Post Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                else {
                    try {
                        JSONObject var = data.getJSONObject("Variables");
                        JSONArray postlist = var.getJSONArray("postlist");
                        mAttachmentList.clear();
                        int i = index - pageIndex * pageSize;
                        if (postlist.length() > i) {
                            Post post = new Post(postlist.getJSONObject(i));
                            mAttachmentList = compileAttachments(post.message, post.attachments);
                            if (i - 1 >= 0) {
                                post = new Post(postlist.getJSONObject(i - 1));
                                int attachments = compileAttachments(post.message, post.attachments).size();
                                if (mHasAttachmentsPrev = attachments > 0)
                                    mAttachmentList.add(0, new RedirectPostAttachment(String.format(
                                            getString(R.string.goto_prev_post_attachments), attachments)));
                            }
                            if (i + 1 < postlist.length()) {
                                post = new Post(postlist.getJSONObject(i + 1));
                                int attachments = compileAttachments(post.message, post.attachments).size();
                                if (mHasAttachmentsNext = attachments > 0)
                                    mAttachmentList.add(new RedirectPostAttachment(String.format(
                                            getString(R.string.goto_next_post_attachments), attachments)));
                            }
                        }
                        mPageAdapter.notifyDataSetChanged();

                        final String src = getIntent().getStringExtra("src");
                        mPager.post(new Runnable() {
                            @Override
                            public void run() {
                                if (src != null) for (int i = 0; i < mAttachmentList.size(); i ++)
                                    if (src.equals(mAttachmentList.get(i).src)) {
                                        mPager.setCurrentItem(i, false);
                                        return;
                                    }
                                if (mHasAttachmentsPrev)
                                    mPager.setCurrentItem(1, false);
                            }
                        });

                        position = 0;
                    }
                    catch (JSONException e) {
                        Log.e(AttachmentViewer.class.toString(),
                                "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
                updatePagerTitle(position);
            }
        });
    }

    public void gotoPrevPost() {
        Intent intent = getIntent();
        intent.putExtra("index", intent.getIntExtra("index", 1) - 1);
        startActivity(intent);
        finish();
    }

    public void gotoNextPost() {
        Intent intent = getIntent();
        intent.putExtra("index", intent.getIntExtra("index", -1) + 1);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
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
                getIntent().putExtra("src", mAttachmentList.get(position).src);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    int current = mPager.getCurrentItem();
                    if (current == 0 && mHasAttachmentsPrev)
                        gotoPrevPost();
                    else if (current == mAttachmentList.size() - 1 && mHasAttachmentsNext)
                        gotoNextPost();
                }
            }
        });
        mPager.setAdapter(mPageAdapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {

            @Override
            public Fragment getItem(final int i) {
                return AttachmentFragment.getNewFragment(i);
            }

            @Override
            public int getCount() {
                return mAttachmentList.size();
            }

            @Override
            public float getPageWidth(int position) {
                if (mAttachmentList.get(position) instanceof RedirectPostAttachment)
                    return REDIRECT_PAGER_WIDTH;
                return super.getPageWidth(position);
            }
        });
        findViewById(R.id.view_title).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAttachmentList();
            }
        });

        mRequestQueue.start();

        loadAttachments();
    }

    public static class AttachmentFragment extends Fragment {
        public static Fragment getNewFragment(int position) {
            AttachmentFragment fragment = new AttachmentFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            fragment.setArguments(bundle);
            return fragment;
        }

        private AttachmentViewer mActivity;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mActivity = (AttachmentViewer) activity;
        }

        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {

            final Bundle bundle = getArguments();
            final int position = bundle.getInt("position");
            if (position < mActivity.mAttachmentList.size()) {
                // save EVERYTHING in Bundle
                Attachment attachment = mActivity.mAttachmentList.get(position);
                bundle.putBoolean("isBlank", attachment instanceof RedirectPostAttachment);
                bundle.putBoolean("isImage", attachment.isImage);
                bundle.putString("src", attachment.src);
                bundle.putString("name", attachment.name);
                bundle.putString("size", attachment.size);
            }

            final String src = bundle.getString("src");
            if (bundle.getBoolean("isBlank")) {
                TextView textView = new TextView(container.getContext());
                textView.setText(bundle.getString("name"));
                textView.setPadding(16, 16, 16, 16);
                textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT));
                textView.setGravity(Gravity.CENTER);
                return textView;
            }
            else if (bundle.getBoolean("isImage")) {
                final PhotoView photoView = new PhotoView(container.getContext());
                Bitmap bitmap = mActivity.mBitmapCache.getBitmap(src);
                if (bitmap != null) {
                    photoView.setImageBitmap(bitmap);
                    new PhotoViewAttacher(photoView);
                } else {
                    Bitmap thumb = mActivity.mThumbCache.get(src);
                    if (thumb != null)
                        photoView.setImageBitmap(thumb);
                    else
                        photoView.setImageResource(android.R.drawable.ic_menu_gallery);
                    ImageRequest imageRequest = new ImageRequest(Discuz.getSafeUrl(src), new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap bitmap) {
                            // Note: On some old devices like Galaxy Nexus,
                            // images larger than 2048x2048 will not be rendered.
                            // As volley is facing OOM when resizing images
                            // we have to resize it here
                            if (bitmap.getWidth() > MAX_TEXTURE_SIZE ||
                                    bitmap.getHeight() > MAX_TEXTURE_SIZE) {
                                try {
                                    bitmap = Helper.getFittedBitmap(bitmap,
                                            MAX_TEXTURE_SIZE, MAX_TEXTURE_SIZE, false);
                                }
                                catch (OutOfMemoryError e) {
                                    bitmap = Helper.getFittedBitmap(bitmap,
                                            MAX_TEXTURE_SIZE / 2, MAX_TEXTURE_SIZE / 2, false);
                                }
                            }
                            mActivity.mBitmapCache.putBitmap(src, bitmap);
                            mActivity.mThumbCache.put(src, Helper.getFittedBitmap(bitmap,
                                    IMAGE_THUMB_SIZE, IMAGE_THUMB_SIZE, true));
                            photoView.setImageBitmap(bitmap);
                        }
                    }, 0, 0, null, null);
                    mActivity.mRequestQueue.add(imageRequest);
                }
                return photoView;
            } else {
                View view = inflater.inflate(R.layout.fragment_attachment_item, container, false);
                TextView textView = (TextView) view.findViewById(R.id.name);
                textView.setText(bundle.getString("name") + " (" + bundle.getString("size") + ")");
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(Discuz.getSafeUrl(src))));
                    }
                });
                return view;
            }
        }
    }
}
