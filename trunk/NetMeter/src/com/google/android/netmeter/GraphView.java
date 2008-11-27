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

import com.google.android.netmeter.HistoryBuffer.CircularBuffer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;


class GraphView extends View {
	final public int CELL_IN = Color.RED;
	final public int CELL_OUT = Color.GREEN;
	final public int WIFI_IN = 0xffd65b06;  // orange
	final public int WIFI_OUT = 0xfff0e70f; // yellow
	final public int CPU = Color.LTGRAY;
	
	final private int TICKS = 3;
	final private Paint mBackgroundPaint = makePaint(Color.BLUE);
	final private Paint mAxisPaint = makePaint(Color.BLACK);
	final private Paint mCellIn = makePaint(CELL_IN);
	final private Paint mCellOut = makePaint(CELL_OUT);
	final private Paint mWifiIn = makePaint(WIFI_IN);
	final private Paint mWifiOut = makePaint(WIFI_OUT);
	final private Paint mCpu = makePaint(CPU);
	
	private Vector<StatCounter> mCounters = null;
	private HistoryBuffer mCpuCounter = null;
	
	private int mResolution = 0;
	private int mRefreshTicks = 0;

	
	class Projection {
		final public int mWidth;
		final public int  mHeight;
		final public int mXrange;
		final public int mYrange;
		final private float mXscale;
		final private float mYscale;
		public Projection(int width, int height, int x_range, int y_range) {
			mWidth = width;
			mHeight = height;
			mXrange = x_range;
			mYrange = y_range;
			mXscale = (float)(width - 10) / x_range;
			mYscale = (float)(height - 10) / y_range;
		}
		public float x(int x) {
			return x * mXscale + 5;
		}
		public float y(int y) {
			return mHeight - y * mYscale - 5;
		}
	}
	
	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public String toggleScale() {
		mResolution += 1;
		mResolution %= 6;
		if (mResolution > getMaxTimescale()) {
			mResolution = 0;
		}
		invalidate();
		mRefreshTicks = (mResolution + 1) * 3;
		return getBanner();
	}
	
	public void refresh() {
		if (mRefreshTicks == 0) {
			invalidate();
			mRefreshTicks = (mResolution + 1) * 3;
		} else {
			--mRefreshTicks;
		}
	}
	
	public void linkCounters(Vector<StatCounter> counters,
							HistoryBuffer cpu) {
		mCounters = counters;
		mCpuCounter = cpu;
		mResolution = getMaxTimescale();
		invalidate();
	}
	
	@Override
	public void onDraw(Canvas canvas) {
        canvas.drawPaint(mBackgroundPaint);
        if (mCounters == null || mCpuCounter == null) return;
  
        Projection proj = getDataScale();
        Projection percent = new Projection(proj.mWidth, proj.mHeight,
        									proj.mXrange, 100);
        
        drawAxis(canvas, proj, percent);
        
        drawGraph(canvas, proj, mCellIn, 
        		mCounters.get(0).getHistory().getData(mResolution));
        drawGraph(canvas, proj, mCellOut, 
        		mCounters.get(1).getHistory().getData(mResolution));
        
        drawGraph(canvas, proj, mWifiIn, 
        		mCounters.get(2).getHistory().getData(mResolution));
        drawGraph(canvas, proj, mWifiOut, 
        		mCounters.get(3).getHistory().getData(mResolution));
        
        drawGraph(canvas, percent, mCpu,
        		mCpuCounter.getData(mResolution));
	}
	
	private int getMaxTimescale() {
		int capacity = mCounters.get(0).getHistory()
						.getData(5).getCapacity();
		int size = mCounters.get(0).getHistory()
					.getData(5).getSize();
		
		capacity -= capacity/10;
		if (size > capacity/2) return 5;
		if (size > capacity/4) return 4;
		if (size > capacity/8) return 3;
		if (size > capacity/24) return 2;
		if (size > capacity/48) return 1;
		return 0;
	}
	
