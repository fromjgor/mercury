package com.binance.api.client.mercury;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.SQLException;
import java.sql.Statement;

import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

public class CandlestickDumpDb // extends Thread
		implements Runnable {
	String symbol;
	CandlestickInterval interval = CandlestickInterval.ONE_MINUTE;
	Candlestick candlestick;
	
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

	public CandlestickDumpDb setCandlestick(Candlestick candlestick) {
		this.candlestick = candlestick;
		return this;
	}

	public void run() {

		try {
			synchronized (Locks.WRITERS) {
				Writer writer = Writer.getNext();
				writer.lock();
				insertCandlestick(writer, candlestick); 
				writer.unlock();
			}

			// Thread.sleep(50);
		} catch (InterruptedException e) {
			System.out.println("Thread " + symbol + " interrupted.");
		}
	}

	
	private void insertCandlestick (Writer writer, Candlestick event) throws InterruptedException {
		String sqlStmnt = "insert into journal values('" + symbol + "'," + event.getOpenTime() + ","
		+ event.getOpen() + "," + event.getLow() + "," + event.getHigh() + "," + event.getClose() + ","
		+ event.getCloseTime() + ",'"+ interval + "',"+ event.getVolume() + "," + event.getNumberOfTrades() + ","
		+ event.getQuoteAssetVolume() + "," + event.getTakerBuyQuoteAssetVolume() + ","
		+ event.getTakerBuyQuoteAssetVolume() + " )";

		try {
			writer.setMessage("journal start");
			writer.executeUpdate(sqlStmnt);			
			writer.setMessage("journal end");
				
		} catch (SQLException e) {
		// if the error message is "out of memory",
		// it probably means no database file is found
		System.err.println(e.getMessage());
	} finally {
	}
		
}
	
	/*public void insert(Candlestick event) {
		try {
			Writer writer = Writer.getNext();
			String sqlStmnt = "insert into journal values('" + symbol + "'," + event.getOpenTime() + ","
			+ event.getOpen() + "," + event.getLow() + "," + event.getHigh() + "," + event.getClose() + ","
			+ event.getCloseTime() + ",'"+ interval + "',"+ event.getVolume() + "," + event.getNumberOfTrades() + ","
			+ event.getQuoteAssetVolume() + "," + event.getTakerBuyQuoteAssetVolume() + ","
			+ event.getTakerBuyQuoteAssetVolume() + " )";
			
			writer.lock();
			writer.setMessage("journal start");
			writer.executeUpdate(sqlStmnt);			
			writer.setMessage("journal end");
			writer.unlock();		

	
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			System.err.println(e.getMessage());
		} finally {

		}
	}*/

	public CandlestickInterval  getInterval() {
		return interval;
	}

	public CandlestickDumpDb setInterval(CandlestickInterval interval) {
		this.interval = interval;
		return this; 
	}
}