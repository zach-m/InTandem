package tectonica.test.intandem;

import org.junit.BeforeClass;

import tectonica.intandem.impl.jdbc.sqlite.SqliteClientAccessor;
import tectonica.intandem.impl.jdbc.sqlite.SqliteServerAccessor;

public class TestSqlite extends BaseJdbcTest
{
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		s = new SqliteServerAccessor();
		c = new SqliteClientAccessor();
	}
}
