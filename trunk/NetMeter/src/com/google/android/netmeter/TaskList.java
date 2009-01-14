package com.google.android.netmeter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Toast;



public class TaskList extends ListActivity {
	private static final int DELAY = 30000;
	final private DecimalFormat mPercentFmt = new DecimalFormat("#0.0");
	private Top mTop;
	private ArrayAdapter<String> mAdapter;
	private Handler mHandler = new Handler();
	private Runnable mRefreshTask = new Runnable() {
		public void run() {
			redrawList();
			mHandler.postDelayed(mRefreshTask, DELAY);
		}
	};
	
	   /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAdapter = new ArrayAdapter<String>(this,
                R.layout.tasklist, new ArrayList<String>());
        setListAdapter(mAdapter);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	mTop = new Top();
    	Toast.makeText(this, getText(R.string.disp_collecting), Toast.LENGTH_SHORT).show();
    	mHandler.postDelayed(mRefreshTask, 1000);
    }
	
    @Override
    public void onPause() {
    	super.onPause();
    	mHandler.removeCallbacks(mRefreshTask);
    	mTop = null;
    }
    
    private void redrawList() {
    	Vector<Top.Task> top_list = mTop.getTopN();
    	mAdapter.clear();
		for(Iterator<Top.Task> it = top_list.iterator(); it.hasNext(); ) {
			Top.Task task = it.next();
			if (task.getUsage() == 0) break;
			mAdapter.add(mPercentFmt.format(((double)task.getUsage())/10.0)
					+ "%  " + task.getName());
		}
    }
}
