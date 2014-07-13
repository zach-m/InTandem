package tectonica.intandem.framework.transport;

import java.util.List;

import tectonica.intandem.framework.client.ClientSyncEvent;
import tectonica.intandem.framework.server.ServerSyncEvent;

public interface ServerAccessorProxy
{
	public void setUrl(String url);

	public List<ServerSyncEvent> sync(String userId, long syncStart, long syncEnd, List<ClientSyncEvent> clientSEs);
}
