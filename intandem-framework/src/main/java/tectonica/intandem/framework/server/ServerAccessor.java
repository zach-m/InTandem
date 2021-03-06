package tectonica.intandem.framework.server;

import java.util.List;

import tectonica.intandem.framework.BaseAccessor;
import tectonica.intandem.framework.client.ClientSyncEvent;

public interface ServerAccessor extends BaseAccessor
{
	public void setAssociation(String userId, String entityId, boolean associate);

	public void setAssociation(String userId, String entityId, long entitySubId, boolean associate);

	public List<ServerSyncEvent> sync(String userId, long syncStart, long syncEnd, List<ClientSyncEvent> clientSEs);
}
