package tectonica.test.intandem;

import org.junit.BeforeClass;

import tectonica.intandem.impl.jdbc.sqlite.SqliteClientAccessor;
import tectonica.intandem.impl.jdbc.sqlite.SqliteServerAccessor;

public class TestSqlite extends BaseJdbcTest
{
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		String connStrBase = "jdbc:sqlite:" + TestSqlite.class.getResource("/").getPath();
		s = new SqliteServerAccessor(connStrBase + "server.db");
		c = new SqliteClientAccessor(connStrBase + "client.db");
	}
}
