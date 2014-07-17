package tectonica.intandem.impl.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import tectonica.intandem.framework.AbstractSyncEvent;
import tectonica.intandem.framework.Entity;
import tectonica.intandem.framework.PatchableEntity;
import tectonica.intandem.framework.client.ClientSyncEvent;
import tectonica.intandem.framework.server.ServerAccessor;
import tectonica.intandem.framework.server.ServerChangeType;
import tectonica.intandem.framework.server.ServerSyncEvent;
import ch.qos.logback.classic.Logger;

public abstract class JdbcServerAccessor extends JdbcBaseProvider implements ServerAccessor, SqlServer
{
	protected static final Logger LOG = (Logger) LoggerFactory.getLogger(JdbcServerAccessor.class.getSimpleName());

	protected static final long ALL_SUBS = -999999;

	private void mergeKV(Connection conn, String id, long subId, String type, String value, long ts) throws SQLException
	{
//		System.err.println("updateKV " + ts);
		PreparedStatement writeStmt = conn.prepareStatement(KV_MERGE());
		writeStmt.setString(1, id);
		writeStmt.setLong(2, subId);
		writeStmt.setString(3, type);
		writeStmt.setString(4, value);
		writeStmt.setLong(5, ts);
		writeStmt.executeUpdate();
	}

	private <T extends Entity> boolean replaceKV(Connection conn, T entity) throws SQLException
	{
		PreparedStatement writeStmt = conn.prepareStatement(KV_REPLACE());
		writeStmt.setString(1, entity.getType());
		writeStmt.setString(2, entityToStr(entity));
		writeStmt.setLong(3, System.currentTimeMillis());
		writeStmt.setString(4, entity.getId());
		writeStmt.setLong(5, entity.getSubId());
		int count = writeStmt.executeUpdate();
		return (count > 0);
	}

	private int deleteKV(Connection conn, String id, Long subId, long ts) throws SQLException
	{
//		System.err.println("deleteKV " + ts);
		String stmt = (subId == null) ? KV_DELETE_ALL() : KV_DELETE();
		PreparedStatement writeStmt = conn.prepareStatement(stmt);
		writeStmt.setLong(1, ts);
		writeStmt.setString(2, id);
		if (subId != null)
			writeStmt.setLong(3, subId);
		int count = writeStmt.executeUpdate();
		return count;
	}

	private boolean existsKV(Connection conn, final String id, final long subId) throws SQLException
	{
		PreparedStatement readStmt = conn.prepareStatement(KV_CHECK());
		readStmt.setString(1, id);
		readStmt.setLong(2, subId);
		ResultSet rs = readStmt.executeQuery();
		return (rs.next());
	}

	private <T extends PatchableEntity> T readSingle(Connection conn, String id, long subId, Class<T> clz) throws SQLException
	{
		PreparedStatement readStmt = conn.prepareStatement(KV_READ_SINGLE());
		readStmt.setString(1, id);
		readStmt.setLong(2, subId);
		ResultSet rs = readStmt.executeQuery();
		if (rs.next())
			return strToEntity(rs.getString(1), clz);
		return null;
	}

	private <T extends Entity> List<T> readMultiple(Connection conn, String id, long subIdFrom, long subIdTo, Class<T> clz)
			throws SQLException
	{
		PreparedStatement readStmt = conn.prepareStatement(KV_READ_SUB_RANGE());
		readStmt.setString(1, id);
		readStmt.setLong(2, subIdFrom);
		readStmt.setLong(3, subIdTo);
		ResultSet rs = readStmt.executeQuery();
		return strsToEntities(rs, clz);
	}

	private <T extends Entity> List<T> readMultiple(Connection conn, String id, Class<T> clz) throws SQLException
	{
		PreparedStatement readStmt = conn.prepareStatement(KV_READ_ALL_SUBS());
		readStmt.setString(1, id);
		ResultSet rs = readStmt.executeQuery();
		return strsToEntities(rs, clz);
	}

	private <T extends Entity> List<T> readTable(Connection conn, String tableName, final Class<T> clz) throws SQLException
	{
		PreparedStatement readStmt = conn.prepareStatement(KV_READ_TYPE());
		readStmt.setString(1, tableName);
		ResultSet rs = readStmt.executeQuery();
		return strsToEntities(rs, clz);
	}

	private long maxSubKV(Connection conn, final String id) throws SQLException
	{
		PreparedStatement readStmt = conn.prepareStatement(KV_MAX());
		readStmt.setString(1, id);
		ResultSet rs = readStmt.executeQuery();
		if (rs.next())
			return rs.getLong(1);
		return 0L;
	}

	private void associate(Connection conn, String userId, String id, long subId, long ts) throws SQLException
	{
//		System.err.println("assocSYNC " + ts);
		PreparedStatement writeStmt = conn.prepareStatement(SYNC_ASSOC());
		writeStmt.setString(1, userId);
		writeStmt.setString(2, id);
		writeStmt.setLong(3, subId);
		writeStmt.setLong(4, ts);
		writeStmt.executeUpdate();
	}

