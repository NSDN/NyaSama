package com.nyasama.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.negusoft.holoaccent.dialog.DividerPainter;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.util.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;
import com.nyasama.util.Discuz.SmileyGroup;
import com.nyasama.util.Helper.Size;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewPostActivity extends BaseThemedActivity {

    static final Size MAX_UPLOAD_SIZE = new Size(2048, 2048);
    static final Size THUMBNAIL_SIZE = new Size(100, 100);

    static final String ARG_POST_TITLE = "thread_title";
    static final String ARG_POST_TRIMSTR = "notice_trimstr";

    static final String TAG = "NewPost";
    static final int REQCODE_PICK_IMAGE = 1;
    static final int REQCODE_PICK_CAPTURE = 2;

    static class ImageAttachment {
        public Bitmap bitmap;
        public String name;
        public String uploadId;
    }

    private EditText mInputTitle;
    private EditText mInputContent;
    private Spinner mSpinnerTypes;

    String mPollOptions = "";
    int mPollChoices = 1;
    int mPollExpiration = 0;

    String mPhotoFilePath;
    Discuz.ThreadTypes mThreadTypes;
    List<ImageAttachment> mImageAttachments = new ArrayList<ImageAttachment>();

    /*
    // TODO: replace html tags with discuz tags
    HtmlImageGetter.HtmlImageCache mImageCache = new HtmlImageGetter.HtmlImageCache(new BitmapLruCache());
    Point mMaxImageSize = new Point(100, 100);
    */

    public String compileToString(EditText html) {
        /*
        // TODO: replace html tags with discuz tags
        return Html.toHtml(html.getText());
        */
        return html.getText().toString();
    }

    public void loadToEditor(EditText html, String string) {
        /*
        // TODO: replace discuz tags with html tags
        Spanned span = Html.fromHtml(string,
                new HtmlImageGetter(html, mImageCache, mMaxImageSize),
                null);
        html.setText(span);
        */
        html.setText(string);
    }

    public void insertCodeToContent(String code) {
        int start = mInputContent.getSelectionStart();
        mInputContent.getText().insert(start, code);
    }

    public void insertImageToContent(ImageAttachment image) {
        insertCodeToContent("[attachimg]" + image.uploadId + "[/attachimg]");
    }

    public void doEdit(View view) {
        if (findViewById(R.id.loading).getVisibility() == View.VISIBLE)
            return;

        final String title = mInputTitle.getText().toString();
        final String content = compileToString(mInputContent);
        if (content.isEmpty()) {
            Helper.toast(R.string.post_content_empty_message);
            return;
        }

        Intent intent = getIntent();
        final int pid = intent.getIntExtra("pid", 0);
        final int tid = intent.getIntExtra("tid", 0);
        if (pid == 0 && tid == 0)
            throw new RuntimeException("pid or tid is required!");

        Helper.updateVisibility(findViewById(R.id.loading), true);
        Discuz.execute("editpost", new HashMap<String, Object>() {{
            put("pid", pid);
            put("tid", tid);
        }}, new HashMap<String, Object>() {{
            put("editsubmit", "true");
            put("pid", pid);
            put("tid", tid);
            put("message", content);
            put("subject", title);

            if (mSpinnerTypes.getVisibility() == View.VISIBLE && mThreadTypes != null)
                put("typeid", mThreadTypes.get(mSpinnerTypes.getSelectedItem().toString()));

            for (ImageAttachment image : mImageAttachments)
                put("attachnew[" + image.uploadId + "][description]", "");
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                Helper.updateVisibility(findViewById(R.id.loading), false);
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        String messageval = message.getString("messageval");
                        if ("post_edit_succeed".equals(messageval)) {
                            setResult(1);
                            finish();
                        } else {
                            Helper.toast(message.getString("messagestr"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Parse Result Failed:" + e.getMessage());
                    }
                }
            }
        });
    }

    public void doPost(View view) {
        if (findViewById(R.id.loading).getVisibility() == View.VISIBLE)
            return;

        final String title = mInputTitle.getText().toString();
        final String content = compileToString(mInputContent);
        final String noticetrimstr = getIntent().getStringExtra(ARG_POST_TRIMSTR);
        if (title.isEmpty() || content.isEmpty()) {
            Helper.toast(R.string.post_content_empty_message);
            return;
        }

        Intent intent = getIntent();
        final int fid = intent.getIntExtra("fid", 0);
        final int tid = intent.getIntExtra("tid", 0);
        if (fid == 0 && tid == 0)
            throw new RuntimeException("fid or tid is required!");

        Helper.updateVisibility(findViewById(R.id.loading), true);
        Discuz.execute(fid > 0 ? "newthread" : "sendreply", new HashMap<String, Object>() {{
            if (fid > 0) {
                put("fid", fid);
                put("topicsubmit", "yes");
            } else {
                put("tid", tid);
                put("replysubmit", "yes");
            }
        }}, new HashMap<String, Object>() {{
            put("message", content);
            if (fid > 0)
                put("subject", title);
            else if (noticetrimstr != null)
                put("noticetrimstr", noticetrimstr);

            if (!mPollOptions.isEmpty()) {
                put("maxchoices", mPollChoices);
                put("expiration", mPollExpiration);
                List<String> options = new ArrayList<String>();
                Collections.addAll(options, mPollOptions.split("\\n"));
                put("polloption[]", options);
                put("tpolloption", "1");
                put("polls", "yes");
                put("special", "1");
            }

            if (mSpinnerTypes.getVisibility() == View.VISIBLE && mThreadTypes != null)
                put("typeid", mThreadTypes.get(mSpinnerTypes.getSelectedItem().toString()));

            for (ImageAttachment image : mImageAttachments)
                put("attachnew[" + image.uploadId + "][description]", "");
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                Helper.updateVisibility(findViewById(R.id.loading), false);
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        String messageval = message.getString("messageval");
                        if ("post_reply_succeed".equals(messageval) ||
                                "post_newthread_succeed".equals(messageval)) {
                            // return tid to parent activity
                            String tid = data.getJSONObject("Variables").getString("tid");
                            setResult(Integer.parseInt(tid));
                            finish();
                        } else {
                            AccentAlertDialog.Builder builder = new AccentAlertDialog.Builder(NewPostActivity.this)
                                    .setTitle(R.string.error_new_post)
                                    .setMessage(message.getString("messagestr"))
                                    .setPositiveButton(android.R.string.ok, null);
                            if ("postperm_login_nopermission//1".equals(messageval) ||
                                    "replyperm_login_nopermission//1".equals(messageval))
                                builder.setNegativeButton(R.string.login_button_text, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        startActivityForResult(new Intent(NewPostActivity.this, LoginActivity.class),
                                                LoginActivity.REQUEST_CODE_LOGIN);
                                    }
                                });
                            builder.show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Parse Result Failed:" + e.getMessage());
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Parse Result Failed:" + e.getMessage());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Parse Result Failed:" + e.getMessage());
                    }
                }
            }
        });
    }

    public void loadMessage() {
        final Intent intent = getIntent();
        mInputTitle.setEnabled(false);
        mInputContent.setEnabled(false);
        Helper.updateVisibility(findViewById(R.id.loading), true);
        Discuz.execute("editpost", new HashMap<String, Object>() {{
            put("pid", intent.getIntExtra("pid", 0));
            put("tid", intent.getIntExtra("tid", 0));
        }}, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                mInputTitle.setEnabled(true);
                mInputContent.setEnabled(true);
                Helper.updateVisibility(findViewById(R.id.loading), false);
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                } else {
                    JSONObject var = data.optJSONObject("Variables");
                    JSONObject postinfo = null;
                    if (var != null)
                        postinfo = var.optJSONObject("postinfo");
                    if (postinfo != null) {
                        mInputTitle.setText(postinfo.optString("subject"));
                        loadToEditor(mInputContent, postinfo.optString("message"));
                        if ("1".equals(postinfo.optString("first"))) {
                            int fid = Helper.toSafeInteger(postinfo.optString("fid"), 0);
                            if (fid > 0) loadThreadTypes(fid,
                                    Helper.toSafeInteger(var.optString("typeid"), 0));
                        }
                    }
                }
            }
        });
    }

    public void loadThreadTypes(final int fid, final int typeid) {
        Discuz.ForumThreadInfo.loadInfo(new Response.Listener<SparseArray<Discuz.ForumThreadInfo>>() {
            @Override
            public void onResponse(SparseArray<Discuz.ForumThreadInfo> forumThreadInfo) {
                if (forumThreadInfo.get(fid) == null) return;
                mThreadTypes = forumThreadInfo.get(fid).types;
                Helper.updateVisibility(mSpinnerTypes, mThreadTypes != null);
                if (mThreadTypes != null) {
                    List<String> list = new ArrayList<String>();
                    list.add(getString(R.string.string_uncategorized));
                    int position = 0;
                    for (Map.Entry<String, Integer> e : mThreadTypes.entrySet()) {
                        list.add(e.getKey());
                        if (e.getValue() == typeid)
                            position = list.size() - 1;
                    }
                    ArrayAdapter adapter = new ArrayAdapter<String>(NewPostActivity.this,
                            android.R.layout.simple_spinner_item, list);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    mSpinnerTypes.setAdapter(adapter);
                    mSpinnerTypes.setSelection(position);
                }
            }
        });
    }

    public void editPollOptions() {
        final View view = getLayoutInflater().inflate(R.layout.fragment_new_poll, null);
        final EditText contentText = (EditText) view.findViewById(R.id.pollcontent);
        contentText.setText(mPollOptions);
        final EditText choicesText = (EditText) view.findViewById(R.id.maxchoices);
        choicesText.setText("" + mPollChoices);
        final EditText expirationText = (EditText) view.findViewById(R.id.expiration);
        expirationText.setText("" + mPollExpiration);
        final AlertDialog dialog = new AccentAlertDialog.Builder(NewPostActivity.this)
                .setTitle(getString(R.string.action_setup_poll))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                new DividerPainter(NewPostActivity.this).paint(dialog.getWindow());
                dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPollOptions = contentText.getText().toString();
                        mPollChoices = Helper.toSafeInteger(choicesText.getText().toString(), 0);
                        mPollExpiration = Helper.toSafeInteger(expirationText.getText().toString(), 0);
                    }
                });
            }
        });
        dialog.show();
    }

    public void showInsertSmileyOptions() {
        final List<SmileyGroup> smilyGroups = SmileyGroup.getSmilies();
        if (smilyGroups == null) {
            Helper.toast(R.string.error_smilely_load);
            return;
        }

        GridView smileyList = new GridView(this);
        smileyList.setNumColumns(3);
        final AlertDialog dialog = new AccentAlertDialog.Builder(NewPostActivity.this)
                .setTitle(R.string.diag_insert_options)
                .setView(smileyList)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        smileyList.setAdapter(new CommonListAdapter<SmileyGroup>(smilyGroups, android.R.layout.simple_list_item_1) {
            @Override
            public void convertView(ViewHolder viewHolder, SmileyGroup item) {
                ((TextView) viewHolder.getConvertView()).setText(item.name);
            }
        });
        smileyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showSmileyOptions(smilyGroups.get(i));
                dialog.cancel();
            }
        });
    }

    public void showInsertImageOptions() {
        ListView attachList = new ListView(this);
        final AlertDialog dialog = new AccentAlertDialog.Builder(NewPostActivity.this)
                .setTitle(R.string.diag_insert_options)
                .setView(mImageAttachments.size() > 0 ? attachList : null)
                .setPositiveButton(R.string.diag_insert_from_gallery, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, REQCODE_PICK_IMAGE);
                    }
                })
                .setNegativeButton(R.string.diag_insert_from_camera, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        File dir = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES);
                        File file = new File(dir, "nyasama_upload_photo.jpg");
                        mPhotoFilePath = file.getAbsolutePath();
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                        startActivityForResult(intent, REQCODE_PICK_CAPTURE);
                    }
                })
                .show();

        attachList.setAdapter(new CommonListAdapter<ImageAttachment>(mImageAttachments,
                R.layout.fragment_select_attachment_item) {
            @Override
            public void convertView(ViewHolder viewHolder, ImageAttachment item) {
                ((ImageView) viewHolder.getView(R.id.image_view)).setImageBitmap(item.bitmap);
                ((TextView) viewHolder.getView(R.id.image_name)).setText(item.name);
            }
        });
        attachList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                insertImageToContent(mImageAttachments.get(i));
                dialog.cancel();
            }
        });
    }

    public void showSmileyOptions(final SmileyGroup smileyGroup) {
        GridView view = new GridView(this);
        view.setNumColumns(4);
        view.setLayoutParams(new AbsListView.LayoutParams(
                AbsListView.LayoutParams.WRAP_CONTENT,
                AbsListView.LayoutParams.WRAP_CONTENT));
        final AlertDialog dialog = new AccentAlertDialog.Builder(NewPostActivity.this)
                .setTitle(R.string.diag_insert_smiley_title)
                .setView(view)
                .setPositiveButton(getString(R.string.button_back), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        showInsertSmileyOptions();
                        dialogInterface.cancel();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        view.setAdapter(new CommonListAdapter<Discuz.Smiley>(smileyGroup.list, R.layout.fragment_smiley_item) {
            @Override
            public void convertView(ViewHolder viewHolder, Discuz.Smiley item) {
                String url = Discuz.getSafeUrl("static/image/smiley/" +
                        smileyGroup.path + "/" + item.image);
                NetworkImageView imageView = (NetworkImageView) viewHolder.getConvertView();
                imageView.setImageUrl(url, ThisApp.imageLoader);
            }
        });
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                insertCodeToContent(smileyGroup.list.get(i).code);
                dialog.cancel();
            }
        });
    }

    public void refreshFormHash() {
        final AlertDialog dialog = new AccentAlertDialog.Builder(NewPostActivity.this)
                .setTitle(R.string.dialog_update_user).setCancelable(false)
                .show();
        // refresh the form hash, or posting will fail
        Discuz.execute("forumindex", new HashMap<String, Object>(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        dialog.cancel();
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        // REF: http://stackoverflow.com/questions/7300497/adjust-layout-when-soft-keyboard-is-on
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mInputTitle = (EditText) findViewById(R.id.input_title);
        mInputContent = (EditText) findViewById(R.id.input_content);
        mSpinnerTypes = (Spinner) findViewById(R.id.thread_type);

        Intent intent = getIntent();
        if (intent.hasExtra(ARG_POST_TITLE)) {
            mInputTitle.setText(intent.getStringExtra(ARG_POST_TITLE));
            mInputTitle.setEnabled(false);
        }

        // editing old post
        if (intent.getIntExtra("pid", 0) > 0) {
            setTitle(getString(R.string.title_editing_post));
            loadMessage();
        }
        // replying
        else if (intent.getIntExtra("tid", 0) > 0) {
            setTitle(getString(R.string.title_editing_reply));
        }
        // creating new post
        else if (intent.getIntExtra("fid", 0) > 0) {
            loadThreadTypes(intent.getIntExtra("fid", 0), 0);
        }
    }

    // REF: http://stackoverflow.com/questions/2507898/how-to-pick-an-image-from-gallery-sd-card-for-my-app
    // REF: http://developer.android.com/training/camera/photobasics.html#TaskPhotoView
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQCODE_PICK_IMAGE || requestCode == REQCODE_PICK_CAPTURE)
                && resultCode == RESULT_OK) {

            // get file path
            String filePath;
            try {
                filePath = requestCode == REQCODE_PICK_IMAGE ?
                        Helper.getPathFromUri(data.getData()) : mPhotoFilePath;
            }
            catch (Throwable e) {
                e.printStackTrace();
                Helper.toast(R.string.toast_open_file_failed);
                return;
            }

            // decode to bitmap
            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeFile(filePath);
                if (bitmap == null) {
                    Helper.toast(R.string.toast_open_image_fail);
                    return;
                }
            }
            catch (Throwable e) {
                e.printStackTrace();
                Helper.toast(R.string.error_new_post);
                return;
            }

            // resize the image if too large
            Size bitmapSize = new Size(bitmap.getWidth(), bitmap.getHeight());
            if (bitmap.getWidth() > MAX_UPLOAD_SIZE.width || bitmap.getHeight() > MAX_UPLOAD_SIZE.height) {
                bitmapSize = Helper.getFittedSize(bitmapSize, MAX_UPLOAD_SIZE, false);
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

                // make sure the image size is smaller than MAX_UPLOAD_BYTES
                ByteArrayOutputStream blob = new ByteArrayOutputStream();
                while (true) {
                    blob.reset();
                    bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSize.width, bitmapSize.height, false);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob);
                    if (blob.size() > Discuz.getMaxUploadSize() && bitmapSize.width > 100) {
                        bitmapSize.width = bitmapSize.width * 9 / 10;
                        bitmapSize.height = bitmapSize.height * 9 / 10;
                    }
                    else {
                        Log.d(NewPostActivity.class.toString(),
                                "resize upload image to " + bitmapSize.width + "x" + bitmapSize.height);
                        break;
                    }
                }

                // write to file
                try {
                    File file = new File(dir, "nyasama_upload_resized.jpg");
                    FileOutputStream stream = new FileOutputStream(file);
                    stream.write(blob.toByteArray());
                    stream.flush();
                    stream.close();
                    filePath = file.getAbsolutePath();
                }
                catch (FileNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                }
                catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

            }

            // create thumbnail
            Size newSize = Helper.getFittedSize(bitmapSize, THUMBNAIL_SIZE, true);
            final Bitmap thumbnail = ThumbnailUtils.extractThumbnail(
                    bitmap, newSize.width, newSize.height);

            // upload
            final String uploadFile = filePath;
            final String fileName =
                    (requestCode == REQCODE_PICK_IMAGE ? "image" : "photo") +
                    " (" + bitmapSize.width + "x" + bitmapSize.height + ")";
            View loadingView = LayoutInflater.from(this)
                    .inflate(R.layout.fragment_upload_process, null, false);
            final AlertDialog dialog = new AccentAlertDialog.Builder(NewPostActivity.this)
                    .setTitle(R.string.dialog_uploading).setCancelable(false)
                    .setView(loadingView)
                    .show();
            final ContentLoadingProgressBar progressBar =
                    (ContentLoadingProgressBar) loadingView.findViewById(R.id.processBar);
            final TextView progressText =
                    (TextView) loadingView.findViewById(R.id.processText);
            Discuz.upload(new HashMap<String, Object>() {{
                put("type", "image");
                put("fid", getIntent().getIntExtra("fid", 0));
            }}, uploadFile, new Response.Listener<String>() {
                @Override
                public void onResponse(final String s) {
                    dialog.cancel();
                    if (s != null) {
                        ImageAttachment image = new ImageAttachment() {{
                            bitmap = thumbnail;
                            name = fileName;
                            uploadId = s;
                        }};
                        mImageAttachments.add(image);
                        insertImageToContent(image);
                    }
                    else {
                        Helper.toast(getString(R.string.image_upload_failed_toast));
                    }
                }
            }, new Response.Listener<Integer>() {
                @Override
                public void onResponse(final Integer integer) {
                    progressBar.setProgress(integer);
                    progressText.setText(integer + "%");
                }
            });
        }
        else if (requestCode == LoginActivity.REQUEST_CODE_LOGIN && resultCode > 0) {
            refreshFormHash();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_post, menu);
        boolean isEditingPost = getIntent().getIntExtra("pid", 0) > 0;
        menu.findItem(R.id.action_save).setVisible(isEditingPost);
        menu.findItem(R.id.action_send).setVisible(!isEditingPost);
        boolean isNewPost = !isEditingPost && getIntent().getIntExtra("tid", 0) == 0;
        menu.findItem(R.id.action_setup_poll).setVisible(isNewPost);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_send) {
            doPost(null);
        }
        else if (id == R.id.action_save) {
            doEdit(null);
        }
        else if (id == R.id.action_setup_poll) {
            editPollOptions();
        }
        else if (id == R.id.action_add_smiley) {
            if (SmileyGroup.getSmilies() == null) {
                final AlertDialog dialog = new AccentAlertDialog.Builder(NewPostActivity.this)
                        .setTitle(R.string.diag_loading_smilies).setCancelable(false)
                        .show();
                SmileyGroup.loadSmilies(new Response.Listener<List<SmileyGroup>>() {
                    @Override
                    public void onResponse(List<SmileyGroup> smileyGroups) {
                        dialog.cancel();
                        mInputContent.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showInsertSmileyOptions();
                            }
                        }, 200);
                    }
                });
            }
            else {
                showInsertSmileyOptions();
            }
            return true;
        }
        else if (id == R.id.action_add_image) {
            showInsertImageOptions();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
