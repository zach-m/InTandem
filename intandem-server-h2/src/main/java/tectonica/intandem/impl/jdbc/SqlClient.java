package tectonica.intandem.impl.jdbc;

public interface SqlClient
{
	public String KV_INIT();

	public String KV_DROP();

	public String KV_READ_SINGLE();

	public String KV_READ_MULTIPLE();

	public String KV_MERGE();

	public String KV_REPLACE();

	public String KV_CHECK();

	public String KV_DELETE_PURGE();

	public String KV_DELETE_PURGE_ALL();

	public String KV_MAX();

	public String SYNC_GET_CHANGES();

	public String SYNC_RESET_CHANGES();
}
