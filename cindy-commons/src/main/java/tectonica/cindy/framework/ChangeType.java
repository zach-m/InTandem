package tectonica.cindy.framework;

public enum ChangeType
{
	CHANGE(1), //
	DELETE(2), //
	PURGE(3), // status assignable on to rows on the client-side only
	REPLACE(4); // status assignable on to rows on the client-side only

	final public byte code;

	private ChangeType(int code)
	{
		this.code = (byte) code;
	}

	public static ChangeType fromCode(int code)
	{
		for (ChangeType ct : values())
			if (ct.code == code)
				return ct;
		throw new RuntimeException("code " + code + " doesn't have an equivalent " + ChangeType.class.getSimpleName());
	}
}
