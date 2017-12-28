package com.binance.api.examples;
import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.TickerPrice;
import com.binance.api.client.domain.market.TickerStatistics;
import com.binance.api.client.exception.BinanceApiException;
import com.binance.api.client.mercury.*;

import java.util.List;

/**
 * Examples on how to get market data information such as the latest price of a symbol, etc., in an async way.
 */
public class MarketDataEndpointsExampleAsync {

  public static void main(String[] args) {
	String myPair = "IOTABTC";  
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
    BinanceApiAsyncRestClient client = factory.newAsyncRestClient();

    // Getting depth of a symbol (async)
    client.getOrderBook(myPair, 10, (OrderBook response) -> {
      System.out.println(response.getBids());
    });

    // Getting latest price of a symbol (async)
    client.get24HrPriceStatistics(myPair, (TickerStatistics response) -> {
      System.out.println(response);
    });

    // Getting all latest prices (async)
    client.getAllPrices((List<TickerPrice> response) -> {
      System.out.println(response);
    });

    // Getting agg trades (async)
    client.getAggTrades(myPair, (List<AggTrade> response) -> System.out.println(response));

    // Weekly candlestick bars for a symbol
    client.getCandlestickBars(myPair, CandlestickInterval.WEEKLY,
        (List<Candlestick> response) -> System.out.println(response));

    // Book tickers (async)
    client.getBookTickers(response -> System.out.println(response));

    // Exception handling
    try {
      client.getOrderBook(myPair, 10, response -> System.out.println(response));
    } catch (BinanceApiException e) {
      System.out.println(e.getError().getCode()); // -1121
      System.out.println(e.getError().getMsg());  // Invalid symbol
    }
  }
}
