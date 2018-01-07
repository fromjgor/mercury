/**
 * 
 */
package com.binance.api.client.mercury;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.mercury.*;
import com.binance.api.examples.AggTradesCacheExample;
import com.binance.api.examples.DepthCacheExample;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;

import org.ta4j.core.BaseTick;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.BaseTick;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Tick;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.demo.charts.realtime.RealtimeChart03;

/**
 * @author fromjgor
 *
 */

public class TradeSignalMonitor {
    private String symbol;
	private BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
	private BinanceApiRestClient client = factory.newRestClient();
	private long tradeSessionStartTime = client.getServerTime();
	
	/**
	 * Tick duration 
	 */
	private static final Duration tickDuration = Duration.ofMillis(500);

	/**
	 * Key is the aggregate trade id, and the value contains the aggregated trade
	 * data, which is automatically updated whenever a new agg data stream event
	 * arrives.
	 */
	private Map<Long, AggTrade> aggTradesCache = new HashMap<>();
	/**
	 * Key is the minimal time stamp of the tick, and the value contains the
	 * aggregated trades data, that is automatically collected whenever a new agg
	 * data stream event arrives.
	 */
	private Map<Long, List<AggTrade>> aggTradeTicksCashe = new HashMap<Long, List<AggTrade>>();
	private MercuryRealTimeChart realTimeChart = null;


	/**
	 * Update data by mixing two inputs. 
	 * The first one is delivered by the Rest API and the second one 
	 * is provided whenever a new data stream event arrives. 
	 */

	private void getData() {
		List<AggTrade> aggTrades = client.getAggTrades(this.symbol.toUpperCase());
		for (AggTrade aggTrade : aggTrades) {
			
			Long currentTimestamp = aggTrade.getTradeTime();
			Double price = new Double(aggTrade.getPrice());
			Double quantity = new Double(aggTrade.getQuantity());
			Double amount = price * quantity;
			
			aggTradesCache.put(aggTrade.getAggregatedTradeId(), aggTrade);
		}
		
	}
	
	public static void main(String[] args) {
		  
		  final TradeSignalMonitor tradeSignalMonitor = new TradeSignalMonitor("QTUMBTC");

		    // Setup the panel
		    //final RealtimeChart03 realtimeChart03 = new RealtimeChart03();
		    //final XChartPanel<XYChart> chartPanel = realtimeChart03.buildPanel();
		  
		    // Schedule a job for the event-dispatching thread:
		    // creating and showing this application's GUI.
		    javax.swing.SwingUtilities.invokeLater(new Runnable() {

		      @Override
		      public void run() {
		    	  
		    	/*
		        // Create and set up the window.
		        JFrame frame = new JFrame("XChart");
		        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		        frame.add(chartPanel);

		        // Display the window.
		        frame.pack();
		        frame.setVisible(true);*/
		      }
		    });

		    // Simulate a data feed
		    TimerTask monitorUpdaterTask = new TimerTask() {

		      @Override
		      public void run() {

		    	  tradeSignalMonitor.getData();
		    	  
		    	  /*realtimeChart03.updateData();
		        chartPanel.revalidate();
		        chartPanel.repaint();*/
		    	  
		      }
		    };

		    Timer timer = new Timer();
		    timer.scheduleAtFixedRate(monitorUpdaterTask, 0, 500);
		  }

	public TradeSignalMonitor(String symbol) {
		this.symbol = symbol;
		
		// Test connectivity
		client.ping();

		// Check server time
		tradeSessionStartTime = client.getServerTime();

		initializeAggTradesCache(symbol);
		startAggTradesEventStreaming(symbol);
	}
	

	/**
	 * Initializes the aggTrades cache by using the REST API.
	 */
	private void initializeAggTradesCache(String symbol) {

		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(SecuritySettings.sKEY,SecuritySettings.sSECRET);
		BinanceApiRestClient client = factory.newRestClient();
		
		List<AggTrade> aggTrades = client.getAggTrades(symbol.toUpperCase());
		this.aggTradesCache = new HashMap<>();

		List<Tick> ticks = null;
		Long tickIndex = 0L;
		List<AggTrade> listAggTrade = new CopyOnWriteArrayList<AggTrade>();
		this.aggTradeTicksCashe = new HashMap<Long, List<AggTrade>>();
		ZonedDateTime tickStartTime = ZonedDateTime.now(ZoneId.systemDefault());
		ZonedDateTime tickEndTime = ZonedDateTime.now(ZoneId.systemDefault());

		// Test connectivity
		client.ping();

		// Check server time
		tradeSessionStartTime = client.getServerTime();

		List<Integer> xData = new CopyOnWriteArrayList<Integer>();
		List<Double> yData = new CopyOnWriteArrayList<Double>();
		List<Double> errorBars = new CopyOnWriteArrayList<Double>();
		int counter = 0;
		long startTimestamp = tradeSessionStartTime;
		long endTimestamp = tradeSessionStartTime;
		for (AggTrade aggTrade : aggTrades) {
			Long currentTimestamp = aggTrade.getTradeTime();
			if (currentTimestamp < startTimestamp)
				startTimestamp = currentTimestamp;
			if (currentTimestamp > endTimestamp)
				endTimestamp = currentTimestamp;
		}

		for (AggTrade aggTrade : aggTrades) {

			/*
			 * ZonedDateTime tradeTimestamp =
			 * ZonedDateTime.ofInstant(Instant.ofEpochMilli(aggTrade.getTradeTime()),
			 * ZoneId.systemDefault()); if ( tradeTimestamp.isBefore(tickStartTime))
			 * tickStartTime = tradeTimestamp; if (!tradeTimestamp.isBefore(tickEndTime))
			 * tickEndTime = tradeTimestamp;
			 * 
			 * if (!tradeTimestamp.isBefore(tickEndTime)) { // new tick if ( (tickIndex >0)
			 * && (listAggTrade.size() > 0)) aggTradeTicksCashe.put( tickIndex, listAggTrade
			 * ); tickEndTime = tradeTimestamp.plus(tickDuration); listAggTrade = new
			 * CopyOnWriteArrayList<AggTrade>(); tickIndex++; } listAggTrade.add(aggTrade);
			 */

			Long currentTimestamp = aggTrade.getTradeTime();
			Double price = new Double(aggTrade.getPrice());
			Double quantity = new Double(aggTrade.getQuantity());
			Double amount = price * quantity;
			xData.add((int) (long) (50 * (currentTimestamp - startTimestamp) / (endTimestamp - startTimestamp)));
			yData.add(amount);
			errorBars.add(0.0);
			aggTradesCache.put(aggTrade.getAggregatedTradeId(), aggTrade);
		}

		/*
		 * realTimeChart = new MercuryRealTimeChart(xData,yData, errorBars, response ->
		 * {
		 * 
		 * } );
		 */
	}

