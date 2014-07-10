package tectonica.cindy.framework.server;

import java.util.List;

import tectonica.cindy.framework.BaseAccessor;
import tectonica.cindy.framework.client.ClientSyncEvent;

public interface ServerAccessor extends BaseAccessor
{
	/**
	 * marks a record to be changed at a client database
	 * 
	 * @param changeType
	 *            either CHANGED or DELETED
	 */
	public void setAssociation(String userId, String entityId, long entitySubId, boolean associate);

	public List<ServerSyncEvent> performSync(String userId, long syncStart, long syncEnd, List<ClientSyncEvent> clientSEs);
}
