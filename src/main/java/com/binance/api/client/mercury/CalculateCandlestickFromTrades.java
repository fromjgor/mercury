package com.binance.api.client.mercury;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateCandlestickFromTrades {

	/**
	 * Illustrates how to use the aggTrades event stream to create a local cache of
	 * candle sticks for a symbol and arbitrary time frame. Unfortunately the
	 * standard API doesn't support the arbitrary time frame that are less than 1
	 * minute. Therefore we cannot use standard klines/candlesticks event stream to
	 * create a local cache of bids/asks for a symbol.
	 */

	/**
	 * Key is the start/open time of the candle, and the value contains candlestick
	 * date.
	 */
	private Map<Long, Candlestick> candlesticksCache;
	private int interval = 1000;// in milliseconds
	private long tradeSessionStartTime;

	/**
	 * Key is the aggregate trade id, and the value contains the aggregated trade
	 * data, which is automatically updated whenever a new agg data stream event
	 * arrives.
	 */
	private Map<Long, AggTrade> aggTradesCache;

	/**
	 * @param symbol
	 *            - a trade instrument, e.g. "QTUMBTC"
	 * @param interval
	 *            - a time frame in milliseconds for which candlesticks are
	 *            calculated from real time trades
	 * @return an Candlesticks cache, containing start/open time of the candle, and
	 *         the value contains candlestick date.
	 * 
	 */
	public CalculateCandlestickFromTrades(String symbol, int interval) {
		this.setInterval(interval);
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

		// Check server time
		tradeSessionStartTime = client.getServerTime();
		long startTimestamp = tradeSessionStartTime;
		long endTimestamp = tradeSessionStartTime;

		this.aggTradesCache = new HashMap<>();
		for (AggTrade aggTrade : aggTrades) {

			Long currentTimestamp = aggTrade.getTradeTime();
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
		 * 
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
			updateAggTrade.setAggregatedTradeId(aggregatedTradeId);
			updateAggTrade.setPrice(response.getPrice());
			updateAggTrade.setQuantity(response.getQuantity());
			updateAggTrade.setFirstBreakdownTradeId(response.getFirstBreakdownTradeId());
			updateAggTrade.setLastBreakdownTradeId(response.getLastBreakdownTradeId());
			updateAggTrade.setBuyerMaker(response.isBuyerMaker());

			// Store the updated agg trade in the cache
			aggTradesCache.put(aggregatedTradeId, updateAggTrade);
			System.out.println(updateAggTrade);
		});
	}

	/**
	 * @return an aggTrades cache, containing the aggregated trade id as the key,
	 *         and the agg trade data as the value.
	 */
	public Map<Long, AggTrade> getAggTradesCache() {
		return aggTradesCache;
	}

	public static void main(String[] args) {
		new CalculateCandlestickFromTrades("ETHBTC", 500);
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}
}
