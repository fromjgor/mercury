package com.binance.api.client.mercury;

import org.ta4j.core.*;

import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

/**
 * 2-Period RSI Strategy
 * <p>
 * 
 * @see http://stockcharts.com/school/doku.php?id=chart_school:trading_strategies:rsi2
 */
public class RSI2Strategy {

	/**
	 * @param series
	 *            a time series
	 * @return a 2-period RSI strategy
	 */
	public static Strategy buildStrategy(TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
		SMAIndicator longSma = new SMAIndicator(closePrice, 200);

		// We use a 2-period RSI indicator to identify buying
		// or selling opportunities within the bigger trend.
		RSIIndicator rsi = new RSIIndicator(closePrice, 2);

		// Entry rule
		// The long-term trend is up when a security is above its 200-period SMA.
		Rule entryRule = new OverIndicatorRule(shortSma, longSma) // Trend
				.and(new CrossedDownIndicatorRule(rsi, Decimal.valueOf(5))) // Signal 1
				.and(new OverIndicatorRule(shortSma, closePrice)); // Signal 2

		// Exit rule
		// The long-term trend is down when a security is below its 200-period SMA.
		Rule exitRule = new UnderIndicatorRule(shortSma, longSma) // Trend
				.and(new CrossedUpIndicatorRule(rsi, Decimal.valueOf(95))) // Signal 1
				.and(new UnderIndicatorRule(shortSma, closePrice)); // Signal 2

		// TODO: Finalize the strategy

		return new BaseStrategy(entryRule, exitRule);
	}

	public static void main(String[] args) {
		/*
		 * // Getting the time series //TimeSeries series =
		 * CsvTradesLoader.loadBitstampSeries(); TimeSeries series =
		 * SqlTradesLoader.getInstance("QTUMBTC").loadSeries(Optional.of(4096));
		 * 
		 * // Building the trading strategy Strategy strategy = buildStrategy(series);
		 * 
		 * // Running the strategy TimeSeriesManager seriesManager = new
		 * TimeSeriesManager(series); TradingRecord tradingRecord =
		 * seriesManager.run(strategy);
		 * System.out.println("Number of trades for the strategy: " +
		 * tradingRecord.getTradeCount());
		 * 
		 * // Analysis System.out.println("Total profit for the strategy: " + new
		 * TotalProfitCriterion().calculate(series, tradingRecord));
		 */
	}

}
