package tectonica.intandem.framework.client;

public enum ClientChangeType
{
	// NOTE: use negative values for non-existing rows. 0 is not allowed (means UNCHANCHED)
	CHANGE(1), //
	REPLACE(2), //
	DELETE(-1), //
	PURGE(-2);

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
