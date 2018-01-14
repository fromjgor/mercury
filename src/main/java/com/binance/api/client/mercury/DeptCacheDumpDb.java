package com.binance.api.client.mercury;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.NavigableMap;

public class DeptCacheDumpDb // extends Thread
		implements Runnable {
	String symbol;

	long updateId;
	long eventTime;
	NavigableMap<BigDecimal, BigDecimal> asks;
	NavigableMap<BigDecimal, BigDecimal> bids;
	
	private static final String BID = "BID";
	private static final String ASK = "ASK";
	
	Connection connection = null;
	Statement statement = null;

	public DeptCacheDumpDb(String name) {
		// super(name); // if the instance is extended Thread
		this.symbol = name;

		try {
			// create a database connection
			connection = DriverManager.getConnection("jdbc:sqlite:" + SecuritySettings.DBNAME);
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	public void setUpdateId(long updateId) {
		this.updateId = updateId;
	}

	public void setEventTime(long eventTime) {
		this.eventTime = eventTime;
	}

	public void setAsks(NavigableMap<BigDecimal, BigDecimal> asks) {
		this.asks = asks;
	}

	public void setBids(NavigableMap<BigDecimal, BigDecimal> bids) {
		this.bids = bids;
	}

	public void run() {

		try {

			synchronized (Locks.WRITERS) {

				Writer writer = Writer.getNext();
				writer.lock();

				writer.setMessage(ASK);
				insertOrderBookSection(writer, ASK, updateId, eventTime, asks);
				
				writer.setMessage(BID);
				insertOrderBookSection(writer, BID, updateId, eventTime, bids);

				writer.unlock();
			}

			// Thread.sleep(50);
		} catch (InterruptedException e) {
			System.out.println("Thread " + symbol + " interrupted.");
		}
	}

	private void insertOrderBookSection(Writer writer, String type, long updateId, long eventTime,
			NavigableMap<BigDecimal, BigDecimal> ticks) throws InterruptedException {

		BigDecimal bestPrice = (type.compareTo("BID") == 0) ? ticks.firstEntry().getKey() : 		
			(type.compareTo("ASK") == 0) ? ticks.lastEntry().getKey() : new BigDecimal(0);
			
		BigDecimal bestQuantity = (type.compareTo("BID") == 0) ? ticks.firstEntry().getValue()
				: (type.compareTo("ASK") == 0) ? ticks.lastEntry().getValue() : new BigDecimal(0);
				
		ticks.entrySet().forEach(entry -> {

			String sQty = entry.getValue().toString();
			String sPrice = entry.getKey().toString();
			BigDecimal price = new BigDecimal(sPrice);
			BigDecimal quantity = new BigDecimal(sQty);
			BigDecimal amount = (new BigDecimal(sPrice)).multiply(new BigDecimal(sQty));

			String sAmount = amount.toPlainString();
			String bestIndicator = "'"
					+ ((price.compareTo(bestPrice) != 0) && (quantity.compareTo(bestQuantity) != 0) ? "" : "X") + "'";
			try {
				
				writer.executeUpdate("insert into orderbook values('" + symbol + "'," + 
						updateId + "," + eventTime + ",'"+ type +"'," + bestIndicator + "," + sPrice + "," + sQty + "," + sAmount + " )");
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			} finally {
				;
			}
		});
	}
}