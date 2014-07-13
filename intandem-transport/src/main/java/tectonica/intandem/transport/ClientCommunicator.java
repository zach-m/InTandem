package tectonica.intandem.transport;

import java.util.List;

import tectonica.intandem.framework.client.ClientSyncEvent;
import tectonica.intandem.framework.server.ServerSyncEvent;
import tectonica.intandem.framework.transport.ServerAccessorProxy;
import tectonica.intandem.framework.transport.TransportRequest;
import tectonica.intandem.framework.transport.TransportResponse;
import tectonica.intandem.transport.util.HTTP;
import tectonica.intandem.transport.util.HTTP.HttpResponse;
import tectonica.intandem.transport.util.JSON;

public class ClientCommunicator implements ServerAccessorProxy
{
	private String url;

	@Override
	public void setUrl(String url)
	{
		this.url = url;
	}

	@Override
	public List<ServerSyncEvent> sync(String userId, long syncStart, long syncEnd, List<ClientSyncEvent> clientSEs)
	{
		TransportRequest tReq = TransportRequest.create(userId, syncStart, syncEnd, clientSEs);
		String json = JSON.toJson(tReq);
		HttpResponse http = HTTP.url(url).body(json).POST();
		if (http.statusCode / 100 == 2)
		{
			TransportResponse tResp = JSON.fromJson(http.content, TransportResponse.class);
			return tResp.serverSEs;
		}
		System.err.println("ERROR: " + http.statusCode);
		return null;
	}
}
