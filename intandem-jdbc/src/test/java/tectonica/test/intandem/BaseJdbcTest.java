package tectonica.test.intandem;

import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import tectonica.intandem.framework.client.ClientAccessor;
import tectonica.intandem.framework.client.ClientAccessor.SyncResult;
import tectonica.intandem.framework.client.ClientSyncEvent;
import tectonica.intandem.framework.server.ServerAccessor;
import tectonica.intandem.framework.server.ServerChangeType;
import tectonica.intandem.framework.server.ServerSyncEvent;
import tectonica.intandem.framework.transport.ServerAccessorProxy;
import tectonica.test.intandem.model.Person;
import tectonica.test.intandem.model.SyncResults;

public abstract class BaseJdbcTest
{
	private static final int NETWORK_LATENCY_MS = 100;
	private static final int DB_UPDATE_TIME_SAFETY_MS = 1; // must be at least 1

	protected static ServerAccessor s;
	protected static ClientAccessor c;
	protected static ServerAccessorProxy sp;

	protected static void initProxy()
	{
		sp = new ServerAccessorProxy()
		{
			@Override
			public void setUrl(String url)
			{}

			@Override
			public List<ServerSyncEvent> sync(String userId, long syncStart, long syncEnd, List<ClientSyncEvent> clientSEs)
			{
				return s.sync(userId, syncStart, syncEnd, clientSEs);
			}
		};
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		c.cleanup();
		s.cleanup();
	}

	@Before
	public void setUp() throws Exception
	{}

	@After
	public void tearDown() throws Exception
	{}

	@Test
	public void test()
	{
		s.put(Person.create("a", 0, "Name A", 10, 1.10));
		s.setAssociation("user1", "a", true); // all-subkeys association

		s.put(Person.create("b", 1, "Wrong", 0, 0.0)); // putting a wrong name under b-1
		s.setAssociation("user1", "b", 1, true);

		s.put(Person.create("c", 5, "Name C", 30, 1.30));

		s.put(Person.create("b", 1, "WrongAgain", 20, 1.20)); // replacing the wrong name under b-1 with another wrong name
		s.put(Person.create("b", 2, "Name B2", 20, 1.20));
		s.put(Person.create("b", 3, "Name B3", 20, 1.20));
		s.setAssociation("user1", "b", 1, true);
		s.setAssociation("user1", "b", 4, true); // doesn't exist, meaningless association

		System.out.println("Running read/write/patch tests..");

		// test that the replacement of b-1 has worked
		List<Person> list = s.get("b", 1L, 2L, Person.class); // validating b-1
		Assert.assertEquals("WrongAgain", list.get(0).name);
		Assert.assertEquals("Name B2", list.get(1).name);

		// test that the basics work
		Assert.assertEquals("Name C", s.get("c", 5L, 5L, Person.class).get(0).name);
		Assert.assertEquals(1, s.get("a", 0, 0, Person.class).size());
		Assert.assertEquals(0, s.get("c", 0, 0, Person.class).size());
		Assert.assertEquals(0, s.get("d", 0, 0, Person.class).size());
		Assert.assertFalse(s.exists("d", 0));
		Assert.assertTrue(s.exists("b", 1));

		// patch b-1
		Person subPerson = Person.create("b", 1, "Name B1", null, null);
		boolean patchSucceeded = s.patch(subPerson, Person.class);
		Assert.assertTrue(patchSucceeded);
		Person latestB1 = s.get("b", 1, 1, Person.class).get(0);
		Assert.assertEquals("Name B1", latestB1.name);
		Assert.assertEquals(20, latestB1.age.intValue());

		System.out.println("Running simple server-sync tests..");

		// sync non-existing user
		Assert.assertEquals(0, serverSync("NOBODY", 0L, null).events.size()); // nobody is associated with this user

		// sync our user
		long syncStart = 0L; // since the beginning of time
		SyncResults sync = serverSync("user1", syncStart, null);
		Assert.assertEquals(2, sync.events.size()); // only a0 and b1 are associated with the user

		s.setAssociation("user1", "a", false);
		s.setAssociation("user1", "c", 5, true);
		s.delete("c", 5);

		syncStart = sync.syncEnd;
		sync = serverSync("user1", syncStart, null);
//		System.out.println(sync.events);
		Assert.assertEquals(2, sync.events.size()); // a0 and c5 have been deleted for this user
		Assert.assertTrue(sync.events.get(0).changeType == ServerChangeType.DELETE);
		Assert.assertTrue(sync.events.get(1).changeType == ServerChangeType.DELETE);

		// re-run the same sync
		SyncResults sync2 = serverSync("user1", syncStart, null);
		Assert.assertEquals(sync.events, sync2.events);

		// / run the next sync
		syncStart = sync.syncEnd;
		sync = serverSync("user1", syncStart, null);
		Assert.assertEquals(0, sync.events.size()); // expected empty as we made no further changes

		System.out.println("Running simple client-sync tests..");

		SyncResult sr = clientSync(0L);
		Person b1 = c.get("b", 1, 1, Person.class).get(0);
		Assert.assertEquals(b1, latestB1);

		b1.name = "Client B1";
		c.put(b1);
		sr = clientSync(sr.nextSyncStart);
		Assert.assertEquals(0, sr.events.size()); // expected empty as we made no further changes

		System.out.println("--------------------------------------------------------");
	}

	public SyncResults serverSync(String userId, long syncStart, List<ClientSyncEvent> clientSEs)
	{
		sleep(NETWORK_LATENCY_MS);
		long syncEnd = System.currentTimeMillis() - DB_UPDATE_TIME_SAFETY_MS;
//		System.err.println("syncStart=" + syncStart + ", syncEnd=" + syncEnd);
//		if (syncStart >= syncEnd)
//			throw new RuntimeException("syncStart=" + syncStart + ", syncEnd=" + syncEnd);
		List<ServerSyncEvent> syncEntities = s.sync(userId, syncStart, syncEnd, clientSEs);
		return SyncResults.create(syncStart, syncEnd, syncEntities);
	}

	public SyncResult clientSync(long syncStart)
	{
		sleep(NETWORK_LATENCY_MS);
		SyncResult sr = c.sync(sp, "user1", syncStart);
		for (ServerSyncEvent event : sr.events)
			System.err.println(event);
		System.err.println("***************************************************");
		return sr;
	}

	public void sleep(int ms)
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
