/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.netmeter;


import java.util.Vector;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class NetMeter extends Activity {
	final private String TAG="NetMeter";
	
	private NetMeterService mService;
	private Vector<TextView> mStatsFields;
	private Vector<TextView> mInfoFields;
	private Vector<TextView> mCpuFields;
	
	//private PowerMon mPower;
	
	private GraphView mGraph;
	
	private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            
            mService = ((NetMeterService.MonNetBinder)service).getService();
            Log.i(TAG, "service connected");
            mService.setDisplay(mStatsFields, mInfoFields, mCpuFields, mGraph);
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            Log.i(TAG, "service disconnected - should never happen");
        }
    };

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        startService(new Intent(this, NetMeterService.class));
        
        setContentView(R.layout.main);
        mStatsFields = new Vector<TextView>();
        mInfoFields = new Vector<TextView>();
        mCpuFields = new Vector<TextView>();
        
        mGraph = (GraphView)findViewById(R.id.graph);
        
        createTable();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_menu, menu);
    	return super.onCreateOptionsMenu(menu);
    }

    /**
     * Framework method called when activity menu option is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.reset:
    		mService.resetCounters();
    		break;
    	case R.id.toggle:
    		String banner = mGraph.toggleScale();
    		Toast.makeText(this, banner, Toast.LENGTH_SHORT).show();
    		break;
    	case R.id.top:
    		Intent intent = new Intent();
            intent.setClass(this, TaskList.class);
            startActivity(intent);
            break;
    	case R.id.help:
    		Intent myIntent = new Intent();
    		myIntent.setClass(this, HelpActivity.class);
    		startActivity(myIntent);
    		break;
    	case R.id.stop:
    		stopService(new Intent(this, NetMeterService.class));
    		finish();
    		break;
    	}
    	return true;
    }

    @Override
    public void onResume() {
    	super.onResume();
    	bindService(new Intent(this, 
                NetMeterService.class), mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onPause() {
    	super.onPause();
    	unbindService(mConnection);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.i(TAG, "onDestroy");
    	
    }
    
    private void createTable() {
    	TableLayout table = (TableLayout)findViewById(R.id.disp);
    	
    	mInfoFields.addElement(createTableRow(table, R.string.disp_cell, -1, 0));
    	mStatsFields.addElement(createTableRow(table, -1, R.string.disp_in, 0));
    	mStatsFields.addElement(createTableRow(table, -1, R.string.disp_out, 0));
    	createTableRow(table, 0, 0, 0);
    	mInfoFields.addElement(createTableRow(table, R.string.disp_wifi, -1, 0));
    	mStatsFields.addElement(createTableRow(table, -1, R.string.disp_in, 0));
    	mStatsFields.addElement(createTableRow(table, -1, R.string.disp_out, 0));
    	createTableRow(table, 0, 0, 0);
    	mCpuFields.addElement(createTableRow(table, R.string.disp_cpu,
    				R.string.disp_cpu_type, 0));
    	//mCpuFields.addElement(createTableRow(table, -1, R.string.disp_user, 0));
    	//mCpuFields.addElement(createTableRow(table, -1, R.string.disp_system, 0));
    }
    
    private TextView createTableRow(TableLayout table, int c1, int c2, int c3) {
    	int[] cell_text_ids = {c1, c2, c3};
    	TableRow tr = new TableRow(this);
		table.addView(tr);
		for (int i=0; i < 3; ++i) {
			TextView txt = new TextView(this);
			tr.addView(txt);
			if (cell_text_ids[i] == -1) {
				txt.setVisibility(View.INVISIBLE);
			} else if (cell_text_ids[i] == 0) {
				txt.setText("");
				txt.setGravity(Gravity.RIGHT);
				return txt;
			} else {
				txt.setText(getString(cell_text_ids[i]));
			}
		}
		return null;
    }
}