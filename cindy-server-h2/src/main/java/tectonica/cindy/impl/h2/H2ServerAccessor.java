package tectonica.cindy.impl.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.LoggerFactory;

import tectonica.cindy.framework.ChangeType;
import tectonica.cindy.framework.Entity;
import tectonica.cindy.framework.ServerAccessor;
import tectonica.cindy.framework.SyncEntity;
import ch.qos.logback.classic.Logger;

public class H2ServerAccessor extends SQLProvider implements ServerAccessor
{
	private static final String CONN_STR = "jdbc:h2:mem:test";
	private static final String CONN_USERNAME = "sa";
	private static final String CONN_PASSWORD = "sa";

	// //////////////////////////////////////////////////////////////////////////////////////
	//
	// Key-Value Modeling ("KV")
	//
	// //////////////////////////////////////////////////////////////////////////////////////
	private static final String KV_INIT = "CREATE TABLE KVDB (K VARCHAR2, SK BIGINT, T VARCHAR2, V VARCHAR2, UT BIGINT, D TINYINT, PRIMARY KEY(K, SK))";
	private static final String KV_READ = "SELECT V FROM KVDB WHERE (K = ?) AND (SK BETWEEN ? AND ?) AND (D = 0)";
	private static final String KV_WRITE = "MERGE INTO KVDB KEY (K, SK) VALUES (?, ?, ?, ?, ?, 0)";
	private static final String KV_CHECK = "SELECT 1 FROM KVDB WHERE (K = ?) AND (SK = ?) AND (D = 0)";
	private static final String KV_DELETE = "UPDATE KVDB SET UT = ?, D = 1 WHERE (K = ?) AND (SK = ?) AND (D = 0)";

	// //////////////////////////////////////////////////////////////////////////////////////
	//
	// User-Key Modeling ("SYNC")
	//
	// //////////////////////////////////////////////////////////////////////////////////////
	private static final String SYNC_INIT = "CREATE TABLE SYNCDB (U VARCHAR2, K VARCHAR2, SK BIGINT, SUT BIGINT, SD TINYINT, PRIMARY KEY(U, K, SK))";
	private static final String SYNC_ASSOC = "MERGE INTO SYNCDB KEY (U, K, SK) VALUES (?, ?, ?, ?, 0)";
	private static final String SYNC_DISAS = "UPDATE SYNCDB SET SUT = ?, SD = 1 WHERE (U = ?) AND (K = ?) AND (SK = ?) AND (SD = 0)";

	private static final String SYNC_RAW = "" + //
			"SELECT SYNCDB.K, SYNCDB.SK, UT, D, SUT, SD, T, V " + //
			"FROM SYNCDB JOIN KVDB ON (SYNCDB.K = KVDB.K AND SYNCDB.SK = KVDB.SK) " + //
			"WHERE (U = ?) AND (" //
			+ "(SUT > ? AND SUT <= ?) OR " //
			+ "((SUT <= ?) AND (UT > ? AND UT <= ?))" //
			+ ")";

	private static Logger LOG = (Logger) LoggerFactory.getLogger(H2ServerAccessor.class.getSimpleName());

	public H2ServerAccessor()
	{
		this(CONN_STR, CONN_USERNAME, CONN_PASSWORD);
	}

	public H2ServerAccessor(String connStr, String username, String password)
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
				conn.createStatement().execute(SYNC_INIT);
				return null;
			}
		});

		LOG.info("Done " + msg);
	}

	public void destruct()
	{
		connPool.dispose();
	}

	private void updateKV(Connection conn, String id, long subId, String type, String value, long ts) throws SQLException
	{
//		System.err.println("updateKV " + ts);
		PreparedStatement writeStmt = conn.prepareStatement(KV_WRITE);
		writeStmt.setString(1, id);
		writeStmt.setLong(2, subId);
		writeStmt.setString(3, type);
		writeStmt.setString(4, value);
		writeStmt.setLong(5, ts);
		writeStmt.executeUpdate();
	}

	private void deleteKV(Connection conn, String id, long subId, long ts) throws SQLException
	{
//		System.err.println("deleteKV " + ts);
		PreparedStatement writeStmt = conn.prepareStatement(KV_DELETE);
		writeStmt.setLong(1, ts);
		writeStmt.setString(2, id);
		writeStmt.setLong(3, subId);
		writeStmt.executeUpdate();
	}

