package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
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
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.Discuz;
import com.nyasama.util.Discuz.Attachment;
import com.nyasama.util.Discuz.Post;
import com.nyasama.util.Helper;
import com.nyasama.util.SimpleIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.co.senab.photoview.PhotoView;

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

    private ViewPager mPager;
    private FragmentStatePagerAdapter mPageAdapter;
    private SimpleIndicator mIndicator;

    private List<Attachment> mAttachmentList = new ArrayList<Attachment>();
    private Discuz.Thread mThread;
    private Discuz.Post mPost;
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
            names.add(attachment.name.replace('\n', ' '));

        final ListView listView = new ListView(this);
        listView.setAdapter(new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
                names));
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setItemChecked(mPager.getCurrentItem(), true);
        final AlertDialog dialog = new AccentAlertDialog.Builder(AttachmentViewer.this)
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

    static Pattern msgAttachPattern = Pattern.compile("\\[attach\\](\\d+?)\\[/attach\\]");
    static Pattern msgPathPattern = Pattern.compile("<img[^>]* file=\"(.*?)\"");
    static List<Attachment> compileAttachments(String message, final List<Attachment> attachments) {
        List<Attachment> list = new ArrayList<Attachment>();
        Matcher matcher;

        Map<String, Attachment> sourceMap = new HashMap<String, Attachment>();
        SparseArray<String> idToSourceMap = new SparseArray<String>();
        for (Attachment attachment : attachments) {
            sourceMap.put(attachment.src, attachment);
            idToSourceMap.put(attachment.id, attachment.src);
        }

        message = message.replaceAll(" src=\"(.*?)\"", " file=\"$1\"");

        matcher = msgAttachPattern.matcher(message);
        while (matcher.find()) {
            int attachmentId = Helper.toSafeInteger(matcher.group(1), 0);
            String src = attachmentId > 0 ? idToSourceMap.get(attachmentId) : null;
            if (src != null && sourceMap.containsKey(src)) {
                list.add(sourceMap.get(src));
                sourceMap.remove(src);
            }
        }

        matcher = msgPathPattern.matcher(message);
        while (matcher.find()) {
            String src = matcher.group(1);
            // attachment image
            if (sourceMap.containsKey(src)) {
                list.add(sourceMap.get(src));
                sourceMap.remove(src);
            }
            // external images
            else if (!Discuz.getSafeUrl(src).startsWith(Discuz.DISCUZ_HOST)) {
                list.add(new ExternalImageAttachment(src));
            }
        }

        // add the rest attachments
        for (Map.Entry<String, Attachment> entry : sourceMap.entrySet())
            list.add(entry.getValue());

        return list;
    }

    interface AttachmentLoadedListener {
        void onResponse(JSONObject data, int dataIndex);
    }
    private static int sCachedThreadId;
    private static Map<String, JSONObject> sCachedAttachmentJson = new HashMap<String, JSONObject>();
    private void parseAttachments(JSONObject data, int dataIndex) {
        int position = -1;
        if (data.has(Discuz.VOLLEY_ERROR)) {
            Helper.toast(R.string.network_error_toast);
        }
        else if (data.opt("Message") instanceof JSONObject) {
            try {
                JSONObject message = data.getJSONObject("Message");
                mAttachmentList.clear();
                new AccentAlertDialog.Builder(AttachmentViewer.this)
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
                mThread = var.has("thread") ? new Discuz.Thread(var.getJSONObject("thread")) : null;

                JSONArray postlist = var.getJSONArray("postlist");
                mAttachmentList.clear();
                if (postlist.length() > dataIndex) {
                    mPost = new Post(postlist.getJSONObject(dataIndex));
                    mAttachmentList = compileAttachments(mPost.message, mPost.attachments);

                    // get src of last attachment
                    if ("-1".equals(getIntent().getStringExtra("src"))) {
                        int size = mAttachmentList.size();
                        if (size > 0)
                            getIntent().putExtra("src", mAttachmentList.get(size - 1).src);
                    }

                    // check previous post
                    if (dataIndex - 1 >= 0) {
                        Post post = new Post(postlist.getJSONObject(dataIndex - 1));
                        int attachments = compileAttachments(post.message, post.attachments).size();
                        if (mHasAttachmentsPrev = attachments > 0)
                            mAttachmentList.add(0, new RedirectPostAttachment(String.format(
                                    getString(R.string.goto_prev_post_attachments), attachments)));
                    }

                    // check next post
                    if (dataIndex + 1 < postlist.length()) {
                        Post post = new Post(postlist.getJSONObject(dataIndex + 1));
                        int attachments = compileAttachments(post.message, post.attachments).size();
                        if (mHasAttachmentsNext = attachments > 0)
                            mAttachmentList.add(new RedirectPostAttachment(String.format(
                                    getString(R.string.goto_next_post_attachments), attachments)));
                    }
                }
                mPageAdapter.notifyDataSetChanged();

                // select the page by src
                final String src = getIntent().getStringExtra("src");
                mPager.post(new Runnable() {
                    @Override
                    public void run() {
                        if (src != null) for (int i = 0; i < mAttachmentList.size(); i ++)
                            if (src.equals(mAttachmentList.get(i).src)) {
                                mPager.setCurrentItem(i, false);
                                return;
                            }
                        mPager.setCurrentItem(mHasAttachmentsPrev ? 1 : 0, false);
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

        mIndicator = (SimpleIndicator) findViewById(R.id.view_title);
        int total = mAttachmentList.size() -
                (mHasAttachmentsPrev ? 1 : 0) -
                (mHasAttachmentsNext ? 1 : 0);
        mIndicator.createIndicators(total, R.layout.fragment_attachment_indicator);
        mIndicator.setActive(position);
    }
    public void loadAttachments(int indexOffset, final AttachmentLoadedListener callback) {
        Intent intent = getIntent();
        final int index = intent.getIntExtra("index", 0) + indexOffset;
        final int pageSize = getPageSize(index > 0 ? index - 1 : 0, index + 2);
        final int pageIndex = index / pageSize;
        final int tid = intent.getIntExtra("tid", 0);
        final int authorId = intent.getIntExtra("authorid", 0);
        final boolean reversed = intent.getBooleanExtra("reverse", false);

        final int dataIndex = index - pageSize * pageIndex;
        final String cacheKey = Helper.toSafeMD5(pageSize+"|"+pageIndex+"|"+tid+"|"+authorId+"|"+reversed);
        if (sCachedAttachmentJson.containsKey(cacheKey)) {
            if (callback != null)
                callback.onResponse(sCachedAttachmentJson.get(cacheKey), dataIndex);
        }
        else Discuz.execute("viewthread", new HashMap<String, Object>() {{
            put("ppp", pageSize);
            put("page", pageIndex + 1);
            put("tid", tid);
            if (authorId > 0)
                put("authorid", authorId);
            if (reversed)
                put("ordertype", 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (sCachedThreadId != tid) {
                    sCachedAttachmentJson.clear();
                    sCachedThreadId = tid;
                }
                if (data != null && !data.has(Discuz.VOLLEY_ERROR))
                    sCachedAttachmentJson.put(cacheKey, data);
                if (callback != null)
                    callback.onResponse(data, dataIndex);
            }
        });
    }

    public void gotoPrevPost() {
        Intent intent = getIntent();
        intent.putExtra("src", "-1");
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
                int index = position +
                        (mHasAttachmentsPrev ? 0 : 1 ) - 1;
                if (mIndicator != null) mIndicator.setActive(index);
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
                AttachmentFragment fragment = new AttachmentFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("position", i);
                fragment.setArguments(bundle);
                return fragment;
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

        Helper.updateVisibility(findViewById(R.id.loading), true);
        loadAttachments(0, new AttachmentLoadedListener() {
            @Override
            public void onResponse(JSONObject data, int dataIndex) {
                Helper.updateVisibility(findViewById(R.id.loading), false);
                parseAttachments(data, dataIndex);
                if (mHasAttachmentsNext)
                    loadAttachments(-1, null);
                if (mHasAttachmentsNext)
                    loadAttachments(1, null);
            }
        });
    }

    public static class AttachmentFragment extends Fragment {
        private AttachmentViewer mActivity;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mActivity = (AttachmentViewer) activity;
        }

        private Bitmap getCachedBitmap(String cacheKey) {
            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeStream(Discuz.getCache(cacheKey));
                if (bitmap == null) return null;
            }
            catch (Throwable e) {
                e.printStackTrace();
                return null;
            }

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
                    return null;
                }
            }

            return bitmap;
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
                bundle.putString("title", mActivity.mThread.title);
                bundle.putInt("tid", mActivity.mThread.id);
                bundle.putInt("pid", mActivity.mPost.id);
                bundle.putInt("index", position + (mActivity.mHasAttachmentsPrev ? -1 : 0));
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
                final View view = inflater.inflate(R.layout.fragment_attachment_image, container, false);
                final PhotoView photoView = (PhotoView) view.findViewById(R.id.image_view);
                final TextView message = (TextView) view.findViewById(R.id.message);
                final View loading = view.findViewById(R.id.loading);

                String key = getString(R.string.pref_key_manga_cache_size);
                int size = Helper.toSafeInteger(ThisApp.preferences.getString(key, ""), 256);
                final String cacheKey;
                if (size > 0) {
                    cacheKey = Helper.toSafeMD5(src);
                }
                else {
                    final String mime = URLConnection.guessContentTypeFromName(src);
                    // Note: your file will probably not be seen from PC until next reboot
                    // REF: https://code.google.com/p/android/issues/detail?id=38282
                    final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/Nyasama/Images/" +
                            bundle.getInt("tid", 0) + "-" + bundle.getString("title", ""),
                            bundle.getInt("pid", 0) + "-" + bundle.getInt("index", 0) + "-" +
                                    Helper.toSafeMD5(src).substring(0, 5) + "." + mime.substring(mime.indexOf('/') + 1));
                    cacheKey = "file:" + file.getAbsolutePath();
                }

                final Bitmap bitmap = getCachedBitmap(cacheKey);
                if (bitmap != null) {
                    photoView.setImageBitmap(bitmap);
                }
                else {
                    loading.setVisibility(View.VISIBLE);
                    Discuz.download(Discuz.getSafeUrl(src), cacheKey, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String err) {
                            loading.setVisibility(View.GONE);
                            if (err != null) {
                                message.setVisibility(View.VISIBLE);
                                message.setText(err);
                            }
                            else {
                                message.setVisibility(View.GONE);
                                Bitmap bitmap = getCachedBitmap(cacheKey);
                                photoView.setImageBitmap(bitmap);
                            }
                        }
                    }, new Discuz.DownloadProgressListener() {
                        @Override
                        public boolean onResponse(int progress) {
                            message.setVisibility(View.VISIBLE);
                            message.setText(progress > 0 ? progress + "%" : "loading");
                            return mActivity.isFinishing();
                        }
                    });
                }
                return view;
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
