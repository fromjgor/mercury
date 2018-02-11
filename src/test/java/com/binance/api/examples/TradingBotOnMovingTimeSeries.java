package com.binance.api.examples;

import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoUnit;

import org.slf4j.LoggerFactory;
import org.ta4j.core.*;
import org.ta4j.core.trading.rules.*;
import org.ta4j.core.analysis.CashFlow;
import org.ta4j.core.analysis.criteria.AverageProfitableTradesCriterion;
import org.ta4j.core.analysis.criteria.RewardRiskRatioCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ta4j.core.indicators.helpers.MedianPriceIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.StochasticRSIIndicator;
import org.ta4j.core.indicators.AwesomeOscillatorIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandWidthIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelLowerIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelMiddleIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelUpperIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import java.time.ZonedDateTime;

import com.binance.api.client.mercury.*;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.CandlestickInterval;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;

public class TradingBotOnMovingTimeSeries {

	final private static String symbol = "QTUMBTC";
	private static long tradeSessionStartTime;

	/**
	 * Key is the minimal time stamp of the tick, and the value contains the
	 * candlestick data, that is automatically collected whenever a new candlestick
	 * data stream event arrives.
	 */
	private static List<Tick> ticks = new ArrayList<>();
	private static Map<Long, Candlestick> candlesticksCache = new HashMap<>();
	private static Map<Long, AggTrade> aggTradesCache = new HashMap<>();
	private static TimeSeries series = null;
	// Initializing the trading history
	private static TradingRecord tradingRecord = new BaseTradingRecord();

	// Building the trading strategy
	private static Strategy strategy = null;

	/**
	 * FYI: Tick duration = 1 second (tbd)
	 */
	private static final Duration tickDuration = Duration.ofSeconds(SqlTradesLoader.getTicksPerSecond());

