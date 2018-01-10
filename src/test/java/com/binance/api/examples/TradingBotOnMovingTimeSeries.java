package com.binance.api.examples;

import org.ta4j.core.*;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;
import java.time.ZonedDateTime;

import com.binance.api.client.mercury.*;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.mercury.*;

import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;


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


public class TradingBotOnMovingTimeSeries {

	final private static String symbol = "QTUMBTC";
	private long tradeSessionStartTime;
	

	/**
	 * Key is the minimal time stamp of the tick, and the value contains the aggregated trades
	 * data, that is automatically collected whenever a new agg data stream event
	 * arrives.
	 */
	private Map<Long, AggTrade> aggTradesCache;
	
	/**
	 * Tick duration 
	 */
	private static final Duration tickDuration = Duration.ofMillis(500);

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

	    // Test connectivity
	    client.ping();

	    // Check server time
	    tradeSessionStartTime = client.getServerTime();

        long startTimestamp = tradeSessionStartTime;
        long endTimestamp = tradeSessionStartTime;
        for (AggTrade aggTrade : aggTrades) {
        	Long currentTimestamp = aggTrade.getTradeTime();
        	if ( currentTimestamp < startTimestamp ) startTimestamp = currentTimestamp;
        	if ( currentTimestamp > endTimestamp ) 	 endTimestamp = currentTimestamp;
        }
        
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
			
			Long currentTimestamp = aggTrade.getTradeTime();
			Double price = new Double(aggTrade.getPrice());
			Double quantity = new Double(aggTrade.getQuantity());
			Double amount = price * quantity;
			
