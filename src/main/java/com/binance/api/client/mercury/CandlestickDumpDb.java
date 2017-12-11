package com.binance.api.client.mercury;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.OrderBookEntry;

public class CandlestickDumpDb // extends Thread
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

	public CandlestickDumpDb(String name) {
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
			/*if ( writer.getMessage().compareTo( ASK ) == 0 ) {
				
			}*/
			
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
	
	/*private void insertOrderBook(Writer writer, long updateId, long eventTime,
			NavigableMap<BigDecimal, BigDecimal> asks, NavigableMap<BigDecimal, BigDecimal> bids)
			throws InterruptedException {		
		insertOrderBookSection(writer, "ASK", updateId, eventTime, asks);
		insertOrderBookSection(writer, "BID", updateId, eventTime, bids);
	}*/

	/*private void insertOrderBookOld(Writer writer, long updateId, long eventTime,
			NavigableMap<BigDecimal, BigDecimal> asks, NavigableMap<BigDecimal, BigDecimal> bids)
			throws InterruptedException {
		try {
			// create a database connection
			connection = DriverManager.getConnection("jdbc:sqlite:" + SecuritySettings.DBNAME);
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			String sqlSymbol = "'" + symbol + "'";

			BigDecimal bestBidPrice = bids.lastEntry().getKey();
			BigDecimal bestBidPQuantity = bids.lastEntry().getValue();

			bids.entrySet().forEach(entry -> {
				String sQty = entry.getValue().toString();
				String sPrice = entry.getKey().toString();
				BigDecimal price = new BigDecimal(sPrice);
				BigDecimal quantity = new BigDecimal(sQty);
				BigDecimal amount = (new BigDecimal(sPrice)).multiply(new BigDecimal(sQty));

				String sAmount = amount.toPlainString();
				String bestBidIndicator = "'"
						+ ((price.compareTo(bestBidPrice) != 0) && (quantity.compareTo(bestBidPQuantity) != 0) ? ""
								: "X")
						+ "'";
				try {
					statement.executeUpdate(
							"insert into orderbook values(" + sqlSymbol + "," + updateId + "," + eventTime + ", 'BID',"
									+ bestBidIndicator + "," + sPrice + "," + sQty + "," + sAmount + " )");
				} catch (SQLException e) {
					System.err.println(e.getMessage());
				}
			});

			BigDecimal bestAskPrice = asks.lastEntry().getKey();
			BigDecimal bestAskQuantity = asks.lastEntry().getValue();

			asks.entrySet().forEach(entry -> {
				String sQty = entry.getValue().toString();
				String sPrice = entry.getKey().toString();
				BigDecimal price = new BigDecimal(sPrice);
				BigDecimal quantity = new BigDecimal(sQty);
				BigDecimal amount = (new BigDecimal(sPrice)).multiply(new BigDecimal(sQty));

				String sAmount = amount.toPlainString();
				String bestAskIndicator = "'"
						+ ((price.compareTo(bestAskPrice) != 0) && (quantity.compareTo(bestAskQuantity) != 0) ? ""
								: "X")
						+ "'";
				try {
					statement.executeUpdate(
							"insert into orderbook values(" + sqlSymbol + "," + updateId + "," + eventTime + ", 'ASK',"
									+ bestAskIndicator + "," + sPrice + "," + sQty + "," + sAmount + " )");
				} catch (SQLException e) {
					System.err.println(e.getMessage());
				}
			});

		} catch (SQLException e) {
			// connection close failed.
			System.err.println(e);
		}

		finally {
			try {
				if (connection != null)
					connection.close();
			} catch (SQLException e) {
				// connection close failed.
				System.err.println(e);
			}
		}
	}
*/
	public void insert(Candlestick event) {
		try {
			// create a database connection
			if (connection != null) {
				connection = DriverManager.getConnection("jdbc:sqlite:" + SecuritySettings.DBNAME);

				Statement statement = connection.createStatement();
				statement.setQueryTimeout(30); // set timeout to 30 sec.
			}

			String sqlSymbol = "'" + symbol + "'";
			statement.executeUpdate("insert into journal values(" + sqlSymbol + "," + event.getOpenTime() + ","
					+ event.getOpen() + "," + event.getLow() + "," + event.getHigh() + "," + event.getClose() + ","
					+ event.getCloseTime() + "," + event.getVolume() + "," + event.getNumberOfTrades() + ","
					+ event.getQuoteAssetVolume() + "," + event.getTakerBuyQuoteAssetVolume() + ","
					+ event.getTakerBuyQuoteAssetVolume() + " )");

			/*
			 * ResultSet rs = statement.executeQuery("select * from candlestick");
			 * while(rs.next()) { // read the result set System.out.println("OpenTime = " +
			 * rs.getString("opentime")); //System.out.println("id = " + rs.getInt("id")); }
			 */
	
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			System.err.println(e.getMessage());
		} finally {
			try {
				if (connection != null)
					connection.close();
			} catch (SQLException e) {
				// connection close failed.
				System.err.println(e);
			}
		}
	}
}