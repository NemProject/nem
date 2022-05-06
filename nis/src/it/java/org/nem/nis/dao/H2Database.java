package org.nem.nis.dao;

import org.nem.core.utils.ExceptionUtils;

import java.sql.*;

@SuppressWarnings("try")
public class H2Database implements AutoCloseable {
	private static final String JDBC_DRIVER = "org.h2.Driver";
	private final Connection connection;

	public H2Database(final String dbName) {
		this.connection = ExceptionUtils.propagate(() -> {
			Class.forName(JDBC_DRIVER);
			return DriverManager.getConnection(this.getDbPath(dbName), "", "");
		});
	}

	public Connection getConnection() {
		return this.connection;
	}

	private String getDbPath(final String dbName) {
		return String.format("jdbc:h2:%s/nem/nis/data/%s", System.getProperty("user.home"), dbName);
	}

	@Override
	public void close() throws Exception {
		if (null != this.connection) {
			this.connection.close();
		}
	}

	public void enableAutoCommit(final boolean enable) {
		try {
			connection.setAutoCommit(enable);
		} catch (SQLException e) {
			throw new RuntimeException("could not enable/disable auto commit.");
		}
	}

	public void commit() {
		try {
			connection.commit();
		} catch (SQLException e) {
			throw new RuntimeException("could not commit.");
		}
	}

	public void rollback() {
		// TODO 20151124 J-B: not used
		try {
			connection.rollback();
		} catch (SQLException e) {
			throw new RuntimeException("could not rollback.");
		}
	}

	public ResultSet executeQuery(final String sql) {
		try {
			final Statement stmt = this.connection.createStatement();
			return stmt.executeQuery(sql);
		} catch (final SQLException e) {
			throw new RuntimeException("h2 query failed.");
		}
	}

	public boolean execute(final String sql) {
		try {
			final Statement stmt = this.connection.createStatement();
			return stmt.execute(sql);
		} catch (final SQLException e) {
			throw new RuntimeException(String.format("h2 execute statement failed, reason: %s", e.getMessage()));
		}
	}
}
