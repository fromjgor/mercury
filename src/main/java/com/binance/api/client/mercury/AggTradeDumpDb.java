package com.binance.api.client.mercury;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.NavigableMap;

import com.binance.api.client.domain.market.AggTrade;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AggTradeDumpDb implements Runnable {
	String symbol;
	AggTrade aggTrade;

	Connection connection = null;
	Statement statement = null;

	public AggTradeDumpDb(String name) {
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

	public void run() {

		try {

			synchronized (Locks.WRITERS) {

				Writer writer = Writer.getNext();
				writer.lock();

				insertAggTrade(writer, aggTrade);

				writer.unlock();

			}

			// Thread.sleep(50);
		} catch (InterruptedException e) {
			System.out.println("Thread " + symbol + " interrupted.");
			/*
			 * if ( writer.getMessage().compareTo( ASK ) == 0 ) {
			 * 
			 * }
			 */

		}
	}

	private void insertAggTrade(Writer writer, AggTrade aggTrade) throws InterruptedException {

		/*
		 * Trades parameters: timestamp, price,amount
		 *
		 * private long aggregatedTradeId --> tradeID --> symbol long tradeTime -->
		 * timestamp String price --> price String quantity --> quantity --> amount =
		 * price * quantity boolean isBuyerMaker --> BOOLEAN (X or space) long
		 * firstBreakdownTradeId --> firstBreakdownTradeId long lastBreakdownTradeId -->
		 * lastBreakdownTradeId
		 * 
		 */

		BigDecimal price = new BigDecimal(aggTrade.getPrice());
		BigDecimal quantity = new BigDecimal(aggTrade.getQuantity());
		BigDecimal amount = price.multiply(quantity);

		try {

			writer.executeUpdate("insert into trade values(" + aggTrade.getAggregatedTradeId() + ",'" + symbol + "',"
					+ aggTrade.getTradeTime() + "," + aggTrade.getPrice() + "," + aggTrade.getQuantity() + ","
					+ amount.toString() + ",'" + ((aggTrade.isBuyerMaker() == true) ? "X" : " ") + "',"
					+ aggTrade.getFirstBreakdownTradeId() + "," + aggTrade.getLastBreakdownTradeId() + " )");

		} catch (SQLException e) {
			System.err.println(e.getMessage());
		} finally {
			;
		}
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public AggTrade getAggtrade() {
		return aggTrade;
	}

	public void setAggtrade(AggTrade aggTrade) {
		this.aggTrade = aggTrade;
	}

}
