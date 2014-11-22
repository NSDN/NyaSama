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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.nyasama.R;
import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.util.Discuz;
import com.nyasama.util.Helper;

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

    final String TAG = "NewPost";
    final int REQCODE_PICK_IMAGE = 1;
    final int REQCODE_PICK_CAPTURE = 2;

    final int MAX_IMAGE_WIDTH = 500;
    final int MAX_IMAGE_HEIGHT = 500;
    final int THUMBNAIL_WIDTH = 100;
    final int THUMBNAIL_HEIGHT = 100;

    public void doPost(View view) {
        final String title = mInputTitle.getText().toString();
        final String content = mInputContent.getText().toString();
        final String noticetrimstr = getIntent().getStringExtra("notice_trimstr");
        if (title.isEmpty() || content.isEmpty()) {
            Helper.toast(R.string.post_content_empty_message);
            return;
        }

        Intent intent = getIntent();
        final String fid = intent.getStringExtra("fid");
        final String tid = intent.getStringExtra("tid");
        if (fid == null && tid == null)
            throw new RuntimeException("fid or tid is required!");

        Discuz.execute(fid != null ? "newthread" : "sendreply", new HashMap<String, Object>() {{
            if (fid != null) {
                put("fid", fid);
                put("topicsubmit", "yes");
            }
            else {
                put("tid", tid);
                put("replysubmit", "yes");
            }
        }}, new HashMap<String, Object>() {{
            put("message", content);
            if (fid != null)
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
                                .setTitle("There is sth wrong...")
                                .setMessage(message.getString("messagestr"))
                                .setPositiveButton(android.R.string.ok, null);
                            if (message.getString("messageval").equals("postperm_login_nopermission//1"))
                                builder.setNegativeButton("Login", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        startActivity(new Intent(NewPostActivity.this, LoginActivity.class));
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

    void insertImageToContent(ImageAttachment image) {
        int start = mInputContent.getSelectionStart();
        mInputContent.getText().insert(start, "[attachimg]"+image.uploadId+"[/attachimg]");
    }

    // REF: http://stackoverflow.com/questions/20067508/get-real-path-from-uri-android-kitkat-new-storage-access-framework
    // by bluebrain
    String getPathFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
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

    private EditText mInputTitle;
    private EditText mInputContent;
    private MenuItem mButtonPost;
    private MenuItem mButtonAddImg;

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
            if (bitmap.getWidth() > MAX_IMAGE_WIDTH || bitmap.getHeight() > MAX_IMAGE_HEIGHT) {
                int newWidth = MAX_IMAGE_WIDTH;
                int newHeight = MAX_IMAGE_HEIGHT;
                if (bitmap.getWidth() * MAX_IMAGE_HEIGHT > bitmap.getHeight() * MAX_IMAGE_WIDTH)
                    newHeight = bitmap.getHeight() * MAX_IMAGE_WIDTH / bitmap.getWidth();
                else
                    newWidth = bitmap.getWidth() * MAX_IMAGE_HEIGHT / bitmap.getHeight();
                File dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                try {
                    File file = File.createTempFile("nyasama_resized_", ".jpg", dir);
                    FileOutputStream stream = new FileOutputStream(file);
                    bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
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

            //
            final Bitmap thumbnail = ThumbnailUtils.extractThumbnail(
                bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
            final String uploadFile = filePath;
            final String fileName = requestCode == REQCODE_PICK_IMAGE ?
                    "image #"+mImageAttachments.size() : "photo #"+mImageAttachments.size();
            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Uploading")
                    .setCancelable(false)
                    .show();
            Discuz.upload(new HashMap<String, Object>() {{
                put("type", "image");
                put("fid", getIntent().getStringExtra("fid"));
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
            });
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
            ListView selection = mImageAttachments.size() > 0 ? new ListView(this) : null;
            final AlertDialog dialog = new AlertDialog.Builder(NewPostActivity.this)
                    .setTitle("Insert An Image")
                    .setView(selection)
                    .setPositiveButton("From Gallery", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent, REQCODE_PICK_IMAGE);
                        }
                    })
                    .setNegativeButton("From Camera", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            File dir = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES);
                            try {
                                File file = File.createTempFile("nyasama_", ".jpg", dir);
                                mPhotoFilePath = file.getAbsolutePath();
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                            }
                            catch (IOException e) {
                                Log.e(TAG, "create photo cache failed");
                            }
                            startActivityForResult(intent, REQCODE_PICK_CAPTURE);
                        }
                    })
                    .show();
            if (selection != null) {
                selection.setAdapter(new CommonListAdapter<ImageAttachment>(mImageAttachments,
                        R.layout.fragment_select_attachment_item) {
                    @Override
                    public void convert(ViewHolder viewHolder, ImageAttachment item) {
                        ((ImageView) viewHolder.getView(R.id.image_view)).setImageBitmap(item.bitmap);
                        ((TextView) viewHolder.getView(R.id.image_name)).setText(item.name);
                    }
                });
                selection.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        insertImageToContent(mImageAttachments.get(i));
                        dialog.cancel();
                    }
                });
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    List<ImageAttachment> mImageAttachments = new ArrayList<ImageAttachment>();
    static class ImageAttachment {
        public Bitmap bitmap;
        public String name;
        public String uploadId;
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
