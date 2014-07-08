package tectonica.cindy.framework;

import java.util.List;

public interface BaseAccessor
{
	public <T extends Entity> List<T> get(String id, long subIdFrom, long subIdTo, Class<T> clz);

	public <T extends Entity> void put(T entity);

	public boolean exists(String id, long subId);

	public void delete(String id, long subId);
}
