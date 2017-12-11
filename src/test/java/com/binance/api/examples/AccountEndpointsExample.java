package com.binance.api.examples;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.mercury.SecuritySettings;

import java.util.List;

/**
 * Examples on how to get account information.
 */
public class AccountEndpointsExample {

  public static void main(String[] args) {
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(SecuritySettings.sKEY,SecuritySettings.sSECRET);
    BinanceApiRestClient client = factory.newRestClient();

    // Get account balances
    Account account = client.getAccount(6000000L, System.currentTimeMillis());
    System.out.println(account.getBalances());
    System.out.println(account.getAssetBalance("BTC"));

    // Get list of trades
    List<Trade> myTrades = client.getMyTrades("IOTABTC");
    System.out.println(myTrades);

    // Get withdraw history
    System.out.println(client.getWithdrawHistory("BTC"));

    // Get deposit history
    System.out.println(client.getDepositHistory("BTC"));

    // Get deposit address
    System.out.println(client.getDepositAddress("BTC"));

    // Withdraw
//    client.withdraw("ETH", "0x123", "0.1", null);
  }
}
