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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

import uk.co.senab.photoview.PhotoViewAttacher;

public class AttachmentViewer extends FragmentActivity {

    private static String TAG = "ImageViewer";

    private FragmentPagerAdapter mPageAdapter;
    private List<Attachment> mAttachmentList = new ArrayList<Attachment>();
    private Map<String, Bitmap> mBitmapCache = new HashMap<String, Bitmap>();

    public void loadAttachments() {
        Discuz.execute("viewthread", new HashMap<String, Object>() {{
            put("ppp", 1);
            put("tid", getIntent().getIntExtra("tid", 0));
            put("page", getIntent().getIntExtra("index", 0) + 1);
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
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
                        if (postlist.length() == 1) {
                            Post post = new Post(postlist.getJSONObject(0));
                            mAttachmentList = post.attachments;
                        }
                        mPageAdapter.notifyDataSetChanged();
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "JsonError: Load Post List Failed (" + e.getMessage() + ")");
                        Helper.toast(R.string.load_failed_toast);
                    }
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attachment_viewer);

        ViewPager pager = (ViewPager) findViewById(R.id.view_pager);
        pager.setAdapter(mPageAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
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
                            }
                            else {
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
                        }
                        else {
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

        loadAttachments();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_attach_image_viewer, menu);
        return true;
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
}
