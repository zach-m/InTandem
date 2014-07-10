package tectonica.cindy.framework.client;

import tectonica.cindy.framework.BaseAccessor;

public interface ClientAccessor extends BaseAccessor
{
	public void purge(String id, long subId);
}
