package tectonica.test.intandem;

import org.junit.BeforeClass;

import tectonica.intandem.impl.jdbc.h2.H2ClientAccessor;
import tectonica.intandem.impl.jdbc.h2.H2ServerAccessor;

public class TestH2 extends BaseJdbcTest
{
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		String connStrBase = "jdbc:h2:mem:";
		s = new H2ServerAccessor(connStrBase + "server", "sa", "sa");
		c = new H2ClientAccessor(connStrBase + "client", "sa", "sa");
		initProxy();
	}
}
