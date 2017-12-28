package com.binance.api.examples;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.AllOrdersRequest;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.exception.BinanceApiException;
import com.binance.api.client.mercury.SecuritySettings;

import java.util.List;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.marketBuy;

/**
 * Examples on how to place orders, cancel them, and query account information.
 */
public class OrdersExample {

  public static void main(String[] args) {
	String sTestPair = "BTGETH";
	
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(SecuritySettings.sKEY,SecuritySettings.sSECRET);
    BinanceApiRestClient client = factory.newRestClient();

    // Getting list of open orders
    List<Order> openOrders = client.getOpenOrders(new OrderRequest(sTestPair));
    System.out.println(openOrders);

    // Getting list of all orders with a limit of 10 (-> get top 10)
    List<Order> allOrders = client.getAllOrders(new AllOrdersRequest(sTestPair).limit(10));
    System.out.println(allOrders);

    // Get status of a particular order
    Order order = client.getOrderStatus(new OrderStatusRequest(sTestPair, 1986860L)); // replace 1986860L by your order-id 
    System.out.println(order);

    // Canceling an order
    try {
      client.cancelOrder(new CancelOrderRequest(sTestPair, 1986860L));
    } catch (BinanceApiException e) {
      System.out.println(e.getError().getMsg());
    }

    // Placing a test LIMIT order
    client.newOrderTest(limitBuy(sTestPair, TimeInForce.GTC, "2", "0.01768"));

    // Placing a test MARKET order
    client.newOrderTest(marketBuy(sTestPair, "1000"));

// Placing a real LIMIT order

    
//   NewOrderResponse newOrderResponse = client.newOrder(limitBuy(sTestPair, TimeInForce.GTC, "2", "0.01768"));
//    System.out.println(newOrderResponse);
  }

}
