package com.binance.api.examples;

import org.ta4j.core.*;
import java.util.Optional;
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
	private static long tradeSessionStartTime;
	

	/**
	 * Key is the minimal time stamp of the tick, and the value contains the aggregated trades
	 * data, that is automatically collected whenever a new agg data stream event
	 * arrives.
	 */
	private static Map<Long, AggTrade> aggTradesCache  = new HashMap<>();

	private static Map<Long, Candlestick> candlesticksCache = new HashMap<>();
	/**
	 * Tick duration 
	 */
	private static final Duration tickDuration = Duration.ofMillis(1000);

	/**
	 * Initializes the Trades cache by using the REST API.
	 */
	private static TimeSeries initializeMovingTimeSeries(String symbol) {

		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiRestClient client = factory.newRestClient();
		
	    // Test connectivity
	    client.ping();

	    // Check server time
	    tradeSessionStartTime = client.getServerTime();
	
		//List<AggTrade> aggTrades = client.getAggTrades(symbol.toUpperCase());	

		CandlestickInterval interval =  CandlestickInterval.ONE_MINUTE;
		List<Candlestick> candlestickBars = client.getCandlestickBars(symbol.toUpperCase(), interval);    	

        long startTimestamp = tradeSessionStartTime;
        long endTimestamp = tradeSessionStartTime;       
        
    	for (Candlestick candlestickBar : candlestickBars) {
    		Long currentTimestamp = candlestickBar.getOpenTime();
        	if ( currentTimestamp <= startTimestamp ) startTimestamp = currentTimestamp;
        	if ( currentTimestamp >= endTimestamp )   endTimestamp = currentTimestamp;
      		candlesticksCache.put(candlestickBar.getOpenTime(), candlestickBar);
    	}
        ZonedDateTime beginTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTimestamp), ZoneId.systemDefault());
        ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp), ZoneId.systemDefault());

        List<Tick> ticks = SqlTradesLoader.buildEmptyTicks(beginTime, endTime, 1); //60 seconds in a minute
       
        for (Candlestick candlestickBar : candlestickBars) {
        	ZonedDateTime tradeTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(candlestickBar.getOpenTime()), ZoneId.systemDefault());
			LAST_TICK_CLOSE_PRICE = Decimal.valueOf(candlestickBar.getClose());
    		for (int i = ticks.size() - 1; i >= 0; i--) {
    			if (ticks.get(i).inPeriod(tradeTime)) {
    				if ( ticks.get(i).getTrades() == 0 ) {
	    				ticks.set(i, new BaseTick( tradeTime,
								Decimal.valueOf(candlestickBar.getOpen()),
								Decimal.valueOf(candlestickBar.getHigh()),
								Decimal.valueOf(candlestickBar.getLow()),
								Decimal.valueOf(candlestickBar.getClose()), 
								Decimal.valueOf(candlestickBar.getVolume())							
									));    					
    				} 
    				ticks.get(i).addTrade(candlestickBar.getVolume(), candlestickBar.getClose());    					
    			}
    		}
        }	

        // Removing still empty ticks
        if ( ticks.size() > 0 ) SqlTradesLoader.removeEmptyTicks(ticks);
        
        return new BaseTimeSeries(symbol, ticks);
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
    	Integer intMaxTickcount = maxTickCount;
		TimeSeries series = //SqlTradesLoader.getInstance(TradingBotOnMovingTimeSeries.symbol).loadSeries(Optional.of(intMaxTickcount));
						    initializeMovingTimeSeries("QTUMBTC");
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
		//Strategy buySellSignals  =  new RSI2Strategy().buildStrategy(series);
        //Strategy buySellSignals  =  new GlobalExtremaStrategy().buildStrategy(series);
        //Strategy buySellSignals  =  new CCICorrectionStrategy().buildStrategy(series);
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, 7);
        //SMAIndicator sma = new SMAIndicator(closePrice, 12);

        // Signals
        // Buy when SMA goes over close price
        // Sell when close price goes over SMA0
        Strategy buySellSignals = new BaseStrategy(
                new OverIndicatorRule(sma, closePrice),
                new UnderIndicatorRule(sma, closePrice)
        );
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

    private static void run(String symbol,int maxTickCount) {
    	
    	TimeSeries series = initializeMovingTimeSeries(symbol);
    	series.setMaximumTickCount(maxTickCount);
    	
    	Tick lastTick = series.getLastTick();
        //ZonedDateTime beginTimeLastTick = lastTick.getBeginTime();
        ZonedDateTime endTimeLastTick = lastTick.getEndTime();
            	
		CandlestickInterval interval =  CandlestickInterval.ONE_MINUTE;
    	BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiRestClient client = factory.newRestClient();
		List<Candlestick> candlestickBars = client.getCandlestickBars(symbol.toUpperCase(), interval);    	
			
		long startOpenTimestamp = tradeSessionStartTime; 
		long endOpenTimestamp = tradeSessionStartTime;
    	for (Candlestick candlestickBar : candlestickBars) {
    		
    		if ( startOpenTimestamp >= candlestickBar.getOpenTime() ) {
    			startOpenTimestamp = candlestickBar.getOpenTime();
    		}
    		
    		if ( endOpenTimestamp <= candlestickBar.getOpenTime() ) {
    			endOpenTimestamp = candlestickBar.getOpenTime();
    		}
      		candlesticksCache.put(candlestickBar.getOpenTime(), candlestickBar);
    	}
        ZonedDateTime endOpenTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endOpenTimestamp), ZoneId.systemDefault());
        
        List<Tick> newTicks = SqlTradesLoader.buildEmptyTicks(endTimeLastTick, endOpenTime, 1); //60 seconds in a minute
    	for (Candlestick candlestickBar : candlestickBars) {
    		ZonedDateTime tradeTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(candlestickBar.getOpenTime()), ZoneId.systemDefault());
    		
    		for (int i = newTicks.size() - 1; i >= 0; i--) {
    			if (newTicks.get(i).inPeriod(tradeTime)) {
    				if ( newTicks.get(i).getTrades() == 0 ) {
    					newTicks.set(i, new BaseTick( tradeTime,
								Decimal.valueOf(candlestickBar.getOpen()),
								Decimal.valueOf(candlestickBar.getHigh()),
								Decimal.valueOf(candlestickBar.getLow()),
								Decimal.valueOf(candlestickBar.getClose()), 
								Decimal.valueOf(candlestickBar.getVolume())							
									));    					
    				}
					Decimal amount = Decimal.valueOf(candlestickBar.getVolume()).multipliedBy(Decimal.valueOf(candlestickBar.getClose()));
					Decimal price = Decimal.valueOf(candlestickBar.getClose());
					newTicks.get(i).addTrade(amount, price);
					LAST_TICK_CLOSE_PRICE = Decimal.valueOf(candlestickBar.getClose());
					/*series.addTick( new BaseTick(
					tradeTime,
					Decimal.valueOf(candlestickBar.getOpen()),
					Decimal.valueOf(candlestickBar.getHigh()),
					Decimal.valueOf(candlestickBar.getLow()),
					Decimal.valueOf(candlestickBar.getClose()), 
					Decimal.valueOf(candlestickBar.getVolume())							
						)			);*/			

    			}
    		}
    	}
        // Removing still empty ticks
        if ( newTicks.size() > 0 ) SqlTradesLoader.removeEmptyTicks(newTicks);

        // Building the trading strategy
        Strategy strategy = buildStrategy(series);
        
        // Initializing the trading history
        TradingRecord tradingRecord = new BaseTradingRecord();
        System.out.println("************************************************************");
      
        
        /**
		   * Begins streaming of depth events.
		   */
	  	factory.newWebSocketClient().onCandlestickEvent(symbol.toLowerCase(), interval, response -> {
	  	  Tick newTick = null;
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
	      
	      long openTimestamp = response.getOpenTime();
	      ZonedDateTime openTimeCandlestick = ZonedDateTime.ofInstant(Instant.ofEpochMilli(openTimestamp), ZoneId.systemDefault());
	      
	      Tick lastKnownTick = series.getLastTick();
	      if (openTimeCandlestick.isAfter(lastKnownTick.getEndTime()) ) {
	    	 newTick = new BaseTick(
					  	openTimeCandlestick,
						Decimal.valueOf(updateCandlestick.getOpen()),
						Decimal.valueOf(updateCandlestick.getHigh()),
						Decimal.valueOf(updateCandlestick.getLow()),
						Decimal.valueOf(updateCandlestick.getClose()), 
						Decimal.valueOf(updateCandlestick.getVolume())							
							);
			 series.addTick( newTick );
             System.out.println("------------------------------------------------------\n"
                    + "Tick "+ series.getTickCount() +" added, close price = " + newTick.getClosePrice().toDouble());
			 
			 LAST_TICK_CLOSE_PRICE = Decimal.valueOf(updateCandlestick.getClose());
	      } else if ( openTimeCandlestick.isBefore(lastKnownTick.getEndTime()) && openTimeCandlestick.isAfter(lastKnownTick.getBeginTime())) {
	    	 
	    	 newTick = lastKnownTick; 
	    	  // new trade within the same tick
	    	 newTick.addTrade(updateCandlestick.getVolume(), response.getClose());
	    	 LAST_TICK_CLOSE_PRICE = Decimal.valueOf(response.getClose());	    	  
	      }
    	  
	
	      // Store this candlestick to the database    
	      //new CandlestickDumpDb(symbol.toUpperCase()).insert(updateCandlestick);
	      
	      // Store the updated candlestick in the cache
	      candlesticksCache.put(openTime, updateCandlestick);
	      System.out.println(updateCandlestick);
	      
	      
          int endIndex = series.getEndIndex();
          newTick 	   = series.getLastTick();
          Decimal amountToBePurchased = Decimal.TEN;
          Decimal amountToBeSold      = Decimal.TEN; 
          if (strategy.shouldEnter(endIndex)) {
              // Our strategy should enter
              System.out.println("Strategy should ENTER on " + endIndex);
              boolean entered = tradingRecord.enter(endIndex, newTick.getClosePrice(), amountToBePurchased);
              if (entered) {
                  Order entry = tradingRecord.getLastEntry();
                  System.out.println("Entered on " + entry.getIndex()
                          + " (price=" + entry.getPrice().toDouble()
                          + ", amount=" + entry.getAmount().toDouble() + ")");
              }
          } else if (strategy.shouldExit(endIndex)) {
              // Our strategy should exit
              System.out.println("Strategy should EXIT on " + endIndex);
              boolean exited = tradingRecord.exit(endIndex, newTick.getClosePrice(), amountToBeSold);
              if (exited) {
                  Order exit = tradingRecord.getLastExit();
                  System.out.println("Exited on " + exit.getIndex()
                          + " (price=" + exit.getPrice().toDouble()
                          + ", amount=" + exit.getAmount().toDouble() + ")");
              }
          }
	      
	    });
    }
    
    /**
     * Generates a random tick.
     * @return a random tick
     */
    private static Tick generateRandomTick() {
    	
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
        run("QTUMBTC",1000);
/*
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
/*        
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
        */
    }
    
}
