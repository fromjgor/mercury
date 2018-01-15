package com.binance.api.client.mercury;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;

public class SyncBinanceServerTimeWithLocalTime {

	private static final String APIKEY = SecuritySettings.sKEY;
	private static final String APISECRET = SecuritySettings.sSECRET;

	public SyncBinanceServerTimeWithLocalTime() {
	}

	public static void main(String[] args) {

		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(APIKEY, APISECRET);
		BinanceApiRestClient client = factory.newRestClient();

		while (true) {
			// Checking time between
			Long clientTime = java.lang.System.currentTimeMillis();
			Long serverTimerTime = client.getServerTime();
			System.out.println("Times : client[" + clientTime + "] server[" + serverTimerTime + "] diff["
					+ (serverTimerTime - clientTime) + "]");
		}
	}

}
