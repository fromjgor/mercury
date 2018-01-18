package com.binance.api.client.mercury;

import org.ta4j.core.Decimal;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.*;

/**
 * Moving average convergence divergence oscillator (histogram indicator). The
 * MACD Histogram represents the difference between MACD and its 9-day EMA
 * <p>
 */

public class MACDOscillator extends CachedIndicator<Decimal> /* MACDIndicator */ {

	private final MACDIndicator macd;
	private final EMAIndicator signalEma;

	public MACDOscillator(Indicator<Decimal> indicator, int shortTimeFrame, int longTimeFrame, int signalTimeframe) {
		super(indicator);
		if (shortTimeFrame > longTimeFrame) {
			throw new IllegalArgumentException("Long term period count must be greater than short term period count");
		}

		macd = new MACDIndicator(indicator, shortTimeFrame, longTimeFrame);
		signalEma = new EMAIndicator(macd, signalTimeframe);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected Decimal calculate(int index) {
		return macd.getValue(index).minus(signalEma.getValue(index));
	}
}
