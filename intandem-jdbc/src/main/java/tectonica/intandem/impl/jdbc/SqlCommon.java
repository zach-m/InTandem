package tectonica.intandem.impl.jdbc;

import java.util.List;

public interface SqlCommon
{
	public List<String> KV_INIT();

	public String KV_READ_SINGLE();

	public String KV_READ_SUB_RANGE();

	public String KV_READ_ALL_SUBS();

	public String KV_READ_TYPE();

	public String KV_MERGE();

	public String KV_REPLACE();

	public String KV_CHECK();

	public String KV_MAX();
	
	public String SYNC_GET_CHANGES();
}