	/**
	 * Begins streaming of agg trades events.
	 */
	private void startAggTradesEventStreaming(String symbol) {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiWebSocketClient client = factory.newWebSocketClient();

		client.onAggTradeEvent(symbol.toLowerCase(), response -> {
			Long aggregatedTradeId = response.getAggregatedTradeId();
			AggTrade updateAggTrade = aggTradesCache.get(aggregatedTradeId);
			if (updateAggTrade == null) {
				// new agg trade
				updateAggTrade = new AggTrade();
			}

			updateAggTrade.setTradeTime(response.getEventTime());
			updateAggTrade.setAggregatedTradeId(aggregatedTradeId);
			updateAggTrade.setPrice(response.getPrice());
			updateAggTrade.setQuantity(response.getQuantity());
			updateAggTrade.setFirstBreakdownTradeId(response.getFirstBreakdownTradeId());
			updateAggTrade.setLastBreakdownTradeId(response.getLastBreakdownTradeId());
			updateAggTrade.setBuyerMaker(response.isBuyerMaker());

			// Store the updated agg trade in the cache
			aggTradesCache.put(aggregatedTradeId, updateAggTrade);

			/*
			 * Log AggTrade into database
			 */
			storeAggTradeCache(symbol, updateAggTrade);
			// System.out.println(updateAggTrade);
		});
	}

	private void storeAggTradeCache(String symbol, AggTrade updateAggTrade) {

		AggTradeDumpDb logger = new AggTradeDumpDb(symbol.toUpperCase());
		logger.setAggtrade(updateAggTrade);

		// synchronous call
		// logger.run();

		// asynchronous call
		Thread thread = new Thread(logger);
		thread.start();

		System.out.println(updateAggTrade);
	}

	/**
	 * @return an aggTrades cache, containing the aggregated trade id as the key,
	 *         and the agg trade data as the value.
	 */
	public Map<Long, AggTrade> getAggTradesCache() {
		return aggTradesCache;
	}
	
	
	/**
	 * Builds a list of empty ticks.
	 * 
	 * @param beginTime
	 *            the begin time of the whole period
	 * @param endTime
	 *            the end time of the whole period
	 * @return the list of empty ticks
	 */
	private static List<Tick> buildEmptyTicks(ZonedDateTime beginTime, ZonedDateTime endTime) {

		List<Tick> emptyTicks = new ArrayList<>();

		ZonedDateTime tickEndTime = beginTime;
		do {
			tickEndTime = tickEndTime.plus(tickDuration);
			emptyTicks.add(new BaseTick(tickDuration, tickEndTime));
		} while (tickEndTime.isBefore(endTime));

		return emptyTicks;
	}

	/**
	 * Removes all empty (i.e. with no trade) ticks of the list.
	 * 
	 * @param ticks
	 *            a list of ticks
	 */
	private static void removeEmptyTicks(List<Tick> ticks) {
		for (int i = ticks.size() - 1; i >= 0; i--) {
			if (ticks.get(i).getTrades() == 0) {
				ticks.remove(i);
			}
		}
	}

	/**
	 * @return a time series from Binance exchange trades
	 */
	public TimeSeries loadBinanceSeries(String symbol, List<AggTrade> aggTrades) {
		List<Tick> ticks = null;

		ZonedDateTime beginTime = null;
		ZonedDateTime endTime = null;

		/*
		 * this.aggTradesCache.size();
		 * 
		 * beginTime =
		 * ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("beginTimestamp")),
		 * ZoneId.systemDefault()); endTime =
		 * ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("endTimestamp")),
		 * ZoneId.systemDefault());
		 * 
		 */
		return new BaseTimeSeries(symbol, ticks);
	}
	
}
