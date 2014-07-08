package tectonica.test.cindy;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import tectonica.cindy.framework.SyncEntity;

@XmlRootElement
public class SyncResults
{
	public long syncStart;
	public long syncEnd;
	public List<SyncEntity> syncEntities;

	public static SyncResults create(long lastSyncTime, long newSyncTime, List<SyncEntity> syncEntities)
	{
		SyncResults s = new SyncResults();
		s.syncStart = lastSyncTime;
		s.syncEnd = newSyncTime;
		s.syncEntities = syncEntities;
		return s;
	}
}
