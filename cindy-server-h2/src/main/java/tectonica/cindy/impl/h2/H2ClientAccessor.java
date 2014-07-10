package tectonica.cindy.impl.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.LoggerFactory;

import tectonica.cindy.framework.Entity;
import tectonica.cindy.framework.PatchableEntity;
import tectonica.cindy.framework.client.ClientAccessor;
import tectonica.cindy.framework.client.ClientChangeType;
import tectonica.cindy.framework.client.ClientSyncEvent;
import tectonica.cindy.framework.server.ServerAccessor;
import ch.qos.logback.classic.Logger;

public class H2ClientAccessor extends SQLProvider implements ClientAccessor
{
	private static final String CONN_STR = "jdbc:h2:mem:client";
	private static final String CONN_USERNAME = "sa";
	private static final String CONN_PASSWORD = "sa";

	// //////////////////////////////////////////////////////////////////////////////////////
	//
	// Key-Value Modeling ("KV")
	//
	// //////////////////////////////////////////////////////////////////////////////////////
	private static final String KV_INIT = "CREATE TABLE KVDB (K VARCHAR2, SK BIGINT, T VARCHAR2, V VARCHAR2, UT BIGINT, C TINYINT, PRIMARY KEY(K, SK))";
	private static final String KV_READ_SINGLE = "SELECT V FROM KVDB WHERE (K = ?) AND (SK = ?) AND (C >= 0)";
	private static final String KV_READ_MULTIPLE = "SELECT V FROM KVDB WHERE (K = ?) AND (SK BETWEEN ? AND ?) AND (C >= 0)";
	private static final String KV_MERGE = "MERGE INTO KVDB KEY (K, SK) VALUES (?, ?, ?, ?, ?, ?)";
	private static final String KV_REPLACE = "UPDATE KVDB SET T = ?, V = ?, UT = ?, C = ? WHERE (K = ?) AND (SK = ?) AND (C >= 0)";
	private static final String KV_CHECK = "SELECT 1 FROM KVDB WHERE (K = ?) AND (SK = ?) AND (C >= 0)";
	private static final String KV_DELETE_PURGE = "UPDATE KVDB SET UT = ?, C = ? WHERE (K = ?) AND (SK = ?)";
	private static final String KV_DELETE_PURGE_ALL = "UPDATE KVDB SET UT = ?, C = ? WHERE (K = ?)";
	private static final String KV_MAX = "SELECT MAX(SK) FROM KVDB WHERE (K = ?)";

	// //////////////////////////////////////////////////////////////////////////////////////
	//
	// User-Key Modeling ("SYNC")
	//
	// //////////////////////////////////////////////////////////////////////////////////////
	private static final String KV_GET_CHANGES = "SELECT K, SK, T, V, C FROM KVDB WHERE (C <> 0)";
	private static final String KV_RESET_CHANGES = "UPDATE KVDB SET C = 0 WHERE (C <> 0)";

	private static Logger LOG = (Logger) LoggerFactory.getLogger(H2ServerAccessor.class.getSimpleName());

	public H2ClientAccessor()
	{
		this(CONN_STR, CONN_USERNAME, CONN_PASSWORD);
	}

	public H2ClientAccessor(String connStr, String username, String password)
	{
		String msg = "Initializing " + H2ServerAccessor.class.getSimpleName();
		LOG.info(msg);

		connPool = JdbcConnectionPool.create(connStr, username, password);
		execute(new ConnListener<Void>()
		{
			@Override
			public Void onConnection(Connection conn) throws SQLException
			{
				conn.createStatement().execute(KV_INIT);
				return null;
			}
		});

		LOG.info("Done " + msg);
	}

	@Override
	public void cleanup()
	{
		connPool.dispose();
	}

