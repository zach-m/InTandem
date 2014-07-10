package tectonica.cindy.framework.client;

import tectonica.cindy.framework.AbstractSyncEvent;

public class ClientSyncEvent extends AbstractSyncEvent
{
	public ClientChangeType changeType;

	public static ClientSyncEvent create(String id, long subId, String type, String value, ClientChangeType changeType)
	{
		ClientSyncEvent se = new ClientSyncEvent();
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
		ClientSyncEvent other = (ClientSyncEvent) obj;
		if (changeType != other.changeType)
			return false;
		return true;
	}
}
