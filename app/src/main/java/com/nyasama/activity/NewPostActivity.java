package com.nyasama.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.nyasama.R;
import com.nyasama.ThisApp;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;
import com.nyasama.util.Discuz.SmileyGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NewPostActivity extends Activity
    implements TextWatcher {

    static class Size {
        public int width;
        public int height;

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    static Size getImageSize(Size source, Size target, boolean coverTarget) {
        boolean tooWide = source.width * target.height > source.height * target.width;
        if ((tooWide && !coverTarget) || (!tooWide && coverTarget))
            target.height = source.height * target.width / source.width;
        else
            target.width = source.width * target.height / source.height;
        return target;
    }

    static final Size uploadSize = new Size(800, 800);
    static final Size thumbSize = new Size(100, 100);

    static final String TAG = "NewPost";
    static final int REQCODE_PICK_IMAGE = 1;
    static final int REQCODE_PICK_CAPTURE = 2;

    static class ImageAttachment {
        public Bitmap bitmap;
        public String name;
        public String uploadId;
    }
    List<ImageAttachment> mImageAttachments = new ArrayList<ImageAttachment>();

    public void doPost(View view) {
        final String title = mInputTitle.getText().toString();
        final String content = mInputContent.getText().toString();
        final String noticetrimstr = getIntent().getStringExtra("notice_trimstr");
        if (title.isEmpty() || content.isEmpty()) {
            Helper.toast(R.string.post_content_empty_message);
            return;
        }

        Intent intent = getIntent();
        final int fid = intent.getIntExtra("fid", 0);
        final int tid = intent.getIntExtra("tid", 0);
        if (fid == 0 && tid == 0)
            throw new RuntimeException("fid or tid is required!");

        Discuz.execute(fid > 0 ? "newthread" : "sendreply", new HashMap<String, Object>() {{
            if (fid > 0) {
                put("fid", fid);
                put("topicsubmit", "yes");
            }
            else {
                put("tid", tid);
                put("replysubmit", "yes");
            }
        }}, new HashMap<String, Object>() {{
            put("message", content);
            if (fid > 0)
                put("subject", title);
            else if (noticetrimstr != null)
                put("noticetrimstr", noticetrimstr);
            // strange, but really works
            for (ImageAttachment image : mImageAttachments)
                put("attachnew["+image.uploadId+"][description]", "");
        }}, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject data) {
                if (data.has(Discuz.VOLLEY_ERROR)) {
                    Helper.toast(R.string.network_error_toast);
                }
                else {
                    try {
                        JSONObject message = data.getJSONObject("Message");
                        String messageval = message.getString("messageval");
                        if ("post_reply_succeed".equals(messageval) ||
                                "post_newthread_succeed".equals(messageval)) {
                                // return tid to parent activity
                                String tid = data.getJSONObject("Variables").getString("tid");
                                setResult(Integer.parseInt(tid));
                            finish();
                        }
                        else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NewPostActivity.this)
                                .setTitle(R.string.there_is_something_wrong)
                                .setMessage(message.getString("messagestr"))
                                .setPositiveButton(android.R.string.ok, null);
                            if ("postperm_login_nopermission//1".equals(messageval) ||
                                    "replyperm_login_nopermission//1".equals(messageval))
                                builder.setNegativeButton("Login", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        startActivityForResult(new Intent(NewPostActivity.this, LoginActivity.class),
                                                Discuz.REQUEST_CODE_LOGIN);
                                    }
                                });
                            builder.show();
                        }
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "Parse Result Failed:"+e.getMessage());
                    }
                    catch (NullPointerException e) {
                        Log.e(TAG, "Parse Result Failed:"+e.getMessage());
                    }
                    catch (NumberFormatException e) {
                        Log.e(TAG, "Parse Result Failed:"+e.getMessage());
                    }
                }
                mButtonPost.setEnabled(true);
            }
        });
        mButtonPost.setEnabled(false);
    }

    void insertCodeToContent(String code) {
        int start = mInputContent.getSelectionStart();
        mInputContent.getText().insert(start, code);
    }

    void insertImageToContent(ImageAttachment image) {
        insertCodeToContent("[attachimg]"+image.uploadId+"[/attachimg]");
    }

    // REF: http://stackoverflow.com/questions/20067508/get-real-path-from-uri-android-kitkat-new-storage-access-framework
    // by bluebrain
    String getPathFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return uri.getPath();
        }
        else {
            cursor.moveToFirst();
            String document_id = cursor.getString(0);
            document_id = document_id.substring(document_id.lastIndexOf(":")+1);
            cursor.close();

            cursor = getContentResolver().query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
            cursor.moveToFirst();
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            cursor.close();
            return path;
        }
    }

    private EditText mInputTitle;
    private EditText mInputContent;
    private MenuItem mButtonPost;
    private MenuItem mButtonAddImg;

    public void showInsertOptions() {
        View view = getLayoutInflater().inflate(R.layout.fragment_insert_options, null);
        final AlertDialog dialog = new AlertDialog.Builder(NewPostActivity.this)
                .setTitle(R.string.diag_insert_options)
                .setView(view)
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
                        try {
                            File file = File.createTempFile("nyasama_", ".jpg", dir);
                            mPhotoFilePath = file.getAbsolutePath();
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                        } catch (IOException e) {
                            Log.e(TAG, "create photo cache failed");
                        }
                        startActivityForResult(intent, REQCODE_PICK_CAPTURE);
                    }
                })
                .show();

        AbsListView smileyList = (AbsListView) view.findViewById(R.id.smiley_list);
        smileyList.setAdapter(new CommonListAdapter<SmileyGroup>(Discuz.sSmilies, android.R.layout.simple_list_item_1) {
            @Override
            public void convertView(ViewHolder viewHolder, SmileyGroup item) {
                ((TextView) viewHolder.getConvertView()).setText(item.name);
            }
        });
        smileyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showSmileyOptions(Discuz.sSmilies.get(i));
                dialog.cancel();
            }
        });

        AbsListView attachList = (AbsListView) view.findViewById(R.id.attachment_list);
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
        final AlertDialog dialog = new AlertDialog.Builder(NewPostActivity.this)
                .setTitle(R.string.diag_insert_smiley_title)
                .setView(view)
                .setPositiveButton("Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        showInsertOptions();
                        dialogInterface.cancel();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        view.setAdapter(new CommonListAdapter<Discuz.Smiley>(smileyGroup.list, R.layout.fragment_smiley_item) {
            @Override
            public void convertView(ViewHolder viewHolder, Discuz.Smiley item) {
                String url = Discuz.DISCUZ_URL + "static/image/smiley/" +
                        smileyGroup.path + "/" + item.image;
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
        final AlertDialog dialog = new AlertDialog.Builder(this)
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

        mInputTitle = (EditText) findViewById(R.id.input_title);
        mInputContent = (EditText) findViewById(R.id.input_content);
        mInputTitle.addTextChangedListener(this);
        mInputContent.addTextChangedListener(this);

        Intent intent = getIntent();
        if (intent.hasExtra("thread_title")) {
            mInputTitle.setText(intent.getStringExtra("thread_title"));
            mInputTitle.setEnabled(false);
        }

        mInputContent.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (mButtonAddImg != null)
                    mButtonAddImg.setVisible(b);
            }
        });

    }

    String mPhotoFilePath;

    // REF: http://stackoverflow.com/questions/2507898/how-to-pick-an-image-from-gallery-sd-card-for-my-app
    // REF: http://developer.android.com/training/camera/photobasics.html#TaskPhotoView
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQCODE_PICK_IMAGE || requestCode == REQCODE_PICK_CAPTURE)
                && resultCode == RESULT_OK) {
            //
            String filePath = requestCode == REQCODE_PICK_IMAGE ?
                    getPathFromUri(data.getData()) : mPhotoFilePath;

            // resize the image if too large
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            Size bitmapSize = new Size(bitmap.getWidth(), bitmap.getHeight());
            if (bitmap.getWidth() > uploadSize.width || bitmap.getHeight() > uploadSize.height) {
                bitmapSize = getImageSize(bitmapSize, uploadSize, false);
                File dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                try {
                    File file = File.createTempFile("nyasama_resized_", ".jpg", dir);
                    FileOutputStream stream = new FileOutputStream(file);
                    bitmap = Bitmap.createScaledBitmap(bitmap, bitmapSize.width, bitmapSize.height, false);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
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
            Size newSize = getImageSize(bitmapSize, thumbSize, true);
            final Bitmap thumbnail = ThumbnailUtils.extractThumbnail(
                    bitmap, newSize.width, newSize.height);
            final String uploadFile = filePath;
            final String fileName =
                    (requestCode == REQCODE_PICK_IMAGE ? "image" : "photo") +
                    " (" + bitmapSize.width + "x" + bitmapSize.height + ")";
            View loadingView = LayoutInflater.from(this)
                    .inflate(R.layout.fragment_upload_process, null, false);
            final AlertDialog dialog = new AlertDialog.Builder(this)
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
        else if (requestCode == Discuz.REQUEST_CODE_LOGIN && resultCode > 0) {
            refreshFormHash();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_post, menu);
        mButtonPost = menu.findItem(R.id.action_send);
        mButtonPost.setEnabled(false);
        mButtonAddImg = menu.findItem(R.id.action_add_image);
        mButtonAddImg.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        else if (id == R.id.action_send) {
            doPost(null);
            return true;
        }
        else if (id == R.id.action_add_image && mInputContent.hasFocus()) {
            if (Discuz.sSmilies == null) {
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.diag_loading_smilies).setCancelable(false)
                        .show();
                Discuz.getSmileies(new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        dialog.cancel();
                        showInsertOptions();
                    }
                });
            }
            else {
                showInsertOptions();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (mButtonPost != null)
            mButtonPost.setEnabled(!(mInputTitle.getText().toString().isEmpty() ||
                    mInputContent.getText().toString().isEmpty()));
    }
}
