package tectonica.cindy.framework;

public interface ClientAccessor extends BaseAccessor
{
	public <T extends Entity> void replace(T entity);

	public void purge(String id, long subId);
}
