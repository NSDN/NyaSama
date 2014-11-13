package com.nyasama.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.nyasama.adapter.CommonListAdapter;
import com.nyasama.R;

import java.util.ArrayList;
import java.util.List;


public class ThreadListActivity extends Activity {

    private class Thread {
        public String id;
        public String title;
        public Thread(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    private List<Thread> mThreads = new ArrayList<Thread>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_list);
        if (savedInstanceState == null) {
            ListView listView = (ListView) findViewById(R.id.thread_list);
            mThreads.add(new Thread("1", "Item1"));
            mThreads.add(new Thread("2", "Item2"));
            mThreads.add(new Thread("3", "Item3"));
            listView.setAdapter(new CommonListAdapter<Thread>(mThreads, R.layout.fragment_thread_item) {
                @Override
                public void convert(ViewHolder viewHolder, Thread item) {
                    ((TextView)viewHolder.getView(R.id.thread_title)).setText(item.title);
                    ((TextView)viewHolder.getView(R.id.thread_sub)).setText(item.id);
                }
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_thread_list, menu);
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
