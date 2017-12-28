package com.binance.api.examples;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;


import com.binance.api.client.domain.market.AggTrade;

/*import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.market.BookTicker;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.TickerPrice;
import com.binance.api.client.domain.market.TickerStatistics;
import com.binance.api.client.exception.BinanceApiException;
*/
public class MarketTickers {

  public static void main(String[] args) {
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
    BinanceApiRestClient client = factory.newRestClient();

/*    
    // Getting depth of a symbol
    OrderBook orderBook = client.getOrderBook("IOTABTC", 10);
    System.out.println(orderBook.getAsks());

    // Getting latest price of a symbol
    TickerStatistics tickerStatistics = client.get24HrPriceStatistics("IOTABTC");
    System.out.println(tickerStatistics);

    // Getting all latest prices
    List<TickerPrice> allPrices = client.getAllPrices();
    System.out.println(allPrices);


    // Weekly candlestick bars for a symbol
    List<Candlestick> candlesticks = client.getCandlestickBars("IOTABTC", CandlestickInterval.WEEKLY);
    System.out.println(candlesticks);*/

    // Getting all book tickers
    Integer counter = 0;
    do {    	
        /*List<BookTicker> allBookTickers = client.getBookTickers();
        System.out.println(allBookTickers);*/

        // Getting agg trades
        List<AggTrade> aggTrades = client.getAggTrades("IOTABTC");
        System.out.println(aggTrades);

        
        
        try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    } while ( ++counter < 300);

    
    /*
    // Exception handling
    try {
      client.getOrderBook("IOTABTC", 10);
    } catch (BinanceApiException e) {
      System.out.println(e.getError().getCode()); // -1121
      System.out.println(e.getError().getMsg());  // Invalid symbol
    }*/
  }
}

