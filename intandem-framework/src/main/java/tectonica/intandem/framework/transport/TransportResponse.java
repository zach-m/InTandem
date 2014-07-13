package tectonica.intandem.framework.transport;

import java.util.List;

import tectonica.intandem.framework.server.ServerSyncEvent;

public class TransportResponse
{
	public List<ServerSyncEvent> serverSEs;

	public static TransportResponse create(List<ServerSyncEvent> serverSEs)
	{
		TransportResponse tr = new TransportResponse();
		tr.serverSEs = serverSEs;
		return tr;
	}
}