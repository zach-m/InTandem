package tectonica.test.intandem;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import tectonica.intandem.framework.client.ClientAccessor;
import tectonica.intandem.framework.client.ClientAccessor.SyncResult;
import tectonica.intandem.framework.server.ServerAccessor;
import tectonica.intandem.framework.transport.ServerAccessorProxy;
import tectonica.intandem.impl.jdbc.h2.H2ClientAccessor;
import tectonica.intandem.impl.jdbc.h2.H2ServerAccessor;
import tectonica.intandem.transport.ClientCommunicator;
import tectonica.test.intandem.model.Person;
import tectonica.test.intandem.server.TestJetty;

public class TestTransport
{
	private static final int NETWORK_LATENCY_MS = 100;

	protected static ClientAccessor c;
	public static ServerAccessor s;
	protected static ServerAccessorProxy sp;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
//		String connStrBase = "jdbc:sqlite:" + TestTransport.class.getResource("/").getPath();
//		s = new SqliteServerAccessor(connStrBase + "server.db");
//		c = new SqliteClientAccessor(connStrBase + "client.db");

		c = new H2ClientAccessor("jdbc:h2:mem:client", "sa", "sa");
		s = new H2ServerAccessor("jdbc:h2:mem:server", "sa", "sa");

		TestJetty.startServer(8888, TestServlet.class, "/sync");

		sp = new ClientCommunicator();
		sp.setUrl("http://localhost:8888/sync");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		c.cleanup();
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
		c.put(Person.create("a", 0, "Name A", 10, 1.10));
		c.put(Person.create("b", 1, "Wrong", 0, 0.0)); // putting a wrong name under b-1
		c.put(Person.create("c", 5, "Name C", 30, 1.30));
		c.put(Person.create("b", 1, "WrongAgain", 20, 1.20)); // replacing the wrong name under b-1 with another wrong name
		c.put(Person.create("b", 2, "Name B2", 20, 1.20));
		c.put(Person.create("b", 3, "Name B3", 20, 1.20));

		SyncResult sr = clientSync(0L);
		Person b1 = c.get("b", 1, 1, Person.class).get(0);
//		Assert.assertEquals(b1, latestB1);

		b1.name = "Client B1";
		c.put(b1);
		sr = clientSync(sr.nextSyncStart);
		Assert.assertEquals(0, sr.events.size()); // expected empty as we made no further changes

		System.out.println("--------------------------------------------------------");
	}

	public SyncResult clientSync(long syncStart)
	{
		sleep(NETWORK_LATENCY_MS);
		SyncResult sr = c.sync(sp, "user1", syncStart);
//		for (ServerSyncEvent event : sr.events)
//			System.err.println(event);
//		System.err.println("***************************************************");
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
