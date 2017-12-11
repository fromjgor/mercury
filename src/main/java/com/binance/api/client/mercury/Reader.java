package com.binance.api.client.mercury;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
//import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//List<String> sharedReaders = new ArrayList<String>();

public class Reader {

	private static final Map<String, Reader> readerCounts = new ConcurrentHashMap<String, Reader>(64);
	private static final Iterator<String> readerIterator = readerCounts.keySet().iterator();

	private String key;
	private String message;
	private Connection connection = null;
	private Statement statement = null;

	private Reader(String key) {
		if (readerCounts.containsKey(key) != true) {
			readerCounts.put(key, this);
			try {
				// create a database connection
				connection = DriverManager.getConnection("jdbc:sqlite:" + SecuritySettings.DBNAME);
				statement = connection.createStatement();
				statement.setQueryTimeout(30); // set timeout to 30 sec.
			} catch (SQLException e) {
				System.err.println(e.getMessage());
			}
		}

		this.key = key;
	}

	public static Reader getNext() {
		String prefix = "orderbook";
		String key = prefix;
		Integer count = 0;

		while (readerIterator.hasNext()) {
			++count;
			key = readerIterator.next();
			if (key.equals("orderbook3") && count > 3) {
				// wait for a reader to be unlocked
				try {
					Thread.sleep(50);
					// restart
					return getNext();

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {

				key = prefix + count.toString();

			}
		}

		Reader reader = new Reader(key);

		return reader;
	}

	public void close() {
		if (readerCounts.containsKey(this.key) == true) {
			Reader reader = readerCounts.get(this.key);

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

				readerCounts.remove(this.key);

			}
		}

	}

	public void execute(String stmnt) throws SQLException {

		if (stmnt.isEmpty() == true) 
			stmnt = "select "
					+ "price,"
					+ "amount,"
					+ "updatetime as timestamp,"
					+ "quantity,"
					+ "symbol,"
					+ "ordertype,"
					+ " from orderbook "
					+ " where symbol = 'IOTABTC' and IsBest = 'X'"
					+ " order by UpdateTime Asc";	
		ResultSet rs = statement.executeQuery(stmnt);
		while (rs.next()) { // read the result set System.out.println("OpenTime = " +
			rs.getString("opentime"); // System.out.println("id = " + rs.getInt("id"));
		}
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

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

}
