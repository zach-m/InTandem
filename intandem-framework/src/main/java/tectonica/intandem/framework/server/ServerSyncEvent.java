package tectonica.intandem.framework.server;

import tectonica.intandem.framework.AbstractSyncEvent;

public class ServerSyncEvent extends AbstractSyncEvent
{
	public ServerChangeType changeType;

	public static ServerSyncEvent create(String id, long subId, String type, String value, ServerChangeType changeType)
	{
		ServerSyncEvent se = new ServerSyncEvent();
		se.id = id;
		se.subId = subId;
		se.type = type;
		se.value = value;
		se.changeType = changeType;
		return se;
	}

	@Override
	public String toString()
	{
		return "SyncEvent [id=" + id + ", subId=" + subId + ", type=" + type + ", value=" + value + ", changeType=" + changeType + "]";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerSyncEvent other = (ServerSyncEvent) obj;
		if (changeType != other.changeType)
			return false;
		return true;
	}
}
