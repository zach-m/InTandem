package tectonica.intandem.impl.jdbc;

import java.util.List;

public interface SqlServer extends SqlCommon
{
	public String KV_DELETE();

	public String KV_DELETE_ALL();

	public List<String> SYNC_INIT();

	public String SYNC_ASSOC();

	public String SYNC_DISAS();
}
