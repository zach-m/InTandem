package tectonica.test.intandem.server;

import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.BaseHolder.Source;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class TestJetty
{
	private static Server server;

	public static <T extends HttpServlet> void startServer(int listenPort, Class<T> servletClass, String servletUrl) throws Exception
	{
		server = new Server(listenPort);
		server.setHandler(createContext("/", servletClass.getName(), servletUrl));

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				if (server.isStarted())
				{
					((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).info(">>> ShutdownHook");
					stopServer();
				}
			}
		});

		server.start();
	}

	public static void stopServer()
	{
		try
		{
			server.stop();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static ServletContextHandler createContext(String contextPath, String servletClassName, String servletUrl)
	{
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath(contextPath);

		ServletHandler servlets = context.getServletHandler();

		// register a Bootstrap servlet
		ServletHolder holder = servlets.newServletHolder(Source.EMBEDDED);
		holder.setClassName(servletClassName);
		holder.setInitOrder(0);
		servlets.addServlet(holder);
		servlets.addServletWithMapping(holder, servletUrl + "/*");

		return context;
	}

	public static void waitForTermination()
	{
		try
		{
			server.join();
		}
		catch (InterruptedException e)
		{
			System.err.println("waitForTermination() was interupted");
		}
	}

	public static Server getServer()
	{
		return server;
	}
}
