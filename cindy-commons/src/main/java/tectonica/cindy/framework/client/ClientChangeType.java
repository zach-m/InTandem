package tectonica.cindy.framework.client;

public enum ClientChangeType
{
	CHANGE(1), //
	DELETE(2), //
	PURGE(3), //
	REPLACE(4);

	final public byte code;

	private ClientChangeType(int code)
	{
		this.code = (byte) code;
	}

	public static ClientChangeType fromCode(int code)
	{
		for (ClientChangeType ct : values())
			if (ct.code == code)
				return ct;
		throw new RuntimeException("code " + code + " doesn't have an equivalent " + ClientChangeType.class.getSimpleName());
	}
}
