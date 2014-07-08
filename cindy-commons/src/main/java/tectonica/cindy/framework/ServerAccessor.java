package tectonica.cindy.framework;

import java.util.List;

public interface ServerAccessor extends BaseAccessor
{
	/**
	 * marks a record to be changed at a client database
	 * 
	 * @param changeType
	 *            either CHANGED or DELETED
	 */
	public void setAssociation(String userId, String entityId, long entitySubId, boolean associate);

	public List<SyncEntity> performSync(String userId, long syncStart, long syncEnd, List<SyncEntity> clientSEs);
}
