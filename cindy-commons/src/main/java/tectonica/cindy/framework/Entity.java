package tectonica.cindy.framework;

public abstract interface Entity
{
	long NO_SUB_ID = -1L; // Long.MIN_VALUE;

	public abstract String getId();

	public long getSubId();

	public abstract String getType();
}