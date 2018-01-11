package com.binance.api.client.mercury;

import java.sql.Connection;
import java.util.Optional;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ta4j.core.BaseTick;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.Decimal;
import org.ta4j.core.Tick;
import org.ta4j.core.TimeSeries;

public class SqlTradesLoader {

	private static final Map<String, SqlTradesLoader> readerCounts = new ConcurrentHashMap<String, SqlTradesLoader>(64);

	private String symbol;
	private Connection connection = null;
	private Statement statement = null;

	private SqlTradesLoader(String symbol) {
		this.symbol = symbol;

		if (readerCounts.containsKey(symbol) != true) {
			readerCounts.put(symbol, this);
			try {
				// create a database connection
				connection = DriverManager.getConnection("jdbc:sqlite:" + SecuritySettings.DBNAME);
				statement = connection.createStatement();
				statement.setQueryTimeout(30); // set timeout to 30 sec.
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	public static SqlTradesLoader getInstance(String symbol) {

		SqlTradesLoader reader = new SqlTradesLoader(symbol);

		return reader;
	}

	public void close() {
		if (readerCounts.containsKey(this.symbol) == true) {
			SqlTradesLoader reader = readerCounts.get(this.symbol);

			// Destroy reader here (close database connection, free buffers,...)

			try {
				Connection connection = reader.getConnection();
				if (reader.getConnection() != null) {
					connection.close();
					connection = null;
					reader.setConnection(connection);
				}
			} catch (SQLException e) {
				// connection close failed.
				System.err.println(e);
			} finally {

				readerCounts.remove(this.symbol);

			}
		}

	}

	/**
	 * Builds a list of empty ticks.
	 * 
	 * @param beginTime
	 *            the begin time of the whole period
	 * @param endTime
	 *            the end time of the whole period
	 * @param duration
	 *            the tick duration (in seconds)
	 * @return the list of empty ticks
	 */
	public static List<Tick> buildEmptyTicks(ZonedDateTime beginTime, ZonedDateTime endTime, int duration) {

		List<Tick> emptyTicks = new ArrayList<>();

		Duration tickDuration = Duration.ofSeconds(duration);
		ZonedDateTime tickEndTime = beginTime;
		do {
			tickEndTime = tickEndTime.plus(tickDuration);
			emptyTicks.add(new BaseTick(tickDuration, tickEndTime));
		} while (tickEndTime.isBefore(endTime));

		return emptyTicks;
	}

	/**
	 * Removes all empty (i.e. with no trade) ticks of the list.
	 * 
	 * @param ticks
	 *            a list of ticks
	 */
	public static void removeEmptyTicks(List<Tick> ticks) {
		for (int i = ticks.size() - 1; i >= 0; i--) {
			if (ticks.get(i).getTrades() == 0 /*|| 
				ticks.get(i).getClosePrice() == Decimal.valueOf(0) || 
				ticks.get(i).getVolume() == Decimal.valueOf(0)*/
				) {
				ticks.remove(i);
			}
		}
	}

	public TimeSeries loadSeries(Optional<Integer> maxticksOpt) {
		Integer ticksizeinseconds = 60;
		Integer maxticks = maxticksOpt.isPresent() ? maxticksOpt.get() : 0;

		List<Tick> ticks = null;
		ResultSet rs = null;

		try {
			ZonedDateTime beginTime = null;
			ZonedDateTime endTime = null;

			rs = statement.executeQuery(
					"SELECT min(timestamp) AS beginTimestamp, max(timestamp) AS endTimestamp FROM trade WHERE symbol = '"
							+ symbol + "'");
			if (rs.next()) {
				beginTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("beginTimestamp")),
						ZoneId.systemDefault());
				endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("endTimestamp")),
						ZoneId.systemDefault());
			}
			rs.close();
			
			if ( maxticks > 0 ) {
				beginTime = endTime.minusSeconds(ticksizeinseconds*maxticks); 
			}

			// Building the empty ticks every one minute (60 seconds), yeah welcome in the crypto-world)
			ticks = buildEmptyTicks(beginTime, endTime, ticksizeinseconds);
			//ticks = buildEmptyTicks(beginTime, endTime, 300);

			// Filling the ticks with trades
			rs = statement.executeQuery(
					"SELECT timestamp,price,amount FROM trade WHERE symbol = '" + symbol + "' ORDER BY timestamp ASC");
			while (rs.next()) { // read the result set System.out.println("OpenTime = " +
				ZonedDateTime tradeTimestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("timestamp")),
						ZoneId.systemDefault());
				for (Tick tick : ticks) {
					if (tick.inPeriod(tradeTimestamp)) {
						tick.addTrade(rs.getDouble("amount"), rs.getDouble("price"));
					}
				}
			}
			// Removing still empty ticks
			removeEmptyTicks(ticks);

		} catch (SQLException ioe) {
			Logger.getLogger(SqlTradesLoader.class.getName()).log(Level.SEVERE, "Unable to load trades from database",
					ioe);
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				Logger.getLogger(SqlTradesLoader.class.getName()).log(Level.SEVERE,
						"Unable to load trades from database", e);
			}
		}

		return new BaseTimeSeries("binance trades IOTA - BTC", ticks);
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public Statement getStatement() {
		return statement;
	}

	public void setStatement(Statement statement) {
		this.statement = statement;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

}
