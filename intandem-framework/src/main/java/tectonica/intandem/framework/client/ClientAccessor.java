package tectonica.intandem.framework.client;

import java.util.List;

import tectonica.intandem.framework.BaseAccessor;
import tectonica.intandem.framework.server.ServerSyncEvent;
import tectonica.intandem.framework.transport.ServerAccessorProxy;

public interface ClientAccessor extends BaseAccessor
{
	public static class SyncResult
	{
		public long syncStart;
		public long nextSyncStart;
		public List<ServerSyncEvent> events;
	}

	public int purge(String id, long subId);

	public SyncResult sync(ServerAccessorProxy server, String userId, long syncStart);
}
