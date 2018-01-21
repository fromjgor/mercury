package com.binance.api.client.mercury;

public enum AwesomeSignal {

	NONE("none"), GREEN("green"), RED("red");

	private final String signalId;

	AwesomeSignal(String signalId) {
		this.signalId = signalId;
	}

	public String getSignalId() {
		return signalId;
	}
}
