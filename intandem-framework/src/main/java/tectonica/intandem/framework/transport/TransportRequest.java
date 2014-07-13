package tectonica.intandem.framework.transport;

import java.util.List;

import tectonica.intandem.framework.client.ClientSyncEvent;

public class TransportRequest
{
	public String userId;
	public long syncStart;
	public long syncEnd;
	public List<ClientSyncEvent> clientSEs;

	public static TransportRequest create(String userId, long syncStart, long syncEnd, List<ClientSyncEvent> clientSEs)
	{
		TransportRequest tr = new TransportRequest();
		tr.userId = userId;
		tr.syncStart = syncStart;
		tr.syncEnd = syncEnd;
		tr.clientSEs = clientSEs;
		return tr;
	}

	@Override
	public String toString()
	{
		return "TransportRequest [userId=" + userId + ", syncStart=" + syncStart + ", syncEnd=" + syncEnd + ", clientSEs=" + clientSEs
				+ "]";
	}
}