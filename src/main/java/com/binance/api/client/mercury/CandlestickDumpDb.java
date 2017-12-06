package com.binance.api.client.mercury;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.binance.api.client.domain.market.Candlestick;

public class CandlestickDumpDb {
	String symbol;

	public CandlestickDumpDb(String symbol) {
		this.symbol = symbol;
	}

	public void insert(Candlestick event) {
		Connection connection = null;
		try {
			// create a database connection
			connection = DriverManager.getConnection("jdbc:sqlite:" + SecuritySettings.DBNAME);

			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			String sqlSymbol = "'" + symbol + "'";
			statement.executeUpdate("insert into journal values(" + sqlSymbol + "," + event.getOpenTime() + ","
					+ event.getOpen() + "," + event.getLow() + "," + event.getHigh() + "," + event.getClose() + ","
					+ event.getCloseTime() + "," + event.getVolume() + "," + event.getNumberOfTrades() + ","
					+ event.getQuoteAssetVolume() + "," + event.getTakerBuyQuoteAssetVolume() + ","
					+ event.getTakerBuyQuoteAssetVolume() +
					// "," + " '" + symbol +"' "
					" )");

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
