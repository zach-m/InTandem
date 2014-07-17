package tectonica.intandem.impl.jdbc.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sqlite.SQLiteDataSource;

import tectonica.intandem.impl.jdbc.JdbcServerAccessor;

public class SqliteServerAccessor extends JdbcServerAccessor
{
	public SqliteServerAccessor(String connStr)
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
		((SQLiteDataSource) connPool).setUrl(connStr);

		execute(new ConnListener<Void>()
		{
			@Override
			public Void onConnection(Connection conn) throws SQLException
			{
				for (String stmt : KV_INIT())
					conn.createStatement().execute(stmt);
				for (String stmt : SYNC_INIT())
					conn.createStatement().execute(stmt);
				return null;
			}
		});

		LOG.info("Done " + msg);
	}

	@Override
	public void cleanup()
	{}

	@Override
	public List<String> KV_INIT()
	{
		return Arrays.asList( //
				"DROP TABLE IF EXISTS KVDB", //
				"CREATE TABLE KVDB (K VARCHAR2, SK BIGINT, T VARCHAR2, V VARCHAR2, UT BIGINT, D TINYINT, PRIMARY KEY(K, SK))");
		// TODO: add index on the type-name (column T)
	}

	@Override
	public String KV_READ_SINGLE()
	{
		return "SELECT V FROM KVDB WHERE (K = ?) AND (SK = ?) AND (D = 0)";
	}

	@Override
	public String KV_READ_SUB_RANGE()
	{
		return "SELECT V FROM KVDB WHERE (K = ?) AND (SK BETWEEN ? AND ?) AND (D = 0)";
	}

	@Override
	public String KV_READ_ALL_SUBS()
	{
		return "SELECT V FROM KVDB WHERE (K = ?) AND (D = 0)";
	}

	@Override
	public String KV_READ_TYPE()
	{
		return "SELECT V FROM KVDB WHERE (T = ?) AND (D = 0)";
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
	public List<String> SYNC_INIT()
	{
		return Arrays.asList( //
				"DROP TABLE IF EXISTS SYNCDB", //
				"CREATE TABLE SYNCDB (U VARCHAR2, K VARCHAR2, SK BIGINT, SUT BIGINT, SD TINYINT, PRIMARY KEY(U, K, SK))");
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
				"SELECT KVDB.K, KVDB.SK, UT, D, SUT, SD, T, V " + //
				"FROM SYNCDB JOIN KVDB ON (SYNCDB.K = KVDB.K AND (SYNCDB.SK = KVDB.SK OR SYNCDB.SK = " + ALL_SUBS + ")) " + //
				"WHERE (U = ?) AND (" + //
				"(SUT > ? AND SUT <= ?) OR " + //
				"((SUT <= ?) AND (UT > ? AND UT <= ?))" + //
				")";
	}
}