	private String getBanner() {
		switch(mResolution) {
		case 0:
			return "30min";
		case 1:
			return "1hour";
		case 2:
			return "3hours";
		case 3:
			return "6hours";
		case 4:
			return "12hours";
		case 5:
			return "24hours";
		default:
			return "invalid";
		}
	}
	
	private Projection getDataScale() {
		int xscale = mCounters.get(0).getHistory()
					.getData(mResolution).getCapacity();
		if (mResolution % 2 == 0) {
			xscale /= 2;
		}
		int yscale = 10;
		
		for (int i=0; i< mCounters.size(); ++i) {
			int val = mCounters.get(i).getHistory()
					.getData(mResolution).getMax(xscale);
			if (val > yscale) {
				yscale = val;
			}
		}
		yscale = yscale + (yscale/10); // + 10%
		yscale = ((yscale / 10) + 1) * 10;
		return new Projection(getWidth(), getHeight() - 10,
				xscale, yscale);
	}
	
	private void drawGraph(Canvas canvas,
			Projection proj,
			Paint color,
			CircularBuffer data) {
		int y_start = data.lookBack(0);
		int y_end;
		for (int i = 1; i < data.getSize(); ++i) {
			y_end = data.lookBack(i);
			canvas.drawLine(proj.x(proj.mXrange - i + 1), proj.y(y_start),
					proj.x(proj.mXrange - i), proj.y(y_end),
					color);
			y_start = y_end;
		}
	}
	
	private void drawAxis(Canvas canvas, Projection proj, Projection percent) {
		
		canvas.drawLine(proj.x(0), proj.y(0),
				proj.x(proj.mXrange), proj.y(0),
				mAxisPaint);

		
		canvas.drawLine(proj.x(0), proj.y(0), proj.x(0),
				proj.y(proj.mYrange),
				mAxisPaint);
		
		canvas.drawLine(percent.x(percent.mXrange), percent.y(0),
						percent.x(percent.mXrange), percent.y(100),
						mAxisPaint);
		
		int x_step = proj.mXrange / TICKS;
		int y_step = proj.mYrange / TICKS;
		for (int i=1; i <= TICKS; ++i) {
			canvas.drawLine(proj.x(x_step * i), proj.y(0),
					proj.x(x_step * i), proj.y(0) - 10, mAxisPaint);
			canvas.drawLine(proj.x(0), proj.y(y_step * i),
					proj.x(0) + 10, proj.y(y_step * i), mAxisPaint);	
		}
		canvas.drawText(Integer.toString(proj.mYrange) + " bps",
				proj.x(0) + 10, proj.y(proj.mYrange) + 10, mAxisPaint);
		
		canvas.drawText("100%",
				percent.x(percent.mXrange) - 30, percent.y(100) + 10,
				mAxisPaint);
		
		canvas.drawText(getBanner(),
				proj.x(proj.mXrange / 2), proj.y(0) + 12,
				mAxisPaint);
		
		canvas.drawText("cell in", proj.x(proj.mXrange / 2) - 20,
				proj.y(proj.mYrange) + 5,
				mCellIn);
		canvas.drawText("cell out", proj.x(proj.mXrange / 2) + 20,
				proj.y(proj.mYrange) + 5,
				mCellOut);
		
		canvas.drawText("wifi in", proj.x(proj.mXrange / 2) - 20,
				proj.y(proj.mYrange) + 20,
				mWifiIn);
		canvas.drawText("wifi out", proj.x(proj.mXrange / 2) + 20,
				proj.y(proj.mYrange) + 20,
				mWifiOut);
		
		canvas.drawText("cpu", proj.x(proj.mXrange / 2) - 20,
				proj.y(proj.mYrange) + 35,
				mCpu);
		
	}
		
	
	private Paint makePaint(int color) {
		Paint p = new Paint();
		p.setColor(color);
		return p;
	}
}
