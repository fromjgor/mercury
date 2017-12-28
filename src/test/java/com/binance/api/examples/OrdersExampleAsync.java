package com.binance.api.examples;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.request.AllOrdersRequest;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.mercury.*;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.marketBuy;

/**
 * Examples on how to place orders, cancel them, and query account information.
 */
public class OrdersExampleAsync {

  public static void main(String[] args) {
	String myPair = "IOTABTC";
	
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(SecuritySettings.sKEY,SecuritySettings.sSECRET); 
    BinanceApiAsyncRestClient client = factory.newAsyncRestClient();

    // Getting list of open orders
    client.getOpenOrders(new OrderRequest(myPair), response -> System.out.println(response));

    // Get status of a particular order
    client.getOrderStatus(new OrderStatusRequest(myPair, 5677202L),
        response -> System.out.println(response));

    // Getting list of all orders with a limit of 10
    client.getAllOrders(new AllOrdersRequest(myPair).limit(10), response -> System.out.println(response));

    // Canceling an order
    client.cancelOrder(new CancelOrderRequest(myPair, 5677202L),
        response -> System.out.println("Order has been canceled."));

    // Placing a test LIMIT order
    client.newOrderTest(limitBuy(myPair, TimeInForce.GTC, "1000", "0.0001"),
        response -> System.out.println("Test order has succeeded."));

    // Placing a test MARKET order
    client.newOrderTest(marketBuy(myPair, "1000"), response -> System.out.println("Test order has succeeded."));

    // Placing a real LIMIT order
    //client.newOrder(limitBuy(myPair, TimeInForce.GTC, "1000", "0.0001"), response -> System.out.println(response));
  }
}
