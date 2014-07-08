package tectonica.test.cindy;

import java.util.List;

import tectonica.cindy.framework.Entity;
import tectonica.cindy.framework.ServerAccessor;
import tectonica.cindy.framework.SyncEntity;
import tectonica.cindy.impl.h2.H2ServerAccessor;
import tectonica.test.cindy.model.Person;

public class Tester
{
	private static ServerAccessor s;

	public static void main(String[] args)
	{
		long syncStart = System.currentTimeMillis();

		s = new H2ServerAccessor();
		s.put(Person.create("a", Entity.NO_SUB_ID, "Name A", 10, 1.10));
		s.setAssociation("user1", "a", Entity.NO_SUB_ID, true);
		s.put(Person.create("b", 1L, "WRONG", 0, 0.0));
		s.setAssociation("user1", "b", 1L, true);
		s.put(Person.create("c", 5L, "Name C", 30, 1.30));
		s.put(Person.create("b", 1L, "Name B1", 20, 1.20));
		s.put(Person.create("b", 2L, "Name B2", 20, 1.20));
		s.put(Person.create("b", 3L, "Name B3", 20, 1.20));
		s.setAssociation("user1", "b", 1L, true);
		s.setAssociation("user1", "b", 4L, true); // doesn't exist, do we want to allow it into the database?..

		List<Person> list = s.get("b", 1L, 2L, Person.class);
		System.out.println(list.toString());

		System.out.println("C:    " + s.get("c", 5L, 5L, Person.class));
		System.out.println("No-C: " + s.get("c", Entity.NO_SUB_ID, Entity.NO_SUB_ID, Person.class));
		System.out.println("D:    " + s.get("d", Entity.NO_SUB_ID, Entity.NO_SUB_ID, Person.class));
		System.out.println("--------------------------------------------------------");

		SyncResults sync = sync("user1", syncStart, null);
		System.out.println(sync.syncEntities.toString());
		System.out.println("--------------------------------------------------------");

		s.setAssociation("user1", "a", Entity.NO_SUB_ID, false);
//		s.delete("a", Entity.NO_SUB_ID);
		syncStart = sync.syncEnd;
		sync = sync("user1", syncStart, null);
		System.out.println(sync.syncEntities.toString());
		System.out.println("--------------------------------------------------------");

		sync = sync("user1", syncStart, null);
		System.out.println(sync.syncEntities.toString());
		System.out.println("--------------------------------------------------------");

		syncStart = sync.syncEnd;
		sync = sync("user1", syncStart, null);
		System.out.println(sync.syncEntities.toString());
		System.out.println("--------------------------------------------------------");
	}

	public static SyncResults sync(String userId, long syncStart, List<SyncEntity> clientSEs)
	{
		long syncEnd = System.currentTimeMillis();
		if (syncEnd == syncStart)
			throw new RuntimeException("Illegal time range");
		List<SyncEntity> syncEntities = s.performSync(userId, syncStart, syncEnd, clientSEs);
		sleep(1);
		return SyncResults.create(syncStart, syncEnd, syncEntities);
	}

	public static void sleep(int ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch (InterruptedException e)
		{
			System.exit(1);
		}
	}
}
