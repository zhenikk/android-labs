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


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;


public class NetMeterService extends Service {
	final private String TAG="NetMeterService";
	final private int SAMPLING_INTERVAL = 5;
	
	private NotificationManager mNM;
	
	public class MonNetBinder extends Binder {
        NetMeterService getService() {
            return NetMeterService.this;
        }
    }
	private final IBinder mBinder = new MonNetBinder();
	
	private StatsProcessor mStatsProc;
	private CpuMon mCpuMon;
	private GraphView mGraph = null;
	
	private Handler mHandler = new Handler();
	private Runnable mRefresh = new Runnable() {
		public void run() {

			mStatsProc.processUpdate();
			mCpuMon.readStats();
			if (mGraph != null) mGraph.refresh();
			mHandler.postDelayed(mRefresh, SAMPLING_INTERVAL * 1000);
		}
	};
	
	public void resetCounters() {
		mStatsProc.reset();
	}
	
	public void setDisplay(Vector<TextView> stats_views,
			Vector<TextView> info_views,
			Vector<TextView> cpu_views,
			GraphView graph) {
		mGraph = graph;
		mStatsProc.linkDisplay(stats_views, info_views, graph);
		mCpuMon.linkDisplay(cpu_views);
		graph.linkCounters(mStatsProc.getCounters(),
						mCpuMon.getHistory());
	}
	
	@Override
    public void onCreate() {
		Log.i(TAG, "onCreate");
		WifiManager wifi = (WifiManager)getSystemService(WIFI_SERVICE);
		TelephonyManager cellular = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		ConnectivityManager cx = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		
		mStatsProc = new StatsProcessor(SAMPLING_INTERVAL, cellular, wifi, cx);
		mCpuMon = new CpuMon();
		
		mStatsProc.processUpdate();
		mStatsProc.reset();
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		postNotification();
		mHandler.postDelayed(mRefresh, SAMPLING_INTERVAL * 1000);
	}
	
	
	@Override
    public void onDestroy() {
		Log.i(TAG, "onDestroy");
		mNM.cancel(R.string.iconized);
		mHandler.removeCallbacks(mRefresh);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.i(TAG, "onBind");
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent arg) {
		Log.i(TAG, "onUnbind");
		mStatsProc.unlinkDisplay();
		mCpuMon.unlinkDisplay();
		mGraph = null;
		return true;
	}
	
	private void postNotification() {
    	// In this sample, we'll use the same text for the ticker and the expanded notification
    	CharSequence text = getText(R.string.app_name);

    	// Set the icon, scrolling text and timestamp
    	Notification notification = new Notification(R.drawable.status_icon, text,
    			System.currentTimeMillis());
    	notification.flags |= Notification.FLAG_NO_CLEAR|Notification.FLAG_ONGOING_EVENT;

    	// The PendingIntent to launch our activity if the user selects this notification
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
    			new Intent(this, NetMeter.class), 0);

    	// Set the info for the views that show in the notification panel.
    	notification.setLatestEventInfo(this, getText(R.string.iconized),
    			text, contentIntent);

    	// Send the notification.
    	// We use a string id because it is a unique number.  We use it later to cancel.
    	mNM.notify(R.string.iconized, notification);

    }
	
}