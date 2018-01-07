package com.binance.api.client.mercury;


/**
 * Copyright 2015-2017 Knowm Inc. (http://knowm.org) and contributors.
 * Copyright 2011-2015 Xeiam LLC (http://xeiam.com) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.demo.charts.ExampleChart;

import com.binance.api.client.BinanceApiCallback;

/**
 * Real-time XY Chart with Error Bars
 * <p>
 * Demonstrates the following:
 * <ul>
 * <li>real-time chart updates with JFrame
 * <li>fixed window
 * <li>error bars
 */
public class MercuryRealTimeChart implements ExampleChart<XYChart> {


  private XYChart xyChart;

  private List<Integer> xData = new CopyOnWriteArrayList<Integer>();
  private List<Double> yData = new CopyOnWriteArrayList<Double>();
  private List<Double> errorBars = new CopyOnWriteArrayList<Double>();

  public static final String SERIES_NAME = "trades";

  public MercuryRealTimeChart() {
	  /*List<Integer> xData,List<Double> yData, List<Double> errorBars,MercuryTradeCallback<Void> callback) {
	  this.xData = xData;
	  this.yData = yData;
	  this.errorBars = errorBars;*/
	  final MercuryRealTimeChart realtimeChart = this; 
	  final XChartPanel<XYChart> chartPanel = this.buildPanel();
	    // Schedule a job for the event-dispatching thread:
	    // creating and showing this application's GUI.
	    javax.swing.SwingUtilities.invokeLater(new Runnable() {

	      @Override
	      public void run() {

	        // Create and set up the window.
	        JFrame frame = new JFrame("XChart");
	        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	        frame.add(chartPanel);

	        // Display the window.
	        frame.pack();
	        frame.setVisible(true);
	      }
	    });

	    // Simulate a data feed
	    TimerTask chartUpdaterTask = new TimerTask() {

	      @Override
	      public void run() {
	    	//	xData,yData,errorBars might have been changed from outside
	        //realtimeChart.updateData(xData,yData,errorBars);
	        chartPanel.revalidate();
	        chartPanel.repaint();
	      }
	    };

	    Timer timer = new Timer();
	    timer.scheduleAtFixedRate(chartUpdaterTask, 0, 50);
  }
  

  public XChartPanel<XYChart> buildPanel() {

    return new XChartPanel<XYChart>(getChart());
  }

  @Override
  public XYChart getChart() {
/*
    yData.add(0.0);
    for (int i = 0; i < 50; i++) {
      double lastPoint = yData.get(yData.size() - 1);
      yData.add(getRandomWalk(lastPoint));
    }
    // generate X-Data
    xData = new CopyOnWriteArrayList<Integer>();
    for (int i = 1; i < yData.size() + 1; i++) {
      xData.add(i);
    }
    // generate error bars
    errorBars = new CopyOnWriteArrayList<Double>();
    for (int i = 0; i < yData.size(); i++) {
      errorBars.add(20 * Math.random());
    }
*/
    // Create Chart
    xyChart = new XYChartBuilder().width(500).height(400).xAxisTitle("X").yAxisTitle("Y").title("Real-time Analysis").build();

    xyChart.addSeries(SERIES_NAME, xData, yData, errorBars);

    return xyChart;
  }

  public void updateData(List<Integer> xData, List<Double> yData, List<Double> errorBars) {
    xyChart.updateXYSeries(SERIES_NAME, xData, yData, errorBars);
    this.xData 	   = xData;
    this.yData 	   = yData;
    this.errorBars = errorBars;
    }

public XYChart getXyChart() {
	return xyChart;
}

public void setXyChart(XYChart xyChart) {
	this.xyChart = xyChart;
}

public List<Integer> getxData() {
	return xData;
}

public void setxData(List<Integer> xData) {
	this.xData = xData;
}

public List<Double> getErrorBars() {
	return errorBars;
}

public void setErrorBars(List<Double> errorBars) {
	this.errorBars = errorBars;
}

public List<Double> getyData() {
	return yData;
}

public static String getSeriesName() {
	return SERIES_NAME;
}

public void setyData(List<Double> yData) {
	this.yData = yData;
}
}
