package com.binance.api.client.mercury;

public enum RenkoSignal {

	SIDEWARDS("sidewards"), STILL_UPWARDS("still_upwards"), STILL_DOWNWARDS("still_downwards"), UPWARDS_DOWNWARDS(
			"upwards_downwards"), DOWNWARDS_UPWARDS("downwards_upwards");

	private final String signalId;

	RenkoSignal(String signalId) {
		this.signalId = signalId;
	}

	public String getSignalId() {
		return signalId;
	}

}
