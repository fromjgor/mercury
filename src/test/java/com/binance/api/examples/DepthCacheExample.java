package com.binance.api.examples;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.binance.api.client.mercury.DeptCacheDumpDb;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Illustrates how to use the depth event stream to create a local cache of
 * bids/asks for a symbol.
 */
public class DepthCacheExample {

	private static final String BIDS = "BIDS";
	private static final String ASKS = "ASKS";

	private long lastUpdateId;

	private Map<String, NavigableMap<BigDecimal, BigDecimal>> depthCache;

	public DepthCacheExample(String symbol) {
		startDepthEventStreaming(symbol, initializeDepthCache(symbol));
	}

	/**
	 * Initializes the depth cache by using the REST API.
	 */
	private OrderBook initializeDepthCache(String symbol) {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiRestClient client = factory.newRestClient();
		OrderBook orderBook = client.getOrderBook(symbol.toUpperCase(), 10);

		this.depthCache = new HashMap<>();
		this.lastUpdateId = orderBook.getLastUpdateId();

		NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>(Comparator.reverseOrder());
		for (OrderBookEntry ask : orderBook.getAsks()) {
			asks.put(new BigDecimal(ask.getPrice()), new BigDecimal(ask.getQty()));
		}
		depthCache.put(ASKS, asks);

		NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
		for (OrderBookEntry bid : orderBook.getBids()) {
			bids.put(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()));
		}
		depthCache.put(BIDS, bids);
		return orderBook;
	}

	/**
	 * Begins streaming of depth events.
	 */
	private void startDepthEventStreaming(String symbol, OrderBook orderBook) {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
		BinanceApiWebSocketClient client = factory.newWebSocketClient();

		client.onDepthEvent(symbol.toLowerCase(), response -> {
			if (response.getUpdateId() > lastUpdateId) {
				System.out.println(response);
				lastUpdateId = response.getUpdateId();
				updateOrderBook(getAsks(), response.getAsks());
				updateOrderBook(getBids(), response.getBids());
//				printDepthCache();
				storeDepthCache(response.getSymbol(), response.getUpdateId(), response.getEventTime());// log order book
																										// to the
																										// database
			}
		});
	}

	/**
	 * Updates an order book (bids or asks) with a delta received from the server.
	 *
	 * Whenever the qty specified is ZERO, it means the price should was removed
	 * from the order book.
	 */
	private void updateOrderBook(NavigableMap<BigDecimal, BigDecimal> lastOrderBookEntries,
			List<OrderBookEntry> orderBookDeltas) {
		for (OrderBookEntry orderBookDelta : orderBookDeltas) {
			BigDecimal price = new BigDecimal(orderBookDelta.getPrice());
			BigDecimal qty = new BigDecimal(orderBookDelta.getQty());
			if (qty.compareTo(BigDecimal.ZERO) == 0) {
				// qty=0 means remove this level
				lastOrderBookEntries.remove(price);
			} else {
				lastOrderBookEntries.put(price, qty);
			}
		}
	}

	public NavigableMap<BigDecimal, BigDecimal> getAsks() {
		return depthCache.get(ASKS);
	}

	public NavigableMap<BigDecimal, BigDecimal> getBids() {
		return depthCache.get(BIDS);
	}

	/**
	 * @return the best ask in the order book
	 */
	private Map.Entry<BigDecimal, BigDecimal> getBestAsk() {
		return getAsks().lastEntry();
	}

	/**
	 * @return the best bid in the order book
	 */
	private Map.Entry<BigDecimal, BigDecimal> getBestBid() {
		return getBids().firstEntry();
	}

	/**
	 * @return a depth cache, containing two keys (ASKs and BIDs), and for each, an
	 *         ordered list of book entries.
	 */
	public Map<String, NavigableMap<BigDecimal, BigDecimal>> getDepthCache() {
		return depthCache;
	}

	/**
	 * Prints the cached order book / depth of a symbol as well as the best ask and
	 * bid price in the book.
	 */
	private void printDepthCache() {
		System.out.println(depthCache);
		System.out.println("ASKS:");
		getAsks().entrySet().forEach(entry -> System.out.println(toDepthCacheEntryString(entry)));
		System.out.println("BIDS:");
		getBids().entrySet().forEach(entry -> System.out.println(toDepthCacheEntryString(entry)));
		System.out.println("BEST ASK: " + toDepthCacheEntryString(getBestAsk()));
		System.out.println("BEST BID: " + toDepthCacheEntryString(getBestBid()));
	}

	private void storeDepthCache(String symbol, long updateId, long eventTime) {

		BigDecimal priceLimit = new BigDecimal( 0.00 );
		BigDecimal priceLossLimit = new BigDecimal( 0.00 );
		BigDecimal bestAskPrice = getBestAsk().getKey();  
		BigDecimal bestBidPrice = getBestBid().getKey();
		
		if( symbol.compareTo("QTUMBTC") == 0) {
			priceLimit = new BigDecimal( 0.0040 );
			priceLossLimit = new BigDecimal( 0.003);
	
			
		}/* else if( symbol.compareTo("NEOBTC") == 0) {
			priceLimit = new BigDecimal( 0.0049 );
			priceLossLimit = new BigDecimal( 0.0048 );
			
		}*/ else if( symbol.compareTo("FUELBTC") == 0) {
		priceLimit = new BigDecimal( 0.00029311 );
		priceLossLimit = new BigDecimal( 0.000028936 );
		
	}

		if( priceLimit.compareTo( priceLossLimit ) > 0 ) 	
			{
			if ( bestAskPrice.compareTo(priceLimit) > 0 ||   
					 bestBidPrice.compareTo(priceLossLimit) < 0 ) {
				System.out.println(" bestAskPrice | priceLimit | priceLossLimit ");
				System.out.println(bestAskPrice);
				System.out.println(priceLimit);
				System.out.println(priceLossLimit);

		    	java.awt.Toolkit.getDefaultToolkit().beep();			
				}
			/*
			else {
					String sTestPair = "IOTABTC";
				
				    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(SecuritySettings.sKEY,SecuritySettings.sSECRET);
				    BinanceApiRestClient client = factory.newRestClient();

				    // Getting list of open orders
				    List<Order> openOrders = client.getOpenOrders(new OrderRequest(sTestPair));
				    System.out.println(openOrders);

				    // Getting list of all orders with a limit of 10 (-> get top 10)
				    List<Order> allOrders = client.getAllOrders(new AllOrdersRequest(sTestPair).limit(10));
				    System.out.println(allOrders);

				    // Get status of a particular order
				    //Order order = client.getOrderStatus(new OrderStatusRequest(sTestPair, 1986860L)); // replace 1986860L by your order-id 
				    //System.out.println(order);


				    // Placing a test LIMIT order
				    //client.newOrderTest(new NewOrder(sTestPair, OrderSide.SELL, OrderType.LIMIT, TimeInForce.GTC, "10", "1.768"));
				    // Placing a test MARKET order
				    //client.newOrderTest(marketBuy(sTestPair, "1000"));

				// Placing a real LIMIT order
				    client.newOrder(new NewOrder(sTestPair, OrderSide.SELL, OrderType.LIMIT, TimeInForce.GTC, "9", "0.0001"));

				    
				//   NewOrderResponse newOrderResponse = client.newOrder(limitBuy(sTestPair, TimeInForce.GTC, "2", "0.01768"));
				//	 System.out.println(newOrderResponse);				
			}*/
		}

		/* The official music of Dot Net Perls.
	    for (int i = 37; i <= 32767; i += 200)
	    {	        console.Beep(i, 100); //using System;
	    }*/		
		
		DeptCacheDumpDb logger = new DeptCacheDumpDb(symbol.toUpperCase());
		logger.setAsks(getAsks());
		logger.setBids(getBids());
		logger.setUpdateId(updateId);
		logger.setEventTime(eventTime);
// asynchronous call
		logger.run();
	}

	/**
	 * Pretty prints an order book entry in the format "price / quantity".
	 */
	private static String toDepthCacheEntryString(Map.Entry<BigDecimal, BigDecimal> depthCacheEntry) {
		return depthCacheEntry.getKey().toPlainString() + " / " + depthCacheEntry.getValue();
	}

	public static void main(String[] args) {
		/*new DepthCacheExample("ETHBTC");
		new DepthCacheExample("IOTAETH");*/
		new DepthCacheExample("IOTABTC");
	}
}
