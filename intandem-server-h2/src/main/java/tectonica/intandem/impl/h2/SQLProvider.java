package tectonica.intandem.impl.h2;

import java.sql.Connection;
import java.sql.SQLException;

import org.h2.jdbcx.JdbcConnectionPool;

public class SQLProvider
{
	protected JdbcConnectionPool connPool;

	protected static interface ConnListener<T>
	{
		T onConnection(final Connection conn) throws SQLException;
	}

	protected <T> T execute(ConnListener<T> connListener)
	{
		Connection conn = null;
		try
		{
			conn = connPool.getConnection();
			try
			{
				return connListener.onConnection(conn);
			}
			finally
			{
				conn.close();
			}
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected <T> T transact(ConnListener<T> connListener)
	{
		Connection conn = null;
		try
		{
			conn = connPool.getConnection();
			try
			{
				conn.setAutoCommit(false);
				T retVal = connListener.onConnection(conn);
				conn.commit();
				return retVal;
			}
			finally
			{
				conn.close();
			}
		}
		catch (SQLException e)
		{
			try
			{
				if (conn != null)
					conn.rollback();
			}
			catch (SQLException e1)
			{
			}
			throw new RuntimeException(e);
		}
	}

}
