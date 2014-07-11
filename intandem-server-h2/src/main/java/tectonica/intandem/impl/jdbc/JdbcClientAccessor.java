package tectonica.intandem.impl.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import tectonica.intandem.framework.Entity;
import tectonica.intandem.framework.PatchableEntity;
import tectonica.intandem.framework.client.ClientAccessor;
import tectonica.intandem.framework.client.ClientChangeType;
import tectonica.intandem.framework.client.ClientSyncEvent;
import tectonica.intandem.framework.server.ServerAccessor;
import tectonica.intandem.framework.server.ServerSyncEvent;
import ch.qos.logback.classic.Logger;

public abstract class JdbcClientAccessor extends JdbcBaseProvider implements ClientAccessor, SqlClient
{
	protected static final Logger LOG = (Logger) LoggerFactory.getLogger(JdbcServerAccessor.class.getSimpleName());

	protected static final int DB_UPDATE_TIME_SAFETY_MS = 50; // must be at least 1

	private void mergeKV(Connection conn, String id, long subId, String type, String value, long ts) throws SQLException
	{
//		System.err.println("updateKV " + ts);
		try (PreparedStatement writeStmt = conn.prepareStatement(KV_MERGE()))
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
		try (PreparedStatement writeStmt = conn.prepareStatement(KV_REPLACE()))
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
		String stmt = (subId == null) ? KV_DELETE_PURGE_ALL() : KV_DELETE_PURGE();
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
				try (PreparedStatement readStmt = conn.prepareStatement(KV_READ_MULTIPLE()))
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
				try (PreparedStatement readStmt = conn.prepareStatement(KV_READ_SINGLE()))
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
				try (PreparedStatement readStmt = conn.prepareStatement(KV_CHECK()))
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
				try (PreparedStatement readStmt = conn.prepareStatement(KV_MAX()))
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
				result.nextSyncStart = System.currentTimeMillis() - DB_UPDATE_TIME_SAFETY_MS;
				if (result.syncStart >= result.nextSyncStart)
					throw new RuntimeException("invalid syncStart"); // TODO: maybe wait a millisec instead?
				result.events = server.sync(userId, syncStart, result.nextSyncStart, getClientChanges(conn));
				for (ServerSyncEvent event : result.events)
					applySync(conn, event);
				resetClientChanges(conn);
				return result;
			}

			private ArrayList<ClientSyncEvent> getClientChanges(Connection conn) throws SQLException
			{
				try (PreparedStatement readStmt = conn.prepareStatement(SYNC_GET_CHANGES()))
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
				try (PreparedStatement readStmt = conn.prepareStatement(SYNC_RESET_CHANGES()))
				{
					readStmt.executeUpdate();
				}
				finally
				{
				}
			}

			private void applySync(Connection conn, ServerSyncEvent syncEntity) throws SQLException
			{
				String id = syncEntity.id;
				long subId = syncEntity.subId;

				switch (syncEntity.changeType)
				{
					case CHANGE:
						mergeKV(conn, id, subId, syncEntity.type, syncEntity.value, System.currentTimeMillis());
						break;

					case DELETE:
						deleteOrPurgeKV(conn, userId, subId, System.currentTimeMillis(), false);
						break;
				}
			}
		});
	}
}
