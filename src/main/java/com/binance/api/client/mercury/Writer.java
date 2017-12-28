/**
 * 
 */
package com.binance.api.client.mercury;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
//import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//List<String> sharedWriters = new ArrayList<String>();

public class Writer {

	private static final Map<String, Writer> writerCounts = new ConcurrentHashMap<String, Writer>(64);
	private static final Iterator<String> writerIterator = writerCounts.keySet().iterator();

	private String key;
	private String message;
	private Connection connection = null;
	private Statement statement = null;
	
	private Writer(String key) {
		if ( writerCounts.containsKey(key) != true ) {
			writerCounts.put(key, this);
		}	
		
		this.key = key;
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

	public void lock() {
		try {
			// create a database connection
			connection = DriverManager.getConnection("jdbc:sqlite:" + SecuritySettings.DBNAME);
			statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void executeUpdate(String stmnt) throws SQLException {
		statement.executeUpdate(stmnt);	
	}
	
	public void unlock() {
		if (writerCounts.containsKey(this.key) == true) {
			Writer writer = writerCounts.get(this.key);

			// Destroy writer here (close database connection, free buffers,...)

			try {
				Connection connection = writer.getConnection();
				if (writer.getConnection() != null) {
					connection.close();
					connection = null;
					writer.setConnection(connection);
				}
			} catch (SQLException e) {
				// connection close failed.
				System.err.println(e);
			} finally {

				writerCounts.remove(this.key);

			}
		}

	}

	public static Writer getNext() {
		String prefix = "orderbook";
		String key = prefix;
		Integer count = 0;

		while (writerIterator.hasNext()) {
			++count;
			key = writerIterator.next();
			if (key.equals("orderbook3") && count > 3) {
				// wait for a writer to be unlocked
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

		Writer writer = new Writer(key);

		return writer;
	}
}