	private void disassociate(Connection conn, String userId, String id, long subId, long ts) throws SQLException
	{
//		System.err.println("disassocSYNC " + ts);
		PreparedStatement writeStmt = conn.prepareStatement(SYNC_DISAS());
		writeStmt.setLong(1, ts);
		writeStmt.setString(2, userId);
		writeStmt.setString(3, id);
		writeStmt.setLong(4, subId);
		writeStmt.executeUpdate();
	}

	@Override
	public <T extends Entity> List<T> get(final String id, final long subIdFrom, final long subIdTo, final Class<T> clz)
	{
		return execute(new ConnListener<List<T>>()
		{
			@Override
			public List<T> onConnection(Connection conn) throws SQLException
			{
				return readMultiple(conn, id, subIdFrom, subIdTo, clz);
			}
		});
	}

	@Override
	public <T extends Entity> List<T> getAllSubs(final String id, final Class<T> clz)
	{
		return execute(new ConnListener<List<T>>()
		{
			@Override
			public List<T> onConnection(Connection conn) throws SQLException
			{
				return readMultiple(conn, id, clz);
			}
		});
	}

	@Override
	public <T extends Entity> List<T> getAllType(final String tableName, final Class<T> clz)
	{
		return execute(new ConnListener<List<T>>()
		{
			@Override
			public List<T> onConnection(Connection conn) throws SQLException
			{
				return readTable(conn, tableName, clz);
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
	public <T extends Entity, R extends PatchableEntity> boolean patch(final T partialEntity, final Class<R> clz)
	{
		return transact(new ConnListener<Boolean>()
		{
			@Override
			public Boolean onConnection(Connection conn) throws SQLException
			{
				R entity = readSingle(conn, partialEntity.getId(), partialEntity.getSubId(), clz);

				if (entity == null)
					return Boolean.FALSE;

				entity.patchWith(partialEntity);
				return Boolean.valueOf(replaceKV(conn, entity)); // theoretically should always be TRUE
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
				return existsKV(conn, id, subId);
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
				return Integer.valueOf(deleteKV(conn, id, subId, System.currentTimeMillis()));
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
				return Integer.valueOf(deleteKV(conn, id, null, System.currentTimeMillis()));
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
				return maxSubKV(conn, id);
			}
		});
	}

	private <T extends Entity> T strToEntity(String entityAsJson, final Class<T> clz)
	{
//		System.err.println(entityAsJson);
		return JSON.fromJson(entityAsJson, clz);
	}

	private <T extends Entity> List<T> strsToEntities(ResultSet rs, final Class<T> clz) throws SQLException
	{
		List<T> list = new ArrayList<>();
		while (rs.next())
			list.add(strToEntity(rs.getString(1), clz));
		return list;
	}

	private <T extends Entity> String entityToStr(final T entity)
	{
		String entityAsJson = JSON.toJson(entity);
//		System.err.println(entityAsJson);
		return entityAsJson;
	}

	@Override
	public void setAssociation(String userId, String entityId, boolean associate)
	{
		setAssociation(userId, entityId, ALL_SUBS, associate);
	}

	@Override
	public void setAssociation(final String userId, final String entityId, final long entitySubId, final boolean associate)
	{
		execute(new ConnListener<Void>()
		{
			@Override
			public Void onConnection(Connection conn) throws SQLException
			{
				if (associate)
					associate(conn, userId, entityId, entitySubId, System.currentTimeMillis());
				else
					disassociate(conn, userId, entityId, entitySubId, System.currentTimeMillis());
				return null;
			}
		});
	}

	@Override
	public List<ServerSyncEvent> sync(final String userId, final long syncStart, final long syncEnd, List<ClientSyncEvent> clientSEs)
	{
		final Map<String, Map<Long, ClientSyncEvent>> index = indexClientSEs(clientSEs);

		return transact(new ConnListener<List<ServerSyncEvent>>()
		{
			@Override
			public List<ServerSyncEvent> onConnection(Connection conn) throws SQLException
			{
//				System.err.println("SYNCING " + syncStart + " .. " + syncEnd);
//				printTable(conn, "KVDB");
//				printTable(conn, "SYNCDB");
				PreparedStatement readStmt = conn.prepareStatement(SYNC_GET_CHANGES());
				readStmt.setString(1, userId);
				readStmt.setLong(2, syncStart);
				readStmt.setLong(3, syncEnd);
				readStmt.setLong(4, syncStart);
				readStmt.setLong(5, syncStart);
				readStmt.setLong(6, syncEnd);
				ResultSet rs = readStmt.executeQuery();
				List<ServerSyncEvent> list = new ArrayList<>();
				while (rs.next())
				{
					ServerSyncEvent serverSE = extractServerSE(rs);
					if (serverSE == null)
						continue;
					ServerSyncEvent resolvedSE = checkConflict(serverSE, conn);
					if (resolvedSE != serverSE) // TODO: either use equals, or have the user return a 'hasChanged' boolean
						applySync(conn, resolvedSE);
					list.add(serverSE);
				}

				// sync all the non-conflicted entities from the client into the server's db
				for (Map<Long, ClientSyncEvent> map : index.values())
					for (ClientSyncEvent clientSE : map.values())
						applySync(conn, clientSE);

				return list;
			}

			private ServerSyncEvent extractServerSE(ResultSet rs) throws SQLException
			{
				final String id = rs.getString(1);
				final long subId = rs.getLong(2);
				final long ut = rs.getLong(3);
				final boolean isDel = rs.getByte(4) != 0;
				final long sut = rs.getLong(5);
				final boolean isDis = rs.getByte(6) != 0;
				final String type = rs.getString(7);
				final String value = rs.getString(8);

				boolean isUpd = !isDel;
				boolean isEng = !isDis;
				final boolean isAssocBefore = (sut <= syncStart);
				final boolean isAssocDuring = !isAssocBefore; // because the query doesn't bring 'AFTER' associations
				final boolean isChangeBefore = (ut <= syncStart);
				final boolean isChangeAfter = (ut > syncEnd);
				final boolean isChangeDuring = !isChangeBefore && !isChangeAfter; // always true if 'isAssocBefore'

				boolean sendDelete = (isAssocBefore && isEng && isDel) || (isAssocDuring && (//
						(isDis && !(isChangeBefore && isDel)) || //
						(isEng && (isChangeDuring && isDel))));

				boolean sendUpdate = isUpd && isEng && !isChangeBefore;

				if (!sendUpdate && !sendDelete)
				{
					System.err.println("Skipping a record..");
					return null;
				}

				return sendDelete ? ServerSyncEvent.create(id, subId, type, null, ServerChangeType.DELETE) : //
						ServerSyncEvent.create(id, subId, type, value, ServerChangeType.CHANGE);
			}

			private void applySync(Connection conn, ServerSyncEvent syncEntity) throws SQLException
			{
				String id = syncEntity.id;
				long subId = syncEntity.subId;

				switch (syncEntity.changeType)
				{
					case CHANGE:
						mergeKV(conn, id, subId, syncEntity.type, syncEntity.value, syncEnd);
						break;

					case DELETE:
						deleteKV(conn, id, subId, syncEnd);
						break;
				}
			}

			private void applySync(Connection conn, ClientSyncEvent syncEntity) throws SQLException
			{
				String id = syncEntity.id;
				long subId = syncEntity.subId;

				switch (syncEntity.changeType)
				{
					case CHANGE:
						// TODO: check if not already associated at the KEY value!
						associate(conn, userId, id, subId, syncEnd); // for the case of client-initiated CREATE
						// no break

					case REPLACE:
						mergeKV(conn, id, subId, syncEntity.type, syncEntity.value, syncEnd);
						break;

					case DELETE:
						deleteKV(conn, id, subId, syncEnd);
						break;

					case PURGE:
						disassociate(conn, userId, id, subId, syncEnd);
						break;
				}
			}

			private ServerSyncEvent checkConflict(ServerSyncEvent serverSE, Connection conn) throws SQLException
			{
				Map<Long, ClientSyncEvent> map = index.get(serverSE.id);
				if (map != null)
				{
					AbstractSyncEvent clientSE = map.remove(serverSE.subId);
					if (clientSE != null)
						return resolve(serverSE, clientSE);
				}
				return serverSE;
			}

			private ServerSyncEvent resolve(ServerSyncEvent serverSE, AbstractSyncEvent clientSE)
			{
				LOG.warn("CONFLICT: " + clientSE + " -- vs -- " + serverSE);
				// TODO: implement (fire an event)
				return serverSE;
			}
		});
	}

	private Map<String, Map<Long, ClientSyncEvent>> indexClientSEs(List<ClientSyncEvent> clientSEs)
	{
		Map<String, Map<Long, ClientSyncEvent>> index = new HashMap<>();
		if (clientSEs != null)
		{
			for (ClientSyncEvent se : clientSEs)
			{
				Map<Long, ClientSyncEvent> map = index.get(se.id);
				if (map == null)
					index.put(se.id, map = new HashMap<>());
				map.put(se.subId, se);
			}
		}
		return index;
	}

	protected void printTable(Connection conn, String tableName) throws SQLException
	{
		ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM " + tableName);
		System.out.println(">>>>>>>>> Printing " + tableName);
		printResultSet(rs);
		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
	}

	protected void printResultSet(ResultSet rs) throws SQLException
	{
		while (rs.next())
		{
			for (int i = 0; i <= rs.getMetaData().getColumnCount() - 1; i++)
				System.out.print(rs.getObject(i + 1).toString() + " | ");
			System.out.println();
		}
	}
}