	/**
	 * Initializes the aggTrades cache by using the REST API.
	 */
	private void initializeAggTradesCache(String symbol) {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiRestClient client = factory.newRestClient();
		List<AggTrade> aggTrades = client.getAggTrades(symbol.toUpperCase());

		// Check server time
		tradeSessionStartTime = client.getServerTime();
		long startTimestamp = tradeSessionStartTime;
		long endTimestamp = tradeSessionStartTime;

		// this.aggTradesCache = new HashMap<>();
		for (AggTrade aggTrade : aggTrades) {

			// Long currentTimestamp = aggTrade.getTradeTime();
			Long currentTimestamp = ((aggTrade.getTradeTime() + 500) / 1000) * 1000;

			if (currentTimestamp <= startTimestamp)
				startTimestamp = currentTimestamp;
			if (currentTimestamp >= endTimestamp)
				endTimestamp = currentTimestamp;

			aggTradesCache.put(aggTrade.getAggregatedTradeId(), aggTrade);
		}

		/**
		 * Generate and calculate ticks:
		 * 
		 * Depending on the type of stock chart you want to create, you must include a
		 * specific combination of data series in the cache - and put the data series in
		 * order:
		 * 
		 * - Volume traded - Opening price - High price - Low price - Closing price
		 */
		ZonedDateTime beginTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTimestamp), ZoneId.systemDefault());
		ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp), ZoneId.systemDefault());

		ticks = SqlTradesLoader.buildEmptyTicks(beginTime, endTime, SqlTradesLoader.getTicksPerSecond());

		for (AggTrade aggTrade : aggTrades) {

			Long currentTimestamp = aggTrade.getTradeTime();
			ZonedDateTime tradeTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentTimestamp),
					ZoneId.systemDefault());

			for (Tick tick : ticks) {
				if (tick.inPeriod(tradeTime)) {
					Decimal volume = Decimal.valueOf(aggTrade.getQuantity());
					Decimal price = Decimal.valueOf(aggTrade.getPrice());
					tick.addTrade(volume, price);
					break;
				}
			}
		}
		SqlTradesLoader.removeEmptyTicks(ticks);
	}

	private static final URL LOGBACK_CONF_FILE = TradingBotOnMovingTimeSeries.class.getClassLoader()
			.getResource("logback-traces.xml");

	/**
	 * Loads the Logback configuration from a resource file. Only here to avoid
	 * polluting other examples with logs. Could be replaced by a simple logback.xml
	 * file in the resource folder.
	 */
	private static void loadLoggerConfiguration() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();

		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(context);
		/**/
		try {
			configurator.doConfigure(LOGBACK_CONF_FILE);
		} catch (JoranException je) {
			Logger.getLogger(TradingBotOnMovingTimeSeries.class.getName()).log(Level.SEVERE,
					"Unable to load Logback configuration", je);
		}

	}

	/**
	 */
	private void startAggTradesEventStreaming(String symbol, int maxTickCount) {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiWebSocketClient client = factory.newWebSocketClient();

		// TimeSeries series = new BaseTimeSeries(symbol, ticks);
		series = new BaseTimeSeries(symbol, ticks);
		series.setMaximumTickCount(maxTickCount);
		// strategy = buildStrategy(series);

		/**
		 * strategy = buildStrategyTrendFollowing(series);// buildStrategy(series);
		 * 
		 */
		strategy = buildMovingMomentumStrategy(series);
		System.out.println("************************************************************");

		client.onAggTradeEvent(symbol.toLowerCase(), response -> {
			Long aggregatedTradeId = response.getAggregatedTradeId();
			AggTrade updateAggTrade = aggTradesCache.get(aggregatedTradeId);
			if (updateAggTrade == null) {
				// new agg trade
				updateAggTrade = new AggTrade();
			}
			updateAggTrade.setAggregatedTradeId(aggregatedTradeId);
			updateAggTrade.setPrice(response.getPrice());
			updateAggTrade.setQuantity(response.getQuantity());
			updateAggTrade.setFirstBreakdownTradeId(response.getFirstBreakdownTradeId());
			updateAggTrade.setLastBreakdownTradeId(response.getLastBreakdownTradeId());
			updateAggTrade.setBuyerMaker(response.isBuyerMaker());

			Long ts = response.getTradeTime();
			// round off the timestamp to nearest second:
			ts = ((response.getTradeTime() + 500) / 1000) * 1000;
			updateAggTrade.setTradeTime(ts);

			// Store the updated agg trade in the cache
			aggTradesCache.put(aggregatedTradeId, updateAggTrade);
			log(updateAggTrade); // store to db

			ZonedDateTime updateAggTradeTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts),
					ZoneId.systemDefault());

			Tick lastick = series.getLastTick();

			/**
			 * inPeriod() returns false if endTime is equal or less to the given timestamp.
			 * Since all ticks within a second will end up having the same ts, inPeriod()
			 * must return true.
			 */

			// if ( lastick.inPeriod(updateAggTradeTime) ) {
			if (!updateAggTradeTime.isBefore(lastick.getBeginTime())
					/* bugfix that works for me: to make sure that endTime is included in period! */
					&& (updateAggTradeTime.isEqual(lastick.getEndTime())
							|| updateAggTradeTime.isBefore(lastick.getEndTime()))) {

				lastick.addTrade(response.getQuantity(), response.getPrice());
				System.out.println("tick #" + series.getEndIndex() + " updated with closing price: "
						+ lastick.getClosePrice() + " [" + lastick.getBeginTime() + " :" + lastick.getEndTime() + "]");
			} else if (updateAggTradeTime.isAfter(series.getLastTick().getEndTime())) {

				// Duration tickDuration = Duration.ofSeconds(duration);
				ZonedDateTime tickEndTime = lastick.getBeginTime();
				do {
					tickEndTime = tickEndTime.plus(tickDuration);
				} while (tickEndTime.isBefore(updateAggTradeTime));

				try {
					Tick nextick = new BaseTick(tickDuration, tickEndTime);
					nextick.addTrade(Decimal.valueOf(response.getQuantity()), Decimal.valueOf(response.getPrice()));
					ticks.add(nextick);

					series = new BaseTimeSeries(symbol, ticks);
					series.setMaximumTickCount(maxTickCount);

					/**
					 * strategy = buildStrategyTrendFollowing(series);// buildStrategy(series);
					 * 
					 */
					strategy = buildMovingMomentumStrategy(series);

					/*
					 * if (nextick.inPeriod(updateAggTradeTime)) { if
					 * (lastick.getEndTime().isBefore(nextick.getEndTime())) {
					 * series.addTick(nextick); System.out.println("new tick added #" +
					 * series.getEndIndex()); } }
					 */
				} catch (java.lang.IllegalArgumentException e) {
					System.out.println(e);
				}
			}

			System.out.println(updateAggTrade);

			/* trade chapter */

			int endIndex = series.getEndIndex();
			Tick newTick = series.getLastTick();
			Decimal amountToBePurchased = Decimal.valueOf(40);// Decimal.TEN;
			Decimal amountToBeSold = Decimal.valueOf(40);// Decimal.TEN;

			Double fee = 0.001;// actually the value is 0.0005 at binance.com

			// rebuild strategy (otherwise s = strategy)
			if (strategy.shouldEnter(endIndex)) {

				// Our strategy should enter
				System.out.println("Strategy should ENTER on " + endIndex);
				boolean entered = tradingRecord.enter(endIndex, newTick.getClosePrice(), amountToBePurchased);
				if (entered) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					Order entry = tradingRecord.getLastEntry();

					walletAmount = walletAmount.minus(
							Decimal.valueOf((1 + fee) * entry.getPrice().toDouble() * entry.getAmount().toDouble()));
					walletQuantity = walletQuantity.minus(Decimal.valueOf((1 + fee) * entry.getAmount().toDouble()));

					System.out.println("Entered on " + entry.getIndex() + " (price=" + entry.getPrice().toDouble()
							+ ", amount=" + entry.getAmount().toDouble() + " Wallet: " + walletAmount.toDouble() + ")");
				}
			} else if (strategy.shouldExit(endIndex)) {
				// Our strategy should exit

				System.out.println("Strategy should EXIT on " + endIndex);
				boolean exited = tradingRecord.exit(endIndex, newTick.getClosePrice(), amountToBeSold);
				if (exited) {
					for (int i = 0; i < 3; i++)
						java.awt.Toolkit.getDefaultToolkit().beep();

					Order exit = tradingRecord.getLastExit();

					walletAmount = walletAmount.plus(
							Decimal.valueOf((1 - fee) * exit.getPrice().toDouble() * exit.getAmount().toDouble()));
					walletQuantity = walletQuantity.plus(Decimal.valueOf((1 - fee) * exit.getAmount().toDouble()));

					System.out.println("Exited on " + exit.getIndex() + " (price=" + exit.getPrice().toDouble()
							+ ", amount=" + exit.getAmount().toDouble() + " Wallet: " + walletAmount.toDouble() + ")");

					// Analysis

					// Getting the cash flow of the resulting trades
					/*
					 * CashFlow cashFlow = new CashFlow(series, tradingRecord); for( int i=0;
					 * i<cashFlow.getSize()-1; i++) { System.out.println("Cashflow on step #: " + i
					 * + " = " + cashFlow.getValue(i)); }
					 */

					// Getting the profitable trades ratio
					AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
					System.out
							.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, tradingRecord));
					// Getting the reward-risk ratio
					AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
					System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, tradingRecord));

					// Total profit of our strategy
					// vs total profit of a buy-and-hold strategy
					AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
					System.out.println(
							"Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, tradingRecord));
				}
			}

		});
	}

	private static TimeSeries initializeMovingTimeSeries(String symbol) {

		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiRestClient client = factory.newRestClient();

		// Test connectivity
		client.ping();

		// Check server time
		tradeSessionStartTime = client.getServerTime();

		List<AggTrade> aggTrades = client.getAggTrades(symbol.toUpperCase());

		CandlestickInterval interval = CandlestickInterval.ONE_MINUTE;
		List<Candlestick> candlestickBars = client.getCandlestickBars(symbol.toUpperCase(), interval);

		long startTimestamp = tradeSessionStartTime;
		long endTimestamp = tradeSessionStartTime;

		for (Candlestick candlestickBar : candlestickBars) {
			Long currentTimestamp = candlestickBar.getOpenTime();
			if (currentTimestamp <= startTimestamp)
				startTimestamp = currentTimestamp;
			if (currentTimestamp >= endTimestamp)
				endTimestamp = currentTimestamp;
			candlesticksCache.put(candlestickBar.getOpenTime(), candlestickBar);
		}

		// reserve time for some ticks will come
		// endTimestamp += 1 * 1000L;

		ZonedDateTime beginTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTimestamp), ZoneId.systemDefault());
		ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp), ZoneId.systemDefault());

		List<Tick> ticks = SqlTradesLoader.buildEmptyTicks(beginTime, endTime, SqlTradesLoader.getTicksPerMilli());

		for (Candlestick candlestickBar : candlestickBars) {
			ZonedDateTime tradeTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(candlestickBar.getOpenTime()),
					ZoneId.systemDefault());
			LAST_TICK_CLOSE_PRICE = Decimal.valueOf(candlestickBar.getClose());
			for (int i = ticks.size() - 1; i >= 0; i--) {
				if (ticks.get(i).inPeriod(tradeTime)) {
					if (ticks.get(i).getTrades() == 0) {
						ticks.set(i, new BaseTick(tradeTime, Decimal.valueOf(candlestickBar.getOpen()),
								Decimal.valueOf(candlestickBar.getHigh()), Decimal.valueOf(candlestickBar.getLow()),
								Decimal.valueOf(candlestickBar.getClose()),
								Decimal.valueOf(candlestickBar.getVolume())));
					}
					ticks.get(i).addTrade(candlestickBar.getVolume(), candlestickBar.getClose());
				}
			}
		}

		// Removing still empty ticks
		if (ticks.size() > 0)
			SqlTradesLoader.removeEmptyTicks(ticks);

		return new BaseTimeSeries(symbol, ticks);
	}

	/** Close price of the last tick */
	private static Decimal LAST_TICK_CLOSE_PRICE;

	private static Map<Integer, Rule> sellingRulesCatalog = new HashMap<>();
	private static Map<Integer, Integer> sellingRulesHistory = new HashMap<>();// Rule Number, Tick Index when the Rule
																				// was satisfied

	/**
	 * @param series
	 *            a time series
	 * @return a moving momentum strategy
	 */
	public Strategy buildMovingMomentumStrategy(TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

		// The bias is bullish when the shorter-moving average moves above the longer
		// moving average.
		// The bias is bearish when the shorter-moving average moves below the longer
		// moving average.
		EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
		EMAIndicator longEma = new EMAIndicator(closePrice, 26);

		StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);

		MACDIndicator macd = new MACDIndicator(closePrice, 9, 26);
		EMAIndicator emaMacd = new EMAIndicator(macd, 18);

		// Entry rule
		Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
				.and(new CrossedDownIndicatorRule(stochasticOscillK, Decimal.valueOf(20))) // Signal 1
				.and(new OverIndicatorRule(macd, emaMacd)); // Signal 2

		// Exit rule
		Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
				.and(new CrossedUpIndicatorRule(stochasticOscillK, Decimal.valueOf(80))) // Signal 1
				.and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2

		return new BaseStrategy(entryRule, exitRule);
	}

	private static Strategy buildStrategyTrendFollowing/* PlusMomentumIndicatorsPlusRenko */(TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		AwesomeOscillatorIndicator awesome = new AwesomeOscillatorIndicator(new MedianPriceIndicator(series));
		AwesomeIndicator ai = new AwesomeIndicator(awesome, series);// returns green or red bars

		// Getting a SMA (e.g. over the 10 last ticks)
		// SMAIndicator sma = new SMAIndicator(new ClosePriceIndicator(series), 10);

		// Getting a EMA (e.g. over the 20 last ticks)
		EMAIndicator ema = new EMAIndicator(closePrice, 20);
		RSIIndicator rsi = new RSIIndicator(closePrice, 14);
		StochasticRSIIndicator srsi = new StochasticRSIIndicator(series, 14);
		Decimal srsiThresholdHigh = Decimal.valueOf(0.9);
		Decimal srsiThresholdLow = Decimal.valueOf(0.25);

		// Using Keltner Channels as the trend following indicator
		KeltnerChannelMiddleIndicator km = new KeltnerChannelMiddleIndicator(new ClosePriceIndicator(series), 20);
		KeltnerChannelLowerIndicator kl = new KeltnerChannelLowerIndicator(km, Decimal.valueOf(1), 20);
		KeltnerChannelUpperIndicator ku = new KeltnerChannelUpperIndicator(km, Decimal.valueOf(1), 20);

		// Getting Bollinger's bands
		SMAIndicator sma = new SMAIndicator(closePrice, 12);
		StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePrice, 12);
		BollingerBandsMiddleIndicator bbmSMA = new BollingerBandsMiddleIndicator(sma);
		BollingerBandsUpperIndicator bbuSMA = new BollingerBandsUpperIndicator(bbmSMA, standardDeviation);
		BollingerBandsLowerIndicator bblSMA = new BollingerBandsLowerIndicator(bbmSMA, standardDeviation);
		BollingerBandWidthIndicator bandwidth = new BollingerBandWidthIndicator(bbuSMA, bbmSMA, bblSMA);

		RenkoIndicator renko = new RenkoIndicator(series);

		MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
		EMAIndicator emaSignal = new EMAIndicator(macd, 9);
		MACDOscillator macdOscillator = new MACDOscillator(closePrice, 12, 26, 9);

		// Getting a short (e.g.5-ticks) SMA
		SMAIndicator shortSma = new SMAIndicator(closePrice, 5);

		// Getting a longer SMA (e.g. over the 30 last ticks)
		SMAIndicator longSma = new SMAIndicator(closePrice, 30);

		// Ok, now let's building our trading rules!
		// Buying rules
		// We want to buy:
		// - if the 5-ticks SMA crosses over 30-ticks SMA

		// When awesome crosses above the Zero Line, short term momentum is now rising
		// faster
		// than the long term momentum
		Rule buyingRuleBasic = (new CrossedUpIndicatorRule(awesome, Decimal.ZERO)
		// .or(new CrossedUpIndicatorRule(shortSma, longSma))
		// .or(new OverIndicatorRule(sma, closePrice))
		).and(new IsRisingRule(closePrice, 3));

		// Selling rules
		// We want to sell:
		// - if the 5-ticks SMA crosses under 30-ticks SMA

		// When AO crosses below the Zero Line,
		// short term momentum is now falling faster then the long term momentum.
		// This can present a bearish selling opportunity.

		Rule sellingRuleBasic = // new CrossedDownIndicatorRule(shortSma, longSma)
				new CrossedDownIndicatorRule(awesome, Decimal.ZERO)
		// .or(new CrossedUpIndicatorRule(shortSma, longSma))
		// .or(new OverIndicatorRule(sma, closePrice))
		// .or(new UnderIndicatorRule(sma, closePrice))
		;

		// Getting the close price of the ticks
		int index = series.getEndIndex();
		// Decimal lastClosePrice = series.getTick(index).getClosePrice();
		// within an indicator:
		// System.out.println("First close price: " + lastClosePrice.toDouble());

		// Here is the same close price:
		// System.out.println(lastClosePrice.isEqual(closePrice.getValue(index))); //
		// equal to firstClosePrice

		/*
		 * String bwdynamic = (bandwidth.getValue(index).toDouble() <
		 * bandwidth.getValue(index - 1).toDouble()) ? " ↓ " : " ↑ ";
		 * System.out.println("K-channels: (L): " + kl.getValue(index).toDouble() +
		 * " (M): " + km.getValue(index).toDouble() + " (U): " +
		 * ku.getValue(index).toDouble()); System.out.println("B-channels: (L): " +
		 * bblSMA.getValue(index).toDouble() + " (M): " +
		 * bbmSMA.getValue(index).toDouble() + " (U): " +
		 * bbuSMA.getValue(index).toDouble() + " B-bandwith: (" + bwdynamic + ") " +
		 * bandwidth.getValue(index).toDouble());
		 */

		// Ok, now let's building our trading rules!

		/**
		 * Buying rules
		 * 
		 */
		// We want to buy:
		// - if the 20-ticks EMA crosses over 20-ticks Keltner's lower indicator
		// (obsolete)- if the 12-ticks SMA crosses over the low bollinger's channel
		// - consider renko + StochRSI > 0.5
		// (obsolete)- or if the price goes below a defined price (e.g 0.0040 BTC)

		Rule buyingRule = buyingRuleBasic.or(
				// buy only while rising prices
				(new CrossedUpIndicatorRule(macd, emaSignal).or(new CrossedUpIndicatorRule(closePrice, kl)) // keltner's
																											// watchdog
						.or(new CrossedUpIndicatorRule(closePrice, bblSMA))// bollinger's watchdog
				// .or(new IsFallingRule(macd, 3))
				// .and(new IsLowestRule(macd, 3))
				// .or(new OverIndicatorRule(srsi, srsiThresholdLow))
				// .or(new UnderIndicatorRule(rsi, Decimal.valueOf(20)))
				));

		// trend was changed to downwards OR still upwards trend
		// buyingRule = buyingRule.and(new OverIndicatorRule(macdOscillator,
		// Decimal.ZERO)).and(new IsFallingRule(macdOscillator, 3))

		// .or(new IsEqualRule(renko, Decimal.ZERO.minus(Decimal.TWO)).or(new
		// IsEqualRule(renko, Decimal.THREE)))

		/**
		 * Selling rules
		 * 
		 */

		// We want to sell:
		// - if the 20-ticks EMA crosses under 20-ticks Keltner's lower indicator
		// - if the 12-ticks SMA crosses under the upper bollinger's channel
		// - consider renko + StochRSI > 0.6
		// - or if if the price looses more than 5%
		// - or if the price earns more than 369%

		//

		if (sellingRulesCatalog.size() == 0) {
			sellingRulesCatalog.put(1, new CrossedDownIndicatorRule(awesome, Decimal.ZERO));
			sellingRulesCatalog.put(2, new CrossedDownIndicatorRule(macd, emaSignal));
			sellingRulesCatalog.put(3, new CrossedDownIndicatorRule(bbuSMA, closePrice));
			sellingRulesCatalog.put(4, new CrossedDownIndicatorRule(ema, ku));
		}

		for (int key = 1; key < sellingRulesCatalog.size(); key++) {
			if (sellingRulesCatalog.get(key).isSatisfied(index)) {
				sellingRulesHistory.put(key, index);
			}
		}

		Rule sellingRule = sellingRuleBasic
				.or((new CrossedDownIndicatorRule(macd, emaSignal).or(new CrossedDownIndicatorRule(bbuSMA, closePrice)) // bollinger's
																														// watchdog
						.or(new CrossedDownIndicatorRule(ema, ku))// keltner's watchdog
						.or(new OverIndicatorRule(rsi, Decimal.valueOf(80)))
						.or(new StopLossRule(closePrice, Decimal.valueOf("0.0036")))));

		/*
		 * Rule r6 = new OverIndicatorRule(rsi, Decimal.valueOf(80));
		 * r6.isSatisfied(index);
		 */

		// .and(new OverIndicatorRule(srsi, srsiThresholdHigh))

		// .or(new CrossedDownIndicatorRule(bbuSMA, closePrice)) // bollinger's watchdog
		// .or(new CrossedDownIndicatorRule(ema, ku)// keltner's watchdog
		// .or(new IsHighestRule(srsi, 6)) // within 6 last ticks
		// .or(new OverIndicatorRule(srsi, srsiThresholdHigh))
		// .or(new StopLossRule(closePrice, Decimal.valueOf("0.0036")))
		// .or(new StopGainRule(closePrice, Decimal.valueOf("369"))))

		// trend was changed to downwards OR still downwards trend
		// sellingRule = sellingRule.and(new UnderIndicatorRule(macdOscillator,
		// Decimal.ZERO)).and(new IsRisingRule(macdOscillator, 3))
		// .or(new IsEqualRule(renko, Decimal.TWO).or(new IsEqualRule(renko,
		// Decimal.ZERO.minus(Decimal.THREE))))

		// Running our juicy trading strategy...
		Strategy buySellSignals = new BaseStrategy(buyingRule, sellingRule, 3);
		return buySellSignals;
	}

	/**
	 * @param series
	 *            a time series
	 * @return a super-puper strategy for crypto
	 */
	private static Strategy buildSuperPuperStrategy(TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

		// Getting the simple moving average (SMA) of the close price over the last 5
		// ticks
		SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
		// Here is the 5-ticks-SMA value at the 42nd index
		// System.out.println("5-ticks-SMA value at the 42nd index: " +
		// shortSma.getValue(42).toDouble());

		// Getting a longer SMA (e.g. over the 30 last ticks)
		SMAIndicator longSma = new SMAIndicator(closePrice, 30);

		// Ok, now let's building our trading rules!

		// Buying rules
		// We want to buy:
		// - if the 5-ticks SMA crosses over 30-ticks SMA
		// - or if the price goes below a defined price (e.g $40.00)
		Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma)
				.or(new CrossedDownIndicatorRule(closePrice, Decimal.valueOf("40")));

		// Selling rules
		// We want to sell:
		// - if the 5-ticks SMA crosses under 30-ticks SMA
		// - or if if the price looses more than 5%
		// - or if the price earns more than 96%
		Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma)
				.or(new StopLossRule(closePrice, Decimal.valueOf("5")))
				.or(new StopGainRule(closePrice, Decimal.valueOf("96")));

		// Running our juicy trading strategy...
		Strategy buySellSignals = new BaseStrategy(buyingRule, sellingRule);
		return buySellSignals;

	}

	private static Strategy buildStrategy(TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

		@SuppressWarnings("static-access")
		// Strategy buySellSignals = new RSI2Strategy().buildStrategy(series);
		// Strategy buySellSignals = new GlobalExtremaStrategy().buildStrategy(series);
		// Strategy buySellSignals = new CCICorrectionStrategy().buildStrategy(series);

		SMAIndicator sma = new SMAIndicator(closePrice, 12);

		// SMAIndicator sma = new SMAIndicator(closePrice, 7);

		// Signals
		// Buy when SMA goes over close price
		// Sell when close price goes over SMA0
		Strategy buySellSignals = new BaseStrategy(new OverIndicatorRule(sma, closePrice),
				new UnderIndicatorRule(sma, closePrice));
		return buySellSignals;
	}

	private static void backtest(int maxtickcount) {
		// Getting a time series (from any provider: CSV, web service, etc.)
		TimeSeries series = SqlTradesLoader.getInstance(TradingBotOnMovingTimeSeries.symbol)
				.loadSeriesFromJournal(Optional.of(maxtickcount));

		// Getting the close price of the ticks
		Decimal firstClosePrice = series.getTick(0).getClosePrice();
		System.out.println("First close price: " + firstClosePrice.toDouble());
		// Or within an indicator:
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		// Here is the same close price:
		System.out.println(firstClosePrice.isEqual(closePrice.getValue(0))); // equal to firstClosePrice

		// Getting the simple moving average (SMA) of the close price over the last 5
		// ticks
		SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
		// Here is the 5-ticks-SMA value at the 42nd index
		System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());

		// Getting a longer SMA (e.g. over the 30 last ticks)
		SMAIndicator longSma = new SMAIndicator(closePrice, 30);

		// Ok, now let's building our trading rules!

		// Buying rules
		// We want to buy:
		// - if the 5-ticks SMA crosses over 30-ticks SMA
		// - or if the price goes below a defined price (e.g $40.00)
		Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma)
				.or(new CrossedDownIndicatorRule(closePrice, Decimal.valueOf("40")));

		// Selling rules
		// We want to sell:
		// - if the 5-ticks SMA crosses under 30-ticks SMA
		// - or if if the price looses more than 3%
		// - or if the price earns more than 2%
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

	private static Decimal walletAmount = Decimal.valueOf(1);// BTC
	private static Decimal walletQuantity = Decimal.valueOf(0);

	private static void run(String symbol, int maxTickCount) {
		TimeSeries series = initializeMovingTimeSeries(symbol);
		series.setMaximumTickCount(maxTickCount);

		CandlestickInterval interval = CandlestickInterval.ONE_MINUTE;
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiRestClient client = factory.newRestClient();
		List<Candlestick> candlestickBars = client.getCandlestickBars(symbol.toUpperCase(), interval);

		long startOpenTimestamp = tradeSessionStartTime;
		long endOpenTimestamp = tradeSessionStartTime;
		for (Candlestick candlestickBar : candlestickBars) {
			if (startOpenTimestamp >= candlestickBar.getOpenTime()) {
				startOpenTimestamp = candlestickBar.getOpenTime();
			}
			if (endOpenTimestamp <= candlestickBar.getOpenTime()) {
				endOpenTimestamp = candlestickBar.getOpenTime();
			}
			candlesticksCache.put(candlestickBar.getOpenTime(), candlestickBar);
		}
		ZonedDateTime startOpenTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startOpenTimestamp),
				ZoneId.systemDefault());
		ZonedDateTime endOpenTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endOpenTimestamp),
				ZoneId.systemDefault());

		List<Tick> newTicks = SqlTradesLoader.buildEmptyTicks(startOpenTime, endOpenTime,
				SqlTradesLoader.getTicksPerMilli());

		for (Candlestick candlestickBar : candlestickBars) {
			ZonedDateTime tradeTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(candlestickBar.getOpenTime()),
					ZoneId.systemDefault());

			for (int i = newTicks.size() - 1; i >= 0; i--) {
				if (newTicks.get(i).inPeriod(tradeTime)) {
					if (newTicks.get(i).getTrades() == 0) {
						newTicks.set(i, new BaseTick(tradeTime, Decimal.valueOf(candlestickBar.getOpen()),
								Decimal.valueOf(candlestickBar.getHigh()), Decimal.valueOf(candlestickBar.getLow()),
								Decimal.valueOf(candlestickBar.getClose()),
								Decimal.valueOf(candlestickBar.getVolume())));
					}
					Decimal amount = Decimal.valueOf(candlestickBar.getVolume())
							.multipliedBy(Decimal.valueOf(candlestickBar.getClose()));
					Decimal price = Decimal.valueOf(candlestickBar.getClose());
					newTicks.get(i).addTrade(amount, price);
					LAST_TICK_CLOSE_PRICE = Decimal.valueOf(candlestickBar.getClose());
					// Store this candlestick to the database
					new CandlestickDumpDb(symbol.toUpperCase()).setInterval(interval).setCandlestick(candlestickBar)
							.run();
				}
			}
		}
		// Removing still empty ticks
		if (newTicks.size() > 0)
			SqlTradesLoader.removeEmptyTicks(newTicks);
		series.setMaximumTickCount(maxTickCount);

		// Building the trading strategy
		/////////////////////////////// Strategy strategy = buildStrategy(series);

		/**
		 * Strategy strategy = buildStrategyTrendFollowing(series); //
		 * buildSuperPuperStrategy(series);
		 */

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
			ZonedDateTime openTimeCandlestick = ZonedDateTime.ofInstant(Instant.ofEpochMilli(openTimestamp),
					ZoneId.systemDefault());

			Tick lastKnownTick = series.getLastTick();
			if (openTimeCandlestick.isAfter(lastKnownTick.getEndTime())) {
				newTick = new BaseTick(openTimeCandlestick, Decimal.valueOf(updateCandlestick.getOpen()),
						Decimal.valueOf(updateCandlestick.getHigh()), Decimal.valueOf(updateCandlestick.getLow()),
						Decimal.valueOf(updateCandlestick.getClose()), Decimal.valueOf(updateCandlestick.getVolume()));
				series.addTick(newTick);
				System.out.println("------------------------------------------------------\n" + "Tick "
						+ series.getTickCount() + " added at " + lastKnownTick.getEndTime() + ", close price = "
						+ newTick.getClosePrice().toDouble());

				LAST_TICK_CLOSE_PRICE = Decimal.valueOf(updateCandlestick.getClose());
			} else if (openTimeCandlestick.isBefore(lastKnownTick.getEndTime())
					&& openTimeCandlestick.isAfter(lastKnownTick.getBeginTime())) {

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
			newTick = series.getLastTick();
			Decimal amountToBePurchased = Decimal.valueOf(40);// Decimal.TEN;
			Decimal amountToBeSold = Decimal.valueOf(40);// Decimal.TEN;

			Double fee = 0.001;// actually the value is 0.0005 at binance.com

			// rebuild strategy (otherwise s = strategy)
			if (strategy.shouldEnter(endIndex)) {

				Strategy s = buildStrategyTrendFollowing(series);

				// Our strategy should enter
				System.out.println("Strategy should ENTER on " + (endIndex + 1));
				boolean entered = tradingRecord.enter(endIndex, newTick.getClosePrice(), amountToBePurchased);
				if (entered) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					Order entry = tradingRecord.getLastEntry();

					walletAmount = walletAmount.minus(
							Decimal.valueOf((1 + fee) * entry.getPrice().toDouble() * entry.getAmount().toDouble()));
					walletQuantity = walletQuantity.minus(Decimal.valueOf((1 + fee) * entry.getAmount().toDouble()));

					System.out.println("Entered on " + entry.getIndex() + " (price=" + entry.getPrice().toDouble()
							+ ", amount=" + entry.getAmount().toDouble() + " Wallet: " + walletAmount.toDouble() + ")");
				}
			} else if (strategy.shouldExit(endIndex)) {
				// Our strategy should exit

				Strategy s = buildStrategyTrendFollowing(series);

				System.out.println("Strategy should EXIT on " + (endIndex + 1));
				boolean exited = tradingRecord.exit(endIndex, newTick.getClosePrice(), amountToBeSold);
				if (exited) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					java.awt.Toolkit.getDefaultToolkit().beep();

					Order exit = tradingRecord.getLastExit();
					walletAmount = walletAmount.plus(
							Decimal.valueOf((1 - fee) * exit.getPrice().toDouble() * exit.getAmount().toDouble()));
					walletQuantity = walletQuantity.plus(Decimal.valueOf((1 - fee) * exit.getAmount().toDouble()));

					System.out.println("Exited on " + exit.getIndex() + " (price=" + exit.getPrice().toDouble()
							+ ", amount=" + exit.getAmount().toDouble() + " Wallet: " + walletAmount.toDouble() + ")");

					// Analysis

					// Getting the cash flow of the resulting trades
					/*
					 * CashFlow cashFlow = new CashFlow(series, tradingRecord); for( int i=0;
					 * i<cashFlow.getSize()-1; i++) { System.out.println("Cashflow on step #: " + i
					 * + " = " + cashFlow.getValue(i)); }
					 */

					// Getting the profitable trades ratio
					AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
					System.out
							.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, tradingRecord));
					// Getting the reward-risk ratio
					AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
					System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, tradingRecord));

					// Total profit of our strategy
					// vs total profit of a buy-and-hold strategy
					AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
					System.out.println(
							"Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, tradingRecord));
				}
			}

		});
	}

	public static void log(AggTrade updateAggTrade) {

		AggTradeDumpDb logger = new AggTradeDumpDb(symbol.toUpperCase());
		logger.setAggtrade(updateAggTrade);
		// synchronous call
		// logger.run();
		// asynchronous call
		Thread thread = new Thread(logger);
		thread.start();

		new DepthCacheExample(symbol);
	}

	public static void main(String[] args) throws InterruptedException {

		// Loading the Logback configuration
		loadLoggerConfiguration();

		System.out.println("********************** Initialization **********************");

		/*
		 * SqlTradesLoader.setTicksPerSecond(1); run("QTUMBTC", 1000);
		 */
		// String instrument = "QTUMBTC";
		String instrument = "XRPBTC";
		TradingBotOnMovingTimeSeries bot = new TradingBotOnMovingTimeSeries();
		bot.initializeAggTradesCache(instrument);
		bot.startAggTradesEventStreaming(instrument, 5000);// max number of ticks

		// backtest(60*60*12);
	}

}
