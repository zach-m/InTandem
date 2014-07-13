package tectonica.test.intandem;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tectonica.intandem.framework.server.ServerSyncEvent;
import tectonica.intandem.framework.transport.TransportRequest;
import tectonica.intandem.framework.transport.TransportResponse;
import tectonica.intandem.transport.util.JSON;

public class TestServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		try
		{
			TransportRequest tReq = JSON.fromJson(req.getInputStream(), TransportRequest.class);
			List<ServerSyncEvent> serverSEs = TestTransport.s.sync(tReq.userId, tReq.syncStart, tReq.syncEnd, tReq.clientSEs);
			JSON.toJson(resp.getOutputStream(), TransportResponse.create(serverSEs));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
	}
}