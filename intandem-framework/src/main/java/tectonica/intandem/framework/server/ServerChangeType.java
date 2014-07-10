package tectonica.intandem.framework.server;

public enum ServerChangeType
{
	CHANGE(1), //
	DELETE(-1);

	final public byte code;

	private ServerChangeType(int code)
	{
		this.code = (byte) code;
	}

	public static ServerChangeType fromCode(int code)
	{
		for (ServerChangeType ct : values())
			if (ct.code == code)
				return ct;
		throw new RuntimeException("code " + code + " doesn't have an equivalent " + ServerChangeType.class.getSimpleName());
	}
}
