package tectonica.cindy.framework;

public class SyncEntity
{
	public String id;
	public long subId;
	public String type;
	public String value;
	public ChangeType changeType;

	public static SyncEntity create(String id, long subId, String type, String value, ChangeType changeType)
	{
		SyncEntity se = new SyncEntity();
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
		return "SyncEntity [id=" + id + ", subId=" + subId + ", type=" + type + ", value=" + value + ", changeType=" + changeType + "]";
	}
}
