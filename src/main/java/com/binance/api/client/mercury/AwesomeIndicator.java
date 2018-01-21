package com.binance.api.client.mercury;

import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.mercury.AwesomeSignal;

import java.util.HashMap;
import java.util.Map;
import java.lang.Object;

import org.ta4j.core.Decimal;
import org.ta4j.core.Indicator;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.helpers.MedianPriceIndicator;
import org.ta4j.core.indicators.AwesomeOscillatorIndicator;

public class AwesomeIndicator extends AwesomeOscillatorIndicator {

	/**
	 * 
	 */
	private static Map<AwesomeSignal, Decimal> signalMap = new HashMap<>();

	private AwesomeOscillatorIndicator awesome;
	private static final long serialVersionUID = 1L;

	public AwesomeIndicator(Indicator<Decimal> indicator, TimeSeries series) {
		super(indicator);
		awesome = new AwesomeOscillatorIndicator(new MedianPriceIndicator(series));

		signalMap.put(AwesomeSignal.NONE, Decimal.ZERO);
		signalMap.put(AwesomeSignal.GREEN, Decimal.ONE);
		signalMap.put(AwesomeSignal.RED, Decimal.ZERO.minus(Decimal.ONE));
	}

	@Override
	protected Decimal calculate(int index) {
		Decimal dif = awesome.getValue(index).minus(awesome.getValue(index - 1));
		if (dif.compareTo(Decimal.ZERO) == 0)
			return signalMap.get(AwesomeSignal.NONE); // Decimal.ZERO for neutral bar;
		else
			return dif.compareTo(Decimal.ZERO) > 0 ? signalMap.get(AwesomeSignal.GREEN) : // Decimal.ONE for green bar
					signalMap.get(AwesomeSignal.RED);// Decimal.ZERO.minus(Decimal.ONE) for red bar
	}

	public static Decimal getSignal(AwesomeSignal signal) {
		return signalMap.get(signal);
	}
}
