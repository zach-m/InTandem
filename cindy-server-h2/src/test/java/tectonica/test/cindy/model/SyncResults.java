package tectonica.test.cindy.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import tectonica.cindy.framework.SyncEvent;

@XmlRootElement
public class SyncResults
{
	public long syncStart;
	public long syncEnd;
	public List<SyncEvent> syncEntities;

	public static SyncResults create(long lastSyncTime, long newSyncTime, List<SyncEvent> syncEntities)
	{
		SyncResults s = new SyncResults();
		s.syncStart = lastSyncTime;
		s.syncEnd = newSyncTime;
		s.syncEntities = syncEntities;
		return s;
	}
}
