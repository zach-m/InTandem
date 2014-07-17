package tectonica.intandem.impl.jdbc;

public interface SqlClient extends SqlCommon
{
	public String KV_DELETE_PURGE();

	public String KV_DELETE_PURGE_ALL();

	public String SYNC_RESET_CHANGES();
}
