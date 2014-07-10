package tectonica.cindy.framework;

import java.util.List;

public interface BaseAccessor
{
	public <T extends Entity> List<T> get(String id, long subIdFrom, long subIdTo, Class<T> clz);

	public <T extends Entity> void put(T entity);

	public <T extends Entity> boolean replace(T entity);

	public <T extends Entity> boolean patch(T partialEntity, Class<T> clz);

	public boolean exists(String id, long subId);

	public int delete(String id, long subId);

	public int delete(String id);

	public long getMaxSubId(String id);

	void cleanup();
}
