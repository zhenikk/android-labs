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

package com.google.android.noisealert;

import java.util.Formatter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

public class NoiseAlert extends Activity {
	/* constants */
	private static final String LOG_TAG = "NoiseAlert";
	private static final int POLL_INTERVAL = 300;
	private static final int NO_NUM_DIALOG_ID=1;

	/** running state **/
	private boolean mRunning = false;
	private boolean mTestMode = false;
	private int mTickCount = 0;
	private int mHitCount =0;

	/** config state **/
	private int mThreshold;
	private int mPollDelay;
	private String mPhoneNumber;
	private boolean mGraphicsEnable;

	private Handler mHandler = new Handler();

	/* References to view elements */
	private TextView mStatusView;
	private TextView mSignalView;
	private SoundLevelView mDisplay;

	/* data source */
	private SoundMeter mSensor;

	private Runnable mSleepTask = new Runnable() {
		public void run() {
			start();
		}
	};
	private Runnable mPollTask = new Runnable() {
		public void run() {
			double amp = mSensor.getAmplitude();
			if (mTestMode) updateDisplay("testing...", amp);
			else           updateDisplay("monitoring...", amp);

			if ((amp > mThreshold) && !mTestMode) {
				mHitCount++;
				if (mHitCount > 5){
					callForHelp();
					return;
				}
			}

			mTickCount++;
			if ((mTestMode || mPollDelay > 0) && mTickCount > 100) {
				if (mTestMode) {
					stop();
				} else {
					sleep();
				}
			} else {
				mHandler.postDelayed(mPollTask, POLL_INTERVAL);
			}
		}
	};
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		setContentView(R.layout.main);
		mStatusView = (TextView) findViewById(R.id.status);
		mSignalView = (TextView) findViewById(R.id.signal);

		mSensor = new SoundMeter();
		mDisplay = (SoundLevelView) findViewById(R.id.volume);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		readApplicationPreferences();
		mDisplay.setLevel(0, mThreshold);
	}

	@Override
	public void onStop() {
		super.onStop();
		stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.test).setEnabled(!mRunning);
		if (mRunning) {
			menu.findItem(R.id.start_stop).setTitle(R.string.stop);
		} else {
			menu.findItem(R.id.start_stop).setTitle(R.string.start);
		}
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.settings:
			Log.i(LOG_TAG, "settings");
			Intent prefs = new Intent(this, Preferences.class);
			startActivity(prefs);
			break;
		case R.id.start_stop:
			if (!mRunning) {

				if (mPhoneNumber.length() == 0) {
					showDialog(NO_NUM_DIALOG_ID);
					break;
				}
				mRunning = true;
				mTestMode = false;
				start();
			} else {
				mRunning = false;
				stop();
			}
			break;
		case R.id.test:
			mTestMode = true;
			start();
			break;
		case R.id.panic:
			callForHelp();
			break;
		}
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == NO_NUM_DIALOG_ID) {
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.icon)
			.setTitle(R.string.no_num_title)
			.setMessage(R.string.no_num_msg)
			.setNeutralButton(R.string.ok, null)
			.create();
		}
		else return null;
	}

	private void start() {
		mTickCount = 0;
		mHitCount = 0;
		mSensor.start();
		mHandler.postDelayed(mPollTask, POLL_INTERVAL);
	}

	private void stop() {
		mHandler.removeCallbacks(mSleepTask);
		mHandler.removeCallbacks(mPollTask);
		mSensor.stop();
		mDisplay.setLevel(0,0);
		updateDisplay("stopped...", 0.0);
		mRunning = false;
		mTestMode = false;
	}

	private void sleep() {
		mSensor.stop();
		updateDisplay("paused...", 0.0);
		mHandler.postDelayed(mSleepTask, 1000*mPollDelay);
	}

	private void readApplicationPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mPhoneNumber = prefs.getString("alert_phone_number", null);
		Log.i(LOG_TAG, "phone number = "+mPhoneNumber);
		mThreshold = Integer.parseInt(prefs.getString("threshold", null));
		Log.i(LOG_TAG, "threshold=" + mThreshold);
		mPollDelay = Integer.parseInt(prefs.getString("sleep", null));
		Log.i(LOG_TAG, "sleep=" + mPollDelay);
		mGraphicsEnable = prefs.getBoolean("display_update", true);
		Log.i(LOG_TAG, "graphics enable=" + mGraphicsEnable);
	}

	private void updateDisplay(String status, double signalEMA) {
		mStatusView.setText(status);

		mSignalView.setText((new Formatter()).format("%03.1f",signalEMA).toString());
		if (mGraphicsEnable) mDisplay.setLevel((int)signalEMA, mThreshold);
	}

	private void callForHelp() {
		if (mPhoneNumber.length() == 0) {
			stop();
			showDialog(NO_NUM_DIALOG_ID);
			return;
		}
		stop();
		final Uri number = Uri.fromParts("tel", mPhoneNumber, "");
		startActivity(new Intent(Intent.ACTION_CALL, number));	
	}
};