package tectonica.intandem.impl.jdbc;

public interface SqlServer
{
	public String KV_INIT();

	public String KV_DROP();

	public String KV_READ_SINGLE();

	public String KV_READ_MULTIPLE();

	public String KV_MERGE();

	public String KV_REPLACE();

	public String KV_CHECK();

	public String KV_DELETE();

	public String KV_DELETE_ALL();

	public String KV_MAX();

	public String SYNC_INIT();

	public String SYNC_DROP();

	public String SYNC_ASSOC();

	public String SYNC_DISAS();

	public String SYNC_GET_CHANGES();
}
