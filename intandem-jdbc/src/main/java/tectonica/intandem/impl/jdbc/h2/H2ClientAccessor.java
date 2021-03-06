package tectonica.intandem.impl.jdbc.h2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.h2.jdbcx.JdbcConnectionPool;

import tectonica.intandem.impl.jdbc.JdbcClientAccessor;

public class H2ClientAccessor extends JdbcClientAccessor
{
	public H2ClientAccessor(String connStr, String username, String password)
	{
		String msg = "Initializing " + this.getClass().getSimpleName();
		LOG.info(msg);

		connPool = JdbcConnectionPool.create(connStr, username, password);
		execute(new ConnListener<Void>()
		{
			@Override
			public Void onConnection(Connection conn) throws SQLException
			{
				for (String stmt : KV_INIT())
					conn.createStatement().execute(stmt);
				return null;
			}
		});

		LOG.info("Done " + msg);
	}

	@Override
	public void cleanup()
	{
//		connPool.dispose();
	}

	@Override
	public List<String> KV_INIT()
	{
		return Arrays.asList( //
				"DROP TABLE IF EXISTS KVDB", //
				"CREATE TABLE KVDB (K VARCHAR2, SK BIGINT, T VARCHAR2, V VARCHAR2, UT BIGINT, C TINYINT, PRIMARY KEY(K, SK))");
		// TODO: add index on the type-name (column T)
	}

	@Override
	public String KV_READ_SINGLE()
	{
		return "SELECT V FROM KVDB WHERE (K = ?) AND (SK = ?) AND (C >= 0)";
	}

	@Override
	public String KV_READ_SUB_RANGE()
	{
		return "SELECT V FROM KVDB WHERE (K = ?) AND (SK BETWEEN ? AND ?) AND (C >= 0)";
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
		return "MERGE INTO KVDB KEY (K, SK) VALUES (?, ?, ?, ?, ?, ?)";
	}

	@Override
	public String KV_REPLACE()
	{
		return "UPDATE KVDB SET T = ?, V = ?, UT = ?, C = ? WHERE (K = ?) AND (SK = ?) AND (C >= 0)";
	}

	@Override
	public String KV_CHECK()
	{
		return "SELECT 1 FROM KVDB WHERE (K = ?) AND (SK = ?) AND (C >= 0)";
	}

	@Override
	public String KV_DELETE_PURGE()
	{
		return "UPDATE KVDB SET UT = ?, C = ? WHERE (K = ?) AND (SK = ?)";
	}

	@Override
	public String KV_DELETE_PURGE_ALL()
	{
		return "UPDATE KVDB SET UT = ?, C = ? WHERE (K = ?)";
	}

	@Override
	public String KV_MAX()
	{
		return "SELECT MAX(SK) FROM KVDB WHERE (K = ?)";
	}

	@Override
	public String SYNC_GET_CHANGES()
	{
		return "SELECT K, SK, T, V, C FROM KVDB WHERE (C <> 0)";
	}

	@Override
	public String SYNC_RESET_CHANGES()
	{
		return "UPDATE KVDB SET C = 0 WHERE (C <> 0)";
	}
}
