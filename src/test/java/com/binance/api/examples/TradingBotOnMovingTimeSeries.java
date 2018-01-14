package com.binance.api.examples;

import org.ta4j.core.*;
import org.ta4j.core.analysis.CashFlow;
import org.ta4j.core.analysis.criteria.AverageProfitableTradesCriterion;
import org.ta4j.core.analysis.criteria.RewardRiskRatioCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;

import java.util.Optional;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandWidthIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.StopGainRule;
import org.ta4j.core.trading.rules.StopLossRule;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;


public class TradingBotOnMovingTimeSeries {

	final private static String symbol = "QTUMBTC";
	private static long tradeSessionStartTime;
	

	/**
	 * Key is the minimal time stamp of the tick, and the value contains the candlestick 
	 * data, that is automatically collected whenever a new candlestick data stream event
	 * arrives.
	 */
	private static Map<Long, Candlestick> candlesticksCache = new HashMap<>();

	/**
	 * FYI: Tick duration = 1 second (tbd) 
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
     * @param series a time series
     * @return a super-puper strategy for crypto
     */
    private static Strategy buildSuperPuperStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // Getting the simple moving average (SMA) of the close price over the last 5 ticks
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        // Here is the 5-ticks-SMA value at the 42nd index
        //System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());

        // Getting a longer SMA (e.g. over the 30 last ticks)
        SMAIndicator longSma = new SMAIndicator(closePrice, 30);


        // Ok, now let's building our trading rules!

        // Buying rules
        // We want to buy:
        //  - if the 5-ticks SMA crosses over 30-ticks SMA
        //  - or if the price goes below a defined price (e.g $40.00)
        Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma)
                .or(new CrossedDownIndicatorRule(closePrice, Decimal.valueOf("40")));
        
        // Selling rules
        // We want to sell:
        //  - if the 5-ticks SMA crosses under 30-ticks SMA
        //  - or if if the price looses more than 5%
        //  - or if the price earns more than 96%
        Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma)
                .or(new StopLossRule(closePrice, Decimal.valueOf("5")))
                .or(new StopGainRule(closePrice, Decimal.valueOf("96")));
        
        // Running our juicy trading strategy...
        Strategy buySellSignals = new BaseStrategy(buyingRule, sellingRule);
        return buySellSignals;

/*        
        // Getting the simple moving average (SMA) of the close price over the last 12 ticks
        SMAIndicator shortSma = new SMAIndicator(closePrice, 12);
        // Here is the 12-ticks-SMA value at the 42nd index
        System.out.println("12-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());

        // Getting a longer SMA (e.g. over the 26 last ticks)
        SMAIndicator longSma = new SMAIndicator(closePrice, 26);

        // Ok, now let's building our trading rules!

        // Buying rules
        // We want to buy:
        //  - if the 12-ticks SMA crosses over 26-ticks SMA
        //  - or if the price goes below a defined price (e.g $40.00)
        Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma)
                .or(new CrossedDownIndicatorRule(closePrice, Decimal.valueOf("40")));
        
        // Selling rules
        // We want to sell:
        //  - if the 5-ticks SMA crosses under 30-ticks SMA
        //  - or if if the price looses more than 3%
        //  - or if the price earns more than 2%
        Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma)
                .or(new StopLossRule(closePrice, Decimal.valueOf("3")))
                .or(new StopGainRule(closePrice, Decimal.valueOf("2")));
        

        
        // Getting bollinger bands
        SMAIndicator sma = new SMAIndicator(closePrice, 12);
        StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePrice, 12);        
        BollingerBandsMiddleIndicator bbmSMA = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsUpperIndicator bbuSMA = new BollingerBandsUpperIndicator(bbmSMA, standardDeviation);
        BollingerBandsLowerIndicator bblSMA = new BollingerBandsLowerIndicator(bbmSMA, standardDeviation);
        BollingerBandWidthIndicator bandwidth = new BollingerBandWidthIndicator(bbuSMA, bbmSMA, bblSMA);
        
        

        
        // Running our juicy trading strategy...
        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(new BaseStrategy(buyingRule, sellingRule));
        System.out.println("Number of trades for our strategy: " + tradingRecord.getTradeCount());

        // Analysis

        // Getting the cash flow of the resulting trades
        CashFlow cashFlow = new CashFlow(series, tradingRecord);

        // Getting the profitable trades ratio
        AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
        System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, tradingRecord));
        // Getting the reward-risk ratio
        AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
        System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, tradingRecord));

        // Total profit of our strategy
        // vs total profit of a buy-and-hold strategy
        AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
        System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, tradingRecord));
        
        Strategy buySellSignals = new BaseStrategy(buyingRule, sellingRule);
        return buySellSignals;
*/    }
    
    private static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
                
        @SuppressWarnings("static-access")
		//Strategy buySellSignals  =  new RSI2Strategy().buildStrategy(series);
        //Strategy buySellSignals  =  new GlobalExtremaStrategy().buildStrategy(series);
        //Strategy buySellSignals  =  new CCICorrectionStrategy().buildStrategy(series);        

        SMAIndicator sma = new SMAIndicator(closePrice, 12);
        
        //SMAIndicator sma = new SMAIndicator(closePrice, 7);

        // Signals
        // Buy when SMA goes over close price
        // Sell when close price goes over SMA0
        Strategy buySellSignals = new BaseStrategy(
                new OverIndicatorRule(sma, closePrice),
                new UnderIndicatorRule(sma, closePrice)
        );
        return buySellSignals;
    }

    private static void backtest(int maxtickcount) {
        // Getting a time series (from any provider: CSV, web service, etc.)
        TimeSeries series = SqlTradesLoader.getInstance(TradingBotOnMovingTimeSeries.symbol).
        		loadSeriesFromJournal(Optional.of(maxtickcount));

        // Getting the close price of the ticks
        Decimal firstClosePrice = series.getTick(0).getClosePrice();
        System.out.println("First close price: " + firstClosePrice.toDouble());
        // Or within an indicator:
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Here is the same close price:
        System.out.println(firstClosePrice.isEqual(closePrice.getValue(0))); // equal to firstClosePrice

        // Getting the simple moving average (SMA) of the close price over the last 5 ticks
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        // Here is the 5-ticks-SMA value at the 42nd index
        System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());

        // Getting a longer SMA (e.g. over the 30 last ticks)
        SMAIndicator longSma = new SMAIndicator(closePrice, 30);


        // Ok, now let's building our trading rules!

        // Buying rules
        // We want to buy:
        //  - if the 5-ticks SMA crosses over 30-ticks SMA
        //  - or if the price goes below a defined price (e.g $40.00)
        Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma)
                .or(new CrossedDownIndicatorRule(closePrice, Decimal.valueOf("40")));
        
        // Selling rules
        // We want to sell:
        //  - if the 5-ticks SMA crosses under 30-ticks SMA
        //  - or if if the price looses more than 3%
        //  - or if the price earns more than 2%
        Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma)
                .or(new StopLossRule(closePrice, Decimal.valueOf("3")))
                .or(new StopGainRule(closePrice, Decimal.valueOf("2")));
        
        // Running our juicy trading strategy...
        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(new BaseStrategy(buyingRule, sellingRule));
        System.out.println("Number of trades for our strategy: " + tradingRecord.getTradeCount());


        // Analysis

        // Getting the cash flow of the resulting trades
        CashFlow cashFlow = new CashFlow(series, tradingRecord);

        // Getting the profitable trades ratio
        AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
        System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, tradingRecord));
        // Getting the reward-risk ratio
        AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
        System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, tradingRecord));

        // Total profit of our strategy
        // vs total profit of a buy-and-hold strategy
        AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
        System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, tradingRecord));

        // Your turn!
    }

    private static Decimal walletAmount   = Decimal.valueOf(1);//BTC
    private static Decimal walletQuantity = Decimal.valueOf(0);
    
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
        ZonedDateTime startOpenTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startOpenTimestamp), ZoneId.systemDefault());
        ZonedDateTime endOpenTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endOpenTimestamp), ZoneId.systemDefault());
        
        //List<Tick> newTicks = SqlTradesLoader.buildEmptyTicks(endTimeLastTick, endOpenTime, 1); 
        List<Tick> newTicks = SqlTradesLoader.buildEmptyTicks(startOpenTime,endOpenTime, 1); 
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
					// Store this candlestick to the database    
				    new CandlestickDumpDb(symbol.toUpperCase()).setInterval(interval).setCandlestick(candlestickBar).run();
    			}
    		}
    	}
        // Removing still empty ticks
        if ( newTicks.size() > 0 ) SqlTradesLoader.removeEmptyTicks(newTicks);
        series.setMaximumTickCount(maxTickCount);
        
        // Building the trading strategy
        ///////////////////////////////Strategy strategy = buildStrategy(series);
        Strategy strategy = buildSuperPuperStrategy(series);
        
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
                    + "Tick "+ series.getTickCount() +" added at " +  lastKnownTick.getEndTime() + ", close price = " + newTick.getClosePrice().toDouble());
			 
			 LAST_TICK_CLOSE_PRICE = Decimal.valueOf(updateCandlestick.getClose());
	      } else if ( openTimeCandlestick.isBefore(lastKnownTick.getEndTime()) && openTimeCandlestick.isAfter(lastKnownTick.getBeginTime())) {
	    	 
	    	 newTick = lastKnownTick; 
	    	  // new trade within the same tick
	    	 newTick.addTrade(updateCandlestick.getVolume(), response.getClose());
	    	 LAST_TICK_CLOSE_PRICE = Decimal.valueOf(response.getClose());	    	  
	      }
	
	      // Store the updated candlestick in the cache
	      candlesticksCache.put(openTime, updateCandlestick);
	      // Store this candlestick to the database (async call)       	      
		  new CandlestickDumpDb(symbol.toUpperCase()).setInterval(interval).setCandlestick(updateCandlestick).run();
	      	      
	      System.out.println(updateCandlestick);

