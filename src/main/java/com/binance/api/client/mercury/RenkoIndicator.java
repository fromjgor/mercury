package com.binance.api.client.mercury;

import org.ta4j.core.Decimal;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.*;
import com.binance.api.client.mercury.RenkoSignal;

/**
 * Renko indicator.
 * <p>
 * This calculation of RI uses traditional moving averages as opposed to
 * Wilder's accumulative moving average technique.
 *
 * @see SmoothedRenko Charts
 */
public class RenkoIndicator extends CachedIndicator<Decimal> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final TimeSeries series;
	private RenkoSignal renkoSignal;

	/** Body height */
	// private final Indicator<Decimal> bodyHeightInd;
	/** Average body height */
	// private final SMAIndicator averageBodyHeightInd;
	// private final Decimal factor = Decimal.valueOf(0);

	/**
	 * Constructor.
	 * 
	 * @param series
	 *            a time series
	 */
	public RenkoIndicator(TimeSeries series) {
		super(series);
		this.series = series;
	}

	@Override

	/**
	 * Renko - three consequent ticks are observed to decide on the trend move
	 * direction Similar to Kagi and Point and Figure charting, Renko ignores the
	 * element of time used on candlesticks, bar charts, and line charts. Instead,
	 * Renko focuses on sustained price movement of a preset amount of pips. For
	 * example, a trader can set the bricks for as little as 5 pips or as many as
	 * 100 or more. Each brick represents e.g. 10 pips of price movement. A new
	 * brick will not be formed until price has moved 100 pips. It could take 24
	 * hours for a new brick to form or it could take just a few hours. However, no
	 * bricks will form until the preset limit is achieved. Green colored bricks are
	 * bullish, while red-colored bricks are bearish. Remember that the size of the
	 * brick can be setup when you first go through the steps of creating Renko
	 * chart. Swing traders may use 50 or 100 pip bricks to represent some fraction
	 * of the average daily trading range. While scalpers and day traders may look
	 * at 20, 10 or 5 pip bricks.
	 *
	 * @todo: Trades amounts are not considered when detect trend direction (to
	 *        avoid tricks)
	 * @todo: A new brick will not be formed until price has moved 100 pips
	 */

	protected Decimal calculate(int index) {
		if (index < 3) {
			return Decimal.ZERO;
		}

		// Tick t = series.getTick(index);

		if (series.getTick(index - 1).getClosePrice().isLessThan(series.getTick(index).getClosePrice())
				&& ((series.getTick(index - 1).getTrades() > 1) && (series.getTick(index).getTrades() > 1))
				&& series.getTick(index - 2).getClosePrice().isLessThan(series.getTick(index - 1).getClosePrice())
				&& ((series.getTick(index - 2).getTrades() > 1) && (series.getTick(index - 1).getTrades() > 1))
				&& series.getTick(index - 3).getClosePrice().isLessThan(series.getTick(index - 2).getClosePrice())
				&& ((series.getTick(index - 3).getTrades() > 1) && (series.getTick(index - 2).getTrades() > 1))) {
			// no trend change at upwards trend - next green renko brick can be made
			renkoSignal = RenkoSignal.STILL_UPWARDS;
			return Decimal.THREE;
		} else if (series.getTick(index - 1).getClosePrice().isGreaterThan(series.getTick(index).getClosePrice())
				&& ((series.getTick(index - 1).getTrades() > 1) && (series.getTick(index).getTrades() > 1))
				&& series.getTick(index - 2).getClosePrice().isGreaterThan(series.getTick(index - 1).getClosePrice())
				&& ((series.getTick(index - 2).getTrades() > 1) && (series.getTick(index - 1).getTrades() > 1))
				&& series.getTick(index - 3).getClosePrice().isGreaterThan(series.getTick(index - 2).getClosePrice())
				&& ((series.getTick(index - 3).getTrades() > 1) && (series.getTick(index - 2).getTrades() > 1))) {
			// no trend change at downward trend - next red renko brick can be made
			renkoSignal = RenkoSignal.STILL_DOWNWARDS;
			return Decimal.ZERO.minus(Decimal.THREE);
		} else if (series.getTick(index - 3).getClosePrice().isLessThan(series.getTick(index - 2).getClosePrice())
				&& ((series.getTick(index - 3).getTrades() > 1) && (series.getTick(index - 2).getTrades() > 1))
				&& series.getTick(index - 2).getClosePrice().isGreaterThan(series.getTick(index - 1).getClosePrice())
				&& ((series.getTick(index - 2).getTrades() > 1) && (series.getTick(index - 1).getTrades() > 1))
				&& series.getTick(index - 1).getClosePrice().isGreaterThan(series.getTick(index).getClosePrice())
				&& ((series.getTick(index - 1).getTrades() > 1) && (series.getTick(index).getTrades() > 1))) {
			// upwards trend seems to be changed to a downward: one up - two down
			renkoSignal = RenkoSignal.UPWARDS_DOWNWARDS;
			return Decimal.ZERO.minus(Decimal.TWO);
		} else if (series.getTick(index - 3).getClosePrice().isGreaterThan(series.getTick(index - 2).getClosePrice())
				&& ((series.getTick(index - 3).getTrades() > 1) && (series.getTick(index - 2).getTrades() > 1))
				&& series.getTick(index - 2).getClosePrice().isLessThan(series.getTick(index - 1).getClosePrice())
				&& ((series.getTick(index - 2).getTrades() > 1) && (series.getTick(index - 1).getTrades() > 1))
				&& series.getTick(index - 1).getClosePrice().isLessThan(series.getTick(index).getClosePrice())
				&& ((series.getTick(index - 1).getTrades() > 1) && (series.getTick(index).getTrades() > 1))) {
			// downwards trend seems to be changed to a upwards : one down - two up
			renkoSignal = RenkoSignal.DOWNWARDS_UPWARDS;
			return Decimal.TWO;
		} else // Nominal case neutral
			renkoSignal = RenkoSignal.SIDEWARDS;
		return Decimal.ONE;
	}

	public RenkoSignal getRenkoSignal() {
		return renkoSignal;
	}

	public void setRenkoSignal(RenkoSignal renkoSignal) {
		this.renkoSignal = renkoSignal;
	}
}