			aggTradesCache.put(aggTrade.getAggregatedTradeId(), aggTrade);
		}	
	}


    /** Close price of the last tick */
    private static Decimal LAST_TICK_CLOSE_PRICE;

    /**
     * Builds a moving time series (i.e. keeping only the maxTickCount last ticks)
     * @param maxTickCount the number of ticks to keep in the time series (at maximum)
     * @return a moving time series
     */
    private static TimeSeries initMovingTimeSeries(int maxTickCount) {
 
        //TimeSeries series = CsvTradesLoader.loadBitstampSeries();
		TimeSeries series = SqlTradesLoader.getInstance(TradingBotOnMovingTimeSeries.symbol).loadSeries();
        System.out.print("Initial tick count: " + series.getTickCount());
        // Limitating the number of ticks to maxTickCount
        series.setMaximumTickCount(maxTickCount);
        LAST_TICK_CLOSE_PRICE = series.getTick(series.getEndIndex()).getClosePrice();
        System.out.println(" (limited to " + maxTickCount + "), close price = " + LAST_TICK_CLOSE_PRICE);
        return series;
    }

    /**
     * @param series a time series
     * @return a dummy strategy
     */
    private static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        @SuppressWarnings("static-access")
		Strategy buySellSignals  =  new RSI2Strategy().buildStrategy(series);
        
        /*ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 12);

        // Signals
        // Buy when SMA goes over close price
        // Sell when close price goes over SMA0
        Strategy buySellSignals = new BaseStrategy(
                new OverIndicatorRule(sma, closePrice),
                new UnderIndicatorRule(sma, closePrice)
        );*/
        return buySellSignals;
    }

    /**
     * Generates a random decimal number between min and max.
     * @param min the minimum bound
     * @param max the maximum bound
     * @return a random decimal number between min and max
     */
    private static Decimal randDecimal(Decimal min, Decimal max) {
        Decimal randomDecimal = null;
        if (min != null && max != null && min.isLessThan(max)) {
            randomDecimal = max.minus(min).multipliedBy(Decimal.valueOf(Math.random())).plus(min);
        }
        return randomDecimal;
    }

    /**
     * Generates a random tick.
     * @return a random tick
     */
    private static Tick generateRandomTick() {
		CandlestickInterval interval =  CandlestickInterval.ONE_MINUTE;
    	BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiRestClient client = factory.newRestClient();
		List<AggTrade> aggTrades = client.getAggTrades(symbol.toUpperCase());	
    	
		Map<Long, AggTrade> aggTradesCache = new HashMap<>();		
		
		
		List<Candlestick> candlestickBars = client.getCandlestickBars(symbol.toUpperCase(), interval);

		Map<Long, Candlestick> candlesticksCache = new TreeMap<>();
    	for (Candlestick candlestickBar : candlestickBars) {
      		candlesticksCache.put(candlestickBar.getOpenTime(), candlestickBar);
      		// Store this candlestick to the database    
	      	new CandlestickDumpDb(symbol.toUpperCase()).insert(candlestickBar);      		
    	}
		
		  /**
		   * Begins streaming of depth events.
		   */
	  	factory.newWebSocketClient().onCandlestickEvent(symbol.toLowerCase(), interval, response -> {
	      Long openTime = response.getOpenTime();
	      Candlestick updateCandlestick = candlesticksCache.get(openTime);
	      if (updateCandlestick == null) {
	        // new candlestick
	        updateCandlestick = new Candlestick();
	      }
	      // update candlestick with the stream data
	      updateCandlestick.setOpenTime(response.getOpenTime());
	      updateCandlestick.setOpen(response.getOpen());
	      updateCandlestick.setLow(response.getLow());
	      updateCandlestick.setHigh(response.getHigh());
	      updateCandlestick.setClose(response.getClose());
	      updateCandlestick.setCloseTime(response.getCloseTime());
	      updateCandlestick.setVolume(response.getVolume());
	      updateCandlestick.setNumberOfTrades(response.getNumberOfTrades());
	      updateCandlestick.setQuoteAssetVolume(response.getQuoteAssetVolume());
	      updateCandlestick.setTakerBuyQuoteAssetVolume(response.getTakerBuyQuoteAssetVolume());
	      updateCandlestick.setTakerBuyBaseAssetVolume(response.getTakerBuyQuoteAssetVolume());
	
	      // Store this candlestick to the database    
	      new CandlestickDumpDb(symbol.toUpperCase()).insert(updateCandlestick);
	      
	      // Store the updated candlestick in the cache
	      candlesticksCache.put(openTime, updateCandlestick);
	      System.out.println(updateCandlestick);
	    });
		
		
		
		/*	List<Tick> ticks = null;
		*	Long tickIndex = 0L;
		*	List<AggTrade> listAggTrade = new CopyOnWriteArrayList<AggTrade>();
		*	this.aggTradeTicksCashe = new HashMap<Long, List<AggTrade>>(); 
		*		
		*
		*	ZonedDateTime tickStartTime = ZonedDateTime.now(ZoneId.systemDefault());
		*	ZonedDateTime tickEndTime = ZonedDateTime.now(ZoneId.systemDefault());
		*/

	    // Test connectivity
	    client.ping();

	    // Check server time
	    long tradeSessionStartTime = client.getServerTime();
        long startTimestamp = tradeSessionStartTime;
        long endTimestamp = tradeSessionStartTime;
        
        for (AggTrade aggTrade : aggTrades) {
        	Long currentTimestamp = aggTrade.getTradeTime();
        	if ( currentTimestamp < startTimestamp ) startTimestamp = currentTimestamp;
        	if ( currentTimestamp > endTimestamp ) 	 endTimestamp = currentTimestamp;
        }
        
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
			
			/*
			** Long currentTimestamp = aggTrade.getTradeTime();
			** Double price = new Double(aggTrade.getPrice());
			** Double quantity = new Double(aggTrade.getQuantity());
			** Double amount = price * quantity;
			*/

			aggTradesCache.put(aggTrade.getAggregatedTradeId(), aggTrade);
		}	    	    	
    	
		/**/    	
    	
    	
        final Decimal maxRange = Decimal.valueOf("0.03"); // 3.0%
        Decimal openPrice = LAST_TICK_CLOSE_PRICE;
        Decimal minPrice = openPrice.minus(openPrice.multipliedBy(maxRange.multipliedBy(Decimal.valueOf(Math.random()))));
        Decimal maxPrice = openPrice.plus(openPrice.multipliedBy(maxRange.multipliedBy(Decimal.valueOf(Math.random()))));
        Decimal closePrice = randDecimal(minPrice, maxPrice);
        LAST_TICK_CLOSE_PRICE = closePrice;
        return new BaseTick(ZonedDateTime.now(), openPrice, maxPrice, minPrice, closePrice, Decimal.ONE);
    }

    public static void main(String[] args) throws InterruptedException {
    
        System.out.println("********************** Initialization **********************");
        // Getting the time series
        TimeSeries series = initMovingTimeSeries(500);

        // Building the trading strategy
        Strategy strategy = buildStrategy(series);
        
        // Initializing the trading history
        TradingRecord tradingRecord = new BaseTradingRecord();
        System.out.println("************************************************************");
        
        /**
         * We run the strategy for the 50 next ticks.
         */
        for (int i = 0; i < 500; i++) {

            // New tick
            Thread.sleep(30); // I know...
            Tick newTick = generateRandomTick();
            System.out.println("------------------------------------------------------\n"
                    + "Tick "+i+" added, close price = " + newTick.getClosePrice().toDouble());
            series.addTick(newTick);
            
            int endIndex = series.getEndIndex();
            if (strategy.shouldEnter(endIndex)) {
                // Our strategy should enter
                System.out.println("Strategy should ENTER on " + endIndex);
                boolean entered = tradingRecord.enter(endIndex, newTick.getClosePrice(), Decimal.TEN);
                if (entered) {
                    Order entry = tradingRecord.getLastEntry();
                    System.out.println("Entered on " + entry.getIndex()
                            + " (price=" + entry.getPrice().toDouble()
                            + ", amount=" + entry.getAmount().toDouble() + ")");
                }
            } else if (strategy.shouldExit(endIndex)) {
                // Our strategy should exit
                System.out.println("Strategy should EXIT on " + endIndex);
                boolean exited = tradingRecord.exit(endIndex, newTick.getClosePrice(), Decimal.TEN);
                if (exited) {
                    Order exit = tradingRecord.getLastExit();
                    System.out.println("Exited on " + exit.getIndex()
                            + " (price=" + exit.getPrice().toDouble()
                            + ", amount=" + exit.getAmount().toDouble() + ")");
                }
            }
        }
    }
}