/* trade chapter */	      
	      
          int endIndex = series.getEndIndex();
          newTick 	   = series.getLastTick();
          Decimal amountToBePurchased = Decimal.valueOf(40);//Decimal.TEN;
          Decimal amountToBeSold      = Decimal.valueOf(40);//Decimal.TEN;
          
          Double fee = 0.001;// actually the value is 0.0005 at binance.com
          
          if (strategy.shouldEnter(endIndex)) {
              // Our strategy should enter
              System.out.println("Strategy should ENTER on " + endIndex);
              boolean entered = tradingRecord.enter(endIndex, newTick.getClosePrice(), amountToBePurchased);
              if (entered) {
                  Order entry = tradingRecord.getLastEntry();
                  
              	walletAmount  = walletAmount.minus(Decimal.valueOf( (1+fee) * entry.getPrice().toDouble() * entry.getAmount().toDouble()));              	 
            	walletQuantity = walletQuantity.minus(Decimal.valueOf((1+fee) * entry.getAmount().toDouble()));
                  
                  System.out.println("Entered on " + entry.getIndex()
                          + " (price=" + entry.getPrice().toDouble()
                          + ", amount=" + entry.getAmount().toDouble() 
                          + " Wallet: " + walletAmount.toDouble()
                		  + ")");
              }
          } else if (strategy.shouldExit(endIndex)) {
              // Our strategy should exit
              System.out.println("Strategy should EXIT on " + endIndex);
              boolean exited = tradingRecord.exit(endIndex, newTick.getClosePrice(), amountToBeSold);
              if (exited) {
                  Order exit = tradingRecord.getLastExit();
                  
	              walletAmount  = walletAmount.plus(Decimal.valueOf((1-fee) * exit.getPrice().toDouble() * exit.getAmount().toDouble()));              	 
	              walletQuantity = walletQuantity.plus(Decimal.valueOf((1-fee) * exit.getAmount().toDouble()));
                  
                  System.out.println("Exited on " + exit.getIndex()
                          + " (price=" + exit.getPrice().toDouble()
                          + ", amount=" + exit.getAmount().toDouble() + " Wallet: " + walletAmount.toDouble()+ ")");
                  
                  
                  // Analysis

							                  // Getting the cash flow of the resulting trades
							                  /*CashFlow cashFlow = new CashFlow(series, tradingRecord);
							                  for( int i=0; i<cashFlow.getSize()-1; i++) {
							                	  System.out.println("Cashflow on step #: " + i + " = " + cashFlow.getValue(i));
							                  }*/
                  
                  // Getting the profitable trades ratio
                  AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
                  System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, tradingRecord));
                  // Getting the reward-risk ratio
                  AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
                  System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, tradingRecord));

                  // Total profit of our strategy
                  // vs total profit of a buy-and-hold strategy
                  AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
                  System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, tradingRecord));
              }
          }
	      
	    });
    }
    
    public static void main(String[] args) throws InterruptedException {
    
        System.out.println("********************** Initialization **********************");
                
        run("QTUMBTC",1000);
        
        //backtest(60*60*12);
    }
    
}
