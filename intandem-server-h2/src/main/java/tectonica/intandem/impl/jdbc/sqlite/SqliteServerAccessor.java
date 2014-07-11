package tectonica.intandem.impl.jdbc.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

import org.sqlite.SQLiteDataSource;

import tectonica.intandem.impl.jdbc.JdbcServerAccessor;

public class SqliteServerAccessor extends JdbcServerAccessor
{
	public SqliteServerAccessor()
	{
		String msg = "Initializing " + this.getClass().getSimpleName();
		LOG.info(msg);

		try
		{
			Class.forName("org.sqlite.JDBC");
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		connPool = new SQLiteDataSource();
		String dbPath = this.getClass().getResource("/").getPath() + "server.db";
		((SQLiteDataSource) connPool).setUrl("jdbc:sqlite:" + dbPath);

		execute(new ConnListener<Void>()
		{
			@Override
			public Void onConnection(Connection conn) throws SQLException
			{
				conn.createStatement().execute(KV_DROP());
				conn.createStatement().execute(SYNC_DROP());
				conn.createStatement().execute(KV_INIT());
				conn.createStatement().execute(SYNC_INIT());
				return null;
			}
		});

		LOG.info("Done " + msg);
	}

	@Override
	public void cleanup()
	{}

	@Override
	public String KV_INIT()
	{
		return "CREATE TABLE KVDB (K VARCHAR2, SK BIGINT, T VARCHAR2, V VARCHAR2, UT BIGINT, D TINYINT, PRIMARY KEY(K, SK))";
	}

	@Override
	public String KV_DROP()
	{
		return "DROP TABLE IF EXISTS KVDB";
	}

	@Override
	public String KV_READ_SINGLE()
	{
		return "SELECT V FROM KVDB WHERE (K = ?) AND (SK = ?) AND (D = 0)";
	}

	@Override
	public String KV_READ_MULTIPLE()
	{
		return "SELECT V FROM KVDB WHERE (K = ?) AND (SK BETWEEN ? AND ?) AND (D = 0)";
	}

	@Override
	public String KV_MERGE()
	{
		return "REPLACE INTO KVDB VALUES (?, ?, ?, ?, ?, 0)";
	}

	@Override
	public String KV_REPLACE()
	{
		return "UPDATE KVDB SET T = ?, V = ?, UT = ? WHERE (K = ?) AND (SK = ?) AND (D = 0)";
	}

	@Override
	public String KV_CHECK()
	{
		return "SELECT 1 FROM KVDB WHERE (K = ?) AND (SK = ?) AND (D = 0)";
	}

	@Override
	public String KV_DELETE()
	{
		return "UPDATE KVDB SET UT = ?, D = 1 WHERE (K = ?) AND (SK = ?) AND (D = 0)";
	}

	@Override
	public String KV_DELETE_ALL()
	{
		return "UPDATE KVDB SET UT = ?, D = 1 WHERE (K = ?) AND (D = 0)";
	}

	@Override
	public String KV_MAX()
	{
		return "SELECT MAX(SK) FROM KVDB WHERE (K = ?)";
	}

	@Override
	public String SYNC_INIT()
	{
		return "CREATE TABLE SYNCDB (U VARCHAR2, K VARCHAR2, SK BIGINT, SUT BIGINT, SD TINYINT, PRIMARY KEY(U, K, SK))";
	}

	@Override
	public String SYNC_DROP()
	{
		return "DROP TABLE IF EXISTS SYNCDB";
	}

	@Override
	public String SYNC_ASSOC()
	{
		return "REPLACE INTO SYNCDB VALUES (?, ?, ?, ?, 0)";
	}

	@Override
	public String SYNC_DISAS()
	{
		return "UPDATE SYNCDB SET SUT = ?, SD = 1 WHERE (U = ?) AND (K = ?) AND (SK = ?) AND (SD = 0)";
	}

	@Override
	public String SYNC_GET_CHANGES()
	{
		return "" + //
				"SELECT SYNCDB.K, SYNCDB.SK, UT, D, SUT, SD, T, V " + //
				"FROM SYNCDB JOIN KVDB ON (SYNCDB.K = KVDB.K AND (SYNCDB.SK = KVDB.SK OR SYNCDB.SK = " + ALL_SUBS + ")) " + //
				"WHERE (U = ?) AND (" + //
				"(SUT > ? AND SUT <= ?) OR " + //
				"((SUT <= ?) AND (UT > ? AND UT <= ?))" + //
				")";
	}
}