//	private void associateSYNC(Connection conn, String userId, String id, long subId, long ts, ChangeType changeType) throws SQLException
//	{
//		PreparedStatement writeStmt = conn.prepareStatement(SYNC_WRITE);
//		writeStmt.setString(1, userId);
//		writeStmt.setString(2, id);
//		writeStmt.setLong(3, subId);
//		writeStmt.setLong(4, ts);
//		writeStmt.setByte(5, changeType.code);
//		writeStmt.executeUpdate();
//	}

	private void associate(Connection conn, String userId, String id, long subId, long ts) throws SQLException
	{
//		System.err.println("assocSYNC " + ts);
		PreparedStatement writeStmt = conn.prepareStatement(SYNC_ASSOC);
		writeStmt.setString(1, userId);
		writeStmt.setString(2, id);
		writeStmt.setLong(3, subId);
		writeStmt.setLong(4, ts);
		writeStmt.executeUpdate();
	}

	private void disassociate(Connection conn, String userId, String id, long subId, long ts) throws SQLException
	{
//		System.err.println("disassocSYNC " + ts);
		PreparedStatement writeStmt = conn.prepareStatement(SYNC_DISAS);
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
				PreparedStatement readStmt = conn.prepareStatement(KV_READ);
				readStmt.setString(1, id);
				readStmt.setLong(2, subIdFrom);
				readStmt.setLong(3, subIdTo);
				ResultSet rs = readStmt.executeQuery();
				List<T> list = new ArrayList<>();
				while (rs.next())
					list.add(JSON.fromJson(rs.getString(1), clz));
				return list;
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
				updateKV(conn, entity.getId(), entity.getSubId(), entity.getType(), JSON.toJson(entity), System.currentTimeMillis());
				return null;
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
				PreparedStatement readStmt = conn.prepareStatement(KV_CHECK);
				readStmt.setString(1, id);
				readStmt.setLong(2, subId);
				ResultSet rs = readStmt.executeQuery();
				return (rs.next());
			}
		});
	}

	@Override
	public void delete(final String id, final long subId)
	{
		execute(new ConnListener<Void>()
		{
			@Override
			public Void onConnection(Connection conn) throws SQLException
			{
				deleteKV(conn, id, subId, System.currentTimeMillis());
				return null;
			}
		});
	}

	// TODO: support association with an entire entityId (i.e. all of its subIds)

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
	public List<SyncEntity> performSync(final String userId, final long syncStart, final long syncEnd, List<SyncEntity> clientSEs)
	{
		final Map<String, Map<Long, SyncEntity>> index = indexClientSEs(clientSEs);

		return transact(new ConnListener<List<SyncEntity>>()
		{
			@Override
			public List<SyncEntity> onConnection(Connection conn) throws SQLException
			{
//				System.err.println("SYNCING " + syncStart + " .. " + syncEnd);
//				printTable(conn, "KVDB");
//				printTable(conn, "SYNCDB");
				PreparedStatement readStmt = conn.prepareStatement(SYNC_RAW);
				readStmt.setString(1, userId);
				readStmt.setLong(2, syncStart);
				readStmt.setLong(3, syncEnd);
				readStmt.setLong(4, syncStart);
				readStmt.setLong(5, syncStart);
				readStmt.setLong(6, syncEnd);
				ResultSet rs = readStmt.executeQuery();
				List<SyncEntity> list = new ArrayList<>();
				while (rs.next())
				{
					SyncEntity serverSE = extractServerSE(rs, syncStart, syncEnd);
					if (serverSE == null)
						continue;
					SyncEntity resolvedSE = checkConflict(serverSE, conn);
					if (resolvedSE != serverSE) // TODO: either use equals, or have the user return a 'hasChanged' boolean
						applySync(conn, resolvedSE, false);
					list.add(serverSE);
				}

				// sync all the non-conflicted entities from the client into the server's db
				for (Map<Long, SyncEntity> map : index.values())
					for (SyncEntity clientSE : map.values())
						applySync(conn, clientSE, true);

				return list;
			}

			private SyncEntity extractServerSE(ResultSet rs, final long syncStart, final long syncEnd) throws SQLException
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

				return sendDelete ? SyncEntity.create(id, subId, type, null, ChangeType.DELETE) : //
						SyncEntity.create(id, subId, type, value, ChangeType.CHANGE);
			}

			private void applySync(Connection conn, SyncEntity syncEntity, boolean isClientSE) throws SQLException
			{
				String id = syncEntity.id;
				long subId = syncEntity.subId;

				switch (syncEntity.changeType)
				{
					case CHANGE:
					case UPDATE:
						updateKV(conn, id, subId, syncEntity.type, syncEntity.value, syncEnd);
						if (isClientSE && syncEntity.changeType != ChangeType.UPDATE)
							associate(conn, userId, id, subId, syncEnd); // for the case of client-initiated CREATE
						break;

					case DELETE:
						deleteKV(conn, id, subId, syncEnd);
						break;

					case PURGE:
						if (isClientSE)
							disassociate(conn, userId, id, subId, syncEnd);
						else
							throw new RuntimeException("Internal error: can't apply PURGE for ServerSE " + syncEntity.toString());
						break;
				}
			}

			private SyncEntity checkConflict(SyncEntity serverSE, Connection conn) throws SQLException
			{
				Map<Long, SyncEntity> map = index.get(serverSE.id);
				if (map != null)
				{
					SyncEntity clientSE = map.remove(serverSE.subId);
					if (clientSE != null)
						return resolve(serverSE, clientSE);
				}
				return serverSE;
			}

			/**
			 * required to return a valid ServerSE
			 * 
			 * @param serverSE
			 * @param clientSE
			 * @return
			 */
			private SyncEntity resolve(SyncEntity serverSE, SyncEntity clientSE)
			{
				LOG.warn("CONFLICT: " + clientSE + " -- vs -- " + serverSE);
				// TODO: implement (fire an event)
				return serverSE;
			}

			@SuppressWarnings("unused")
			private void printTable(Connection conn, String tableName) throws SQLException
			{
				ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM " + tableName);
				System.out.println(">>>>>>>>> Printing " + tableName);
				while (rs.next())
				{
					for (int i = 0; i <= rs.getMetaData().getColumnCount() - 1; i++)
						System.out.print(rs.getObject(i + 1).toString() + " | ");
					System.out.println();
				}
				System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			}
		});
	}

	private Map<String, Map<Long, SyncEntity>> indexClientSEs(List<SyncEntity> clientSEs)
	{
		Map<String, Map<Long, SyncEntity>> index = new HashMap<>();
		if (clientSEs != null)
		{
			for (SyncEntity se : clientSEs)
			{
				Map<Long, SyncEntity> map = index.get(se.id);
				if (map == null)
					index.put(se.id, map = new HashMap<>());
				map.put(se.subId, se);
			}
		}
		return index;
	}
}
