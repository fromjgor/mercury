package com.binance.api.examples;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.mercury.*;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

/**
 * Illustrates how to use the klines/candlesticks event stream to create a local
 * cache of bids/asks for a symbol.
 */
public class CandlesticksCacheExample {

	/**
	 * Key is the start/open time of the candle, and the value contains candlestick
	 * date.
	 */
	private Map<Long, Candlestick> candlesticksCache;

	public CandlesticksCacheExample(String symbol, CandlestickInterval interval) {
		initializeCandlestickCache(symbol, interval);
		startCandlestickEventStreaming(symbol, interval);
	}

	/**
	 * Initializes the candlestick cache by using the REST API.
	 */
	private void initializeCandlestickCache(String symbol, CandlestickInterval interval) {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiRestClient client = factory.newRestClient();
		List<Candlestick> candlestickBars = client.getCandlestickBars(symbol.toUpperCase(), interval);

		this.candlesticksCache = new TreeMap<>();
		for (Candlestick candlestickBar : candlestickBars) {
			candlesticksCache.put(candlestickBar.getOpenTime(), candlestickBar);
		}
	}

	/**
	 * Begins streaming of depth events.
	 */
	private void startCandlestickEventStreaming(String symbol, CandlestickInterval interval) {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiWebSocketClient client = factory.newWebSocketClient();

		client.onCandlestickEvent(symbol.toLowerCase(), interval, response -> {

			if (response == null) {
				startCandlestickEventStreaming(symbol, interval);
				return;
			}
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
			CandlestickDumpDb logger = new CandlestickDumpDb(symbol.toUpperCase());
			logger.setCandlestick(updateCandlestick);
			// asynchronous call
			logger.run();

			// Store the updated candlestick in the cache
			candlesticksCache.put(openTime, updateCandlestick);
			System.out.println(updateCandlestick);
		});
	}

	/**
	 * @return a klines/candlestick cache, containing the open/start time of the
	 *         candlestick as the key, and the candlestick data as the value.
	 */
	public Map<Long, Candlestick> getCandlesticksCache() {
		return candlesticksCache;
	}

	public static void main(String[] args) {
		/**
		 * Simulate a rest client activity to keep the connection alive by sending a
		 * ping twice per hour
		 */
		TimerTask connectivityTest = new TimerTask() {

			@Override
			public void run() {
				BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
				BinanceApiRestClient client = factory.newRestClient();

				// Keep connection alive
				client.ping();
			}
		};

		Timer timer = new Timer();

		long period = 1000 * 60 * 60 / 2;// milliseconds_per_second*seconds_per_minute*minutes_per_hour/2
		long delay = period;

		/**
		 * twice per hour
		 * 
		 */
		timer.scheduleAtFixedRate(connectivityTest, delay, period);

		/************************************************************
		 * do the needful
		 ***********************************************************/

		String[] myFavoritesBTC = new String[] { "QTUM"/* ,"NEO", "IOTA", "FUEL", "ETH", "BNB" */ };
		for (String symbol : myFavoritesBTC) {
			String pair = symbol + "BTC";
			new AggTradesCacheExample(pair);
			new DepthCacheExample(pair);
			new CandlesticksCacheExample(pair, CandlestickInterval.ONE_MINUTE);
		}

		// new CandlesticksCacheExample("ETHBTC", CandlestickInterval.ONE_MINUTE);
		// new CandlesticksCacheExample("EOSBTC", CandlestickInterval.ONE_MINUTE);
		// new CandlesticksCacheExample("IOTAETH", CandlestickInterval.ONE_MINUTE);
		// new CandlesticksCacheExample("IOTABTC", CandlestickInterval.ONE_MINUTE);
		// new CandlesticksCacheExample("ETHBTC", CandlestickInterval.ONE_MINUTE);
		// new CandlesticksCacheExample("NEOBTC", CandlestickInterval.ONE_MINUTE);
		// new CandlesticksCacheExample("BTGBTC", CandlestickInterval.ONE_MINUTE);
		// new CandlesticksCacheExample("QTUMBTC", CandlestickInterval.ONE_MINUTE);
		// new CandlesticksCacheExample("NEOBTC",CandlestickInterval.ONE_MINUTE);

	}
}
