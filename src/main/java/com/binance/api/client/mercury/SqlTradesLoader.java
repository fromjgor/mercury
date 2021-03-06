package com.binance.api.client.mercury;

import java.time.LocalDateTime;
import java.sql.Timestamp;

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

	private static int ticksPerSecond = 1;

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
	 *            the tick duration (in milliseconds)
	 * @return the list of empty ticks
	 */
	public static List<Tick> buildEmptyTicks(ZonedDateTime beginTime, ZonedDateTime endTime, int duration) {

		List<Tick> emptyTicks = new ArrayList<>();

		// Duration tickDuration = Duration.ofSeconds(duration);
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
			if (ticks.get(i).getTrades() == 0) {
				ticks.remove(i);
			}
		}
	}

	public TimeSeries loadSeriesFromJournal(Optional<Integer> maxticksOpt) {

		Integer maxticks = maxticksOpt.isPresent() ? maxticksOpt.get() : 0;

		List<Tick> ticks = null;
		ResultSet rs = null;

		try {
			ZonedDateTime beginTime = null;
			ZonedDateTime endTime = null;
			Long beginTimestamp = 0L;
			Long endTimestamp = 0L;

			rs = statement.executeQuery("SELECT min(opentime) AS beginTimestamp, max(opentime) AS endTimestamp"
					+ " FROM journal WHERE pair = '" + symbol + "'");
			rs = statement.executeQuery(
					"SELECT min(opentime) AS beginTimestamp, max(opentime) AS endTimestamp FROM journal WHERE pair = '"
							+ symbol + "'");
			if (rs.next()) {

				beginTimestamp = rs.getLong("beginTimestamp");
				endTimestamp = rs.getLong("endTimestamp");

				beginTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(beginTimestamp), ZoneId.systemDefault());
				endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp), ZoneId.systemDefault());
			}
			rs.close();

			if (maxticks > 0) {
				beginTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp - maxticks / ticksPerSecond),
						ZoneId.systemDefault());
			}

			LocalDateTime beginTimeWithoutTimezone = beginTime.toLocalDateTime();
			Timestamp begTimestamp = Timestamp.valueOf(beginTimeWithoutTimezone);
			beginTimestamp = begTimestamp.getTime();

			// Building the empty ticks every one minute (60 seconds), yeah welcome in the
			// crypto-world)
			ticks = buildEmptyTicks(beginTime, endTime, 1000 / ticksPerSecond);

			// Filling the ticks with candlesticks
			rs = statement.executeQuery("SELECT opentime," + "       open," + "       high," + "       close,"
					+ "       closetime," + "       interval," + "       volume," + "       NumberOfTrades,"
					+ "       QuoteAssetVolume," + "       TakerBuyQuoteAssetVolume,"
					+ "       TakerBaseQuoteAssetVolume" + " FROM journal" + " WHERE pair = '" + symbol
					+ "' AND opentime BETWEEN " + (beginTimestamp - 1) + " AND " + (endTimestamp + 1)
					+ " ORDER BY opentime ASC");

			while (rs.next()) { // read the result set System.out.println("OpenTime = " +
				ZonedDateTime openTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("opentime")),
						ZoneId.systemDefault());

				for (int i = ticks.size() - 1; i >= 0; i--) {
					if (ticks.get(i).inPeriod(openTime)) {
						if (ticks.get(i).getTrades() == 0) {
							ticks.set(i, new BaseTick(openTime, Decimal.valueOf(rs.getDouble("open")),
									Decimal.valueOf(rs.getDouble("high")), Decimal.valueOf(rs.getDouble("low")),
									Decimal.valueOf(rs.getDouble("close")), Decimal.valueOf(rs.getDouble("volume"))));
						}
						ticks.get(i).addTrade(rs.getDouble("volume"), rs.getDouble("close"));
					}
				}
			}
			// Removing still empty ticks
			removeEmptyTicks(ticks);

		} catch (SQLException ioe) {
			Logger.getLogger(SqlTradesLoader.class.getName()).log(Level.SEVERE,
					"Unable to load candlesticks from database", ioe);
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				Logger.getLogger(SqlTradesLoader.class.getName()).log(Level.SEVERE,
						"Unable to load candlesticks from database", e);
			}
		}

		return new BaseTimeSeries("symbol", ticks);

	}

	public TimeSeries loadSeries(Optional<Integer> maxticksOpt) {

		Integer maxticks = maxticksOpt.isPresent() ? maxticksOpt.get() : 0;

		List<Tick> ticks = null;
		ResultSet rs = null;
		Long beginTimeStamp = 0L;
		Long endTimeStamp = 0L;

		try {
			ZonedDateTime beginTime = null;
			ZonedDateTime endTime = null;

			rs = statement.executeQuery(
					"SELECT min(timestamp) AS beginTimestamp, max(timestamp) AS endTimestamp FROM trade WHERE symbol = '"
							+ symbol + "'");
			if (rs.next()) {
				beginTimeStamp = rs.getLong("beginTimestamp");
				endTimeStamp = rs.getLong("endTimestamp");
				beginTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(beginTimeStamp), ZoneId.systemDefault());
				endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeStamp), ZoneId.systemDefault());
			}
			rs.close();

			if (maxticks > 0) {
				beginTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeStamp - maxticks / ticksPerSecond),
						ZoneId.systemDefault());
			}

			// Building the empty ticks every one minute (60 seconds), yeah welcome in the
			// crypto-world)
			ticks = buildEmptyTicks(beginTime, endTime, 1000 / ticksPerSecond);
			// ticks = buildEmptyTicks(beginTime, endTime, 300);

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

		return new BaseTimeSeries(symbol, ticks);
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

	public static int getTicksPerSecond() {
		return ticksPerSecond;
	}

	public static int getTicksPerMilli() {
		return ticksPerSecond * 1000;
	}

	public static void setTicksPerSecond(int tps) {
		ticksPerSecond = tps;
	}

}
