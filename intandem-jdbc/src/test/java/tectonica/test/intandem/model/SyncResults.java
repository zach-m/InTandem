package tectonica.test.intandem.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import tectonica.intandem.framework.server.ServerSyncEvent;

@XmlRootElement
public class SyncResults
{
	public long syncStart;
	public long syncEnd;
	public List<ServerSyncEvent> events;

	public static SyncResults create(long lastSyncTime, long newSyncTime, List<ServerSyncEvent> syncEntities)
	{
		SyncResults s = new SyncResults();
		s.syncStart = lastSyncTime;
		s.syncEnd = newSyncTime;
		s.events = syncEntities;
		return s;
	}
}