	private void mergeKV(Connection conn, String id, long subId, String type, String value, long ts) throws SQLException
	{
//		System.err.println("updateKV " + ts);
		try (PreparedStatement writeStmt = conn.prepareStatement(KV_MERGE))
		{
			writeStmt.setString(1, id);
			writeStmt.setLong(2, subId);
			writeStmt.setString(3, type);
			writeStmt.setString(4, value);
			writeStmt.setLong(5, ts);
			writeStmt.setByte(6, ClientChangeType.CHANGE.code);
			writeStmt.executeUpdate();
		}
		finally
		{
		}
	}

	private <T extends Entity> boolean replaceKV(Connection conn, T entity) throws SQLException
	{
		try (PreparedStatement writeStmt = conn.prepareStatement(KV_REPLACE))
		{
			writeStmt.setString(1, entity.getType());
			writeStmt.setString(2, entityToStr(entity));
			writeStmt.setLong(3, System.currentTimeMillis());
			writeStmt.setByte(4, ClientChangeType.REPLACE.code);
			writeStmt.setString(5, entity.getId());
			writeStmt.setLong(6, entity.getSubId());
			int count = writeStmt.executeUpdate();
			return (count > 0);
		}
		finally
		{
		}
	}

	private int deleteOrPurgeKV(Connection conn, String id, Long subId, long ts, boolean purge) throws SQLException
	{
//		System.err.println("deleteKV " + ts);
		String stmt = (subId == null) ? KV_DELETE_PURGE_ALL : KV_DELETE_PURGE;
		try (PreparedStatement writeStmt = conn.prepareStatement(stmt))
		{
			writeStmt.setLong(1, ts);
			writeStmt.setByte(2, purge ? ClientChangeType.PURGE.code : ClientChangeType.DELETE.code);
			writeStmt.setString(3, id);
			if (subId != null)
				writeStmt.setLong(4, subId);
			int count = writeStmt.executeUpdate();
			return count;
		}
		finally
		{
		}
	}

	@Override
	public <T extends Entity> List<T> get(final String id, final long subIdFrom, final long subIdTo, final Class<T> clz)
	{
		return execute(new ConnListener<List<T>>()
		{
			@Override
			public List<T> onConnection(Connection conn) throws SQLException
			{
				try (PreparedStatement readStmt = conn.prepareStatement(KV_READ_MULTIPLE))
				{
					readStmt.setString(1, id);
					readStmt.setLong(2, subIdFrom);
					readStmt.setLong(3, subIdTo);
					ResultSet rs = readStmt.executeQuery();
					List<T> list = new ArrayList<>();
					while (rs.next())
						list.add(strToEntity(rs.getString(1), clz));
					return list;
				}
				finally
				{
				}
			}
		});
	}

	@Override
	public <T extends Entity> void put(final T entity)
	{
		execute(new ConnListener<Void>()
		{
			@Override
			public Void onConnection(Connection conn) throws SQLException
			{
				mergeKV(conn, entity.getId(), entity.getSubId(), entity.getType(), entityToStr(entity), System.currentTimeMillis());
				return null;
			}
		});
	}

	@Override
	public <T extends Entity> boolean replace(final T entity)
	{
		return execute(new ConnListener<Boolean>()
		{
			@Override
			public Boolean onConnection(Connection conn) throws SQLException
			{
				return Boolean.valueOf(replaceKV(conn, entity));
			}
		});
	}

	@Override
	public <T extends Entity> boolean patch(final T partialEntity, final Class<T> clz)
	{
		return transact(new ConnListener<Boolean>()
		{
			@Override
			public Boolean onConnection(Connection conn) throws SQLException
			{
				try (PreparedStatement readStmt = conn.prepareStatement(KV_READ_SINGLE))
				{
					readStmt.setString(1, partialEntity.getId());
					readStmt.setLong(2, partialEntity.getSubId());
					ResultSet rs = readStmt.executeQuery();
					if (!rs.next())
						return Boolean.FALSE;

					// if the following casting fails, it means that patching was tried on entity that doesn't support patching
					PatchableEntity entity = (PatchableEntity) strToEntity(rs.getString(1), clz);
					entity.patchWith(partialEntity);
					return Boolean.valueOf(replaceKV(conn, entity)); // theoretically should always be TRUE
				}
				finally
				{
				}
			}
		});
	}

	@Override
	public boolean exists(final String id, final long subId)
	{
		return execute(new ConnListener<Boolean>()
		{
			@Override
			public Boolean onConnection(Connection conn) throws SQLException
			{
				try (PreparedStatement readStmt = conn.prepareStatement(KV_CHECK))
				{
					readStmt.setString(1, id);
					readStmt.setLong(2, subId);
					ResultSet rs = readStmt.executeQuery();
					return (rs.next());
				}
				finally
				{
				}
			}
		});
	}

	@Override
	public int delete(final String id, final long subId)
	{
		return execute(new ConnListener<Integer>()
		{
			@Override
			public Integer onConnection(Connection conn) throws SQLException
			{
				return Integer.valueOf(deleteOrPurgeKV(conn, id, subId, System.currentTimeMillis(), false));
			}
		});
	}

	@Override
	public int delete(final String id)
	{
		return execute(new ConnListener<Integer>()
		{
			@Override
			public Integer onConnection(Connection conn) throws SQLException
			{
				return Integer.valueOf(deleteOrPurgeKV(conn, id, null, System.currentTimeMillis(), false));
			}
		});
	}

	@Override
	public int purge(final String id, final long subId)
	{
		return execute(new ConnListener<Integer>()
		{
			@Override
			public Integer onConnection(Connection conn) throws SQLException
			{
				return Integer.valueOf(deleteOrPurgeKV(conn, id, subId, System.currentTimeMillis(), true));
			}
		});
	}

	@Override
	public long getMaxSubId(final String id)
	{
		return execute(new ConnListener<Long>()
		{
			@Override
			public Long onConnection(Connection conn) throws SQLException
			{
				try (PreparedStatement readStmt = conn.prepareStatement(KV_MAX))
				{
					readStmt.setString(1, id);
					ResultSet rs = readStmt.executeQuery();
					if (rs.next())
						return rs.getLong(1);
					return 0L;
				}
				finally
				{
				}
			}
		});
	}

	private <T extends Entity> T strToEntity(String entityAsJson, final Class<T> clz)
	{
		return JSON.fromJson(entityAsJson, clz);
	}

	private <T extends Entity> String entityToStr(final T entity)
	{
		return JSON.toJson(entity);
	}

	@Override
	public SyncResult sync(final ServerAccessor server, final String userId, final long syncStart)
	{
		return transact(new ConnListener<SyncResult>()
		{
			@Override
			public SyncResult onConnection(Connection conn) throws SQLException
			{
				SyncResult result = new SyncResult();
				result.syncStart = syncStart;
				result.nextSyncStart = System.currentTimeMillis();
				if (result.syncStart >= result.nextSyncStart)
					throw new RuntimeException("invalid syncStart"); // TODO: maybe wait a millisec instead?
				List<ClientSyncEvent> clientSEs = getClientChanges(conn);
				result.events = server.sync(userId, syncStart, result.nextSyncStart, clientSEs);
				resetClientChanges(conn);
				return result;
			}

			private ArrayList<ClientSyncEvent> getClientChanges(Connection conn) throws SQLException
			{
				try (PreparedStatement readStmt = conn.prepareStatement(KV_GET_CHANGES))
				{
					ResultSet rs = readStmt.executeQuery();
					ArrayList<ClientSyncEvent> list = new ArrayList<ClientSyncEvent>();
					while (rs.next())
					{
						String id = rs.getString(1);
						long subId = rs.getLong(2);
						String type = rs.getString(3);
						String value = rs.getString(4);
						ClientChangeType changeType = ClientChangeType.fromCode(rs.getByte(5));
						list.add(ClientSyncEvent.create(id, subId, type, value, changeType));
					}
					return list;
				}
				finally
				{
				}
			}

			private void resetClientChanges(Connection conn) throws SQLException
			{
				try (PreparedStatement readStmt = conn.prepareStatement(KV_RESET_CHANGES))
				{
					readStmt.executeUpdate();
				}
				finally
				{
				}
			}
		});
	}
}
