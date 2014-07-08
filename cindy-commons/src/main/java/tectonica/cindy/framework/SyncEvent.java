package tectonica.cindy.framework;

public class SyncEvent
{
	public String id;
	public long subId;
	public String type;
	public String value;
	public ChangeType changeType;

	public static SyncEvent create(String id, long subId, String type, String value, ChangeType changeType)
	{
		SyncEvent se = new SyncEvent();
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
}
