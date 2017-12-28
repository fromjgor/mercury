package com.binance.api.client.mercury;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.mercury.*;

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

/**
 * Illustrates how to use the aggTrades event stream to create a local cache of
 * trades for a symbol.
 */
public class TradesRealTimeAnalysis {

	/**
	 * Key is the aggregate trade id, and the value contains the aggregated trade
	 * data, which is automatically updated whenever a new agg data stream event
	 * arrives.
	 */
	private Map<Long, AggTrade> aggTradesCache;
	/**
	 * Key is the minimal time stamp of the tick, and the value contains the aggregated trades
	 * data, that is automatically collected whenever a new agg data stream event
	 * arrives.
	 */	
	//private Map<Long, List<AggTrade>> aggTradeTicksCashe;
	private MercuryRealTimeChart realTimeChart = null;   
	
	/**
	 * Tick duration is 1000 millisecond 
	 */
	private static final Duration tickDuration = Duration.ofMillis(500);
	
	  /*private List<Double> getRandomData(int numPoints) {

		    List<Double> data = new CopyOnWriteArrayList<Double>();
		    for (int i = 0; i < numPoints; i++) {
		      data.add(Math.random() * 100);
		    }
		    return data;
		  }*/

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

		/*this.aggTradesCache.size();
		
		beginTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("beginTimestamp")),
				ZoneId.systemDefault());
		endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("endTimestamp")),
				ZoneId.systemDefault());

		*/
		return new BaseTimeSeries( symbol, ticks );
	}

	
	public TradesRealTimeAnalysis(String symbol) {
		initializeAggTradesCache(symbol);
		startAggTradesEventStreaming(symbol);
	}

	/**
	 * Initializes the aggTrades cache by using the REST API.
	 */
	private void initializeAggTradesCache(String symbol) {

		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiRestClient client = factory.newRestClient();
		List<AggTrade> aggTrades = client.getAggTrades(symbol.toUpperCase());	
		this.aggTradesCache = new HashMap<>();		
		
		
		/*	List<Tick> ticks = null;
		*	Long tickIndex = 0L;
		*	List<AggTrade> listAggTrade = new CopyOnWriteArrayList<AggTrade>();
		*	this.aggTradeTicksCashe = new HashMap<Long, List<AggTrade>>(); 
		*		
		*
		*	ZonedDateTime tickStartTime = ZonedDateTime.now(ZoneId.systemDefault());
		*	ZonedDateTime tickEndTime = ZonedDateTime.now(ZoneId.systemDefault());
		*/

		List<Integer> xData = new CopyOnWriteArrayList<Integer>();
		List<Double> yData = new CopyOnWriteArrayList<Double>();
		List<Double> errorBars = new CopyOnWriteArrayList<Double>();
        int counter = 0;					
		for (AggTrade aggTrade : aggTrades) {
			
			/*ZonedDateTime tradeTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(aggTrade.getTradeTime()),
																						ZoneId.systemDefault());
			if ( tradeTimestamp.isBefore(tickStartTime)) tickStartTime = tradeTimestamp;
			if (!tradeTimestamp.isBefore(tickEndTime))   tickEndTime   = tradeTimestamp;  			

			if (!tradeTimestamp.isBefore(tickEndTime)) 
			{	// new tick
				if ( (tickIndex >0) && (listAggTrade.size() > 0)) aggTradeTicksCashe.put( tickIndex, listAggTrade );
				tickEndTime = tradeTimestamp.plus(tickDuration);
				listAggTrade = new CopyOnWriteArrayList<AggTrade>();				
				tickIndex++;			
			}
			listAggTrade.add(aggTrade);*/
			xData.add(++counter);
			yData.add(new Double(aggTrade.getPrice()));
			aggTradesCache.put(aggTrade.getAggregatedTradeId(), aggTrade);
		}

		realTimeChart = new MercuryRealTimeChart(xData,yData, errorBars);
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

							/*Duration tickDuration = Duration.ofSeconds(1);
							Long tickIndex = new Long(this.aggTradeTicksCashe.size());
							List<AggTrade> listAggTrade = null;    
				
							if (aggTradeTicksCashe.isEmpty()) {
								listAggTrade = new CopyOnWriteArrayList<AggTrade>();
								aggTradeTicksCashe.put( tickIndex, listAggTrade );
							} else {
								listAggTrade = this.aggTradeTicksCashe.get(tickIndex);
							}
							
							ZonedDateTime tradeTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(updateAggTrade.getTradeTime()),
									ZoneId.systemDefault());
							ZonedDateTime tickEndTime = tradeTimestamp.plus(tickDuration);
				
							if (!tradeTimestamp.isBefore(tickEndTime)) 
							{	// new tick
								++tickIndex;
								listAggTrade = new CopyOnWriteArrayList<AggTrade>();
								aggTradeTicksCashe.put( tickIndex, listAggTrade );					
							} 
							// Store the updated agg trade in the current tick cache
							listAggTrade.add(updateAggTrade);*/
							
							/*List<Tick> ticks = null;
							Long tickIndex = 0L;
							List<AggTrade> listAggTrade = new CopyOnWriteArrayList<AggTrade>();
							this.aggTradeTicksCashe = new HashMap<Long, List<AggTrade>>();*/ 
			
					
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
 * Build ticks and Series 
 * */			
			Long trendAnalysisTimeFrame = 5L;  // perform a trend analysis using last 5 seconds time frame
			
			ZonedDateTime tickEndTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(response.getEventTime()), //lastTradeEventTime
					ZoneId.systemDefault());
			ZonedDateTime tickStartTime = tickEndTime.minusSeconds(trendAnalysisTimeFrame);   
			/*	
			 * ZonedDateTime tickStartTime = ZonedDateTime.now(ZoneId.systemDefault());
			 * ZonedDateTime tickEndTime = tickStartTime;
			 * Iterator<Long> aggTradesCacheIterator = aggTradesCache.keySet().iterator();
			 * while (aggTradesCacheIterator.hasNext()) {
				AggTrade aggTrade = aggTradesCache.get(aggTradesCacheIterator.next());
				ZonedDateTime tradeTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(aggTrade.getTradeTime()),ZoneId.systemDefault());
				if ( tradeTimestamp.isBefore(tickStartTime)) tickStartTime = tradeTimestamp;
				if ( tradeTimestamp.isAfter(tickEndTime))    tickEndTime   = tradeTimestamp;
			}
			*/
			
			// Building the empty ticks 
			List<Tick> ticks = buildEmptyTicks(tickStartTime, tickEndTime);
			
			Iterator<Long> aggTradesCacheIterator = aggTradesCache.keySet().iterator();
			while (aggTradesCacheIterator.hasNext()) {
				AggTrade aggTrade = aggTradesCache.get(aggTradesCacheIterator.next());
				ZonedDateTime tradeTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(aggTrade.getTradeTime()),
						ZoneId.systemDefault());
				
				if ( tradeTime.isAfter(tickStartTime) && tradeTime.isBefore(tickEndTime)) {
					//	Filling the ticks with trades
					for (Tick tick : ticks) {
						if (tick.inPeriod(tradeTime)) {
							Double price = new Double(aggTrade.getPrice());
							Double quantity = new Double(aggTrade.getQuantity());
							Double amount = price * quantity;
							tick.addTrade( amount, price);
						}
					}					
				}
			}
			// Removing still empty ticks
			removeEmptyTicks(ticks);
			
			// Build time series
			TimeSeries series = new BaseTimeSeries(symbol, ticks);
			
			
			/*
			 *  Log AggTrade into database
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

	public static void main(String[] args) {
		
		//System.out.println("Hello world!");
		
		TradesRealTimeAnalysis instance = new TradesRealTimeAnalysis("IOTABTC");
		instance.notify();
		
		/*
		new AggTradesCacheExample("NEOBTC");
		new AggTradesCacheExample("ETHBTC");
		new AggTradesCacheExample("QTUMBTC");		
		new AggTradesCacheExample("IOTAETH");

		new DepthCacheExample("NEOBTC");
		new DepthCacheExample("ETHBTC");
		new DepthCacheExample("QTUMBTC");
		new DepthCacheExample("IOTAETH");
		new DepthCacheExample("IOTABTC");
		 */
		
		/*
		 * new CandlesticksCacheExample("IOTAETH", CandlestickInterval. ONE_MINUTE); new
		 * CandlesticksCacheExample("IOTABTC", CandlestickInterval.ONE_MINUTE); new
		 * CandlesticksCacheExample("ETHBTC", CandlestickInterval.ONE_MINUTE);
		 */
	}
}
