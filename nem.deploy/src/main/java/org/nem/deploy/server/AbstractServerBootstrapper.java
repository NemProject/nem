package org.nem.deploy.server;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.*;
import org.eclipse.jetty.webapp.Configuration;
import org.nem.deploy.*;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContextListener;

/**
 * Abstract class for booting a server.
 */
public abstract class AbstractServerBootstrapper {
	private static final int IDLE_TIMEOUT = 30000;

	private final CommonConfiguration configuration;

	/**
	 * Creates a bootstrapper.
	 *
	 * @param configuration The configuration.
	 */
	protected AbstractServerBootstrapper(final CommonConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Gets the configuration.
	 *
	 * @return The configuration.
	 */
	protected CommonConfiguration getConfiguration() {
		return this.configuration;
	}

	/**
	 * Boots the server.
	 *
	 * @return The server.
	 */
	public Server boot() {
		final Server server = this.createServer();
		server.addBean(new ScheduledExecutorScheduler());

		final ServerConnector connector = this.createConnector(server);
		connector.setIdleTimeout(IDLE_TIMEOUT);
		server.addConnector(connector);

		server.setHandler(this.createHandlers());
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		server.setStopAtShutdown(true);
		return server;
	}

	private Server createServer() {
		// Taken from Jetty doc
		final QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(this.configuration.getMaxThreads());
		final Server server = new Server(threadPool);
		server.addBean(new ScheduledExecutorScheduler());

		if (this.configuration.isNcc()) {
			final Configuration.ClassList classList = Configuration.ClassList.setServerDefault(server);
			classList.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration", "org.eclipse.jetty.plus.webapp.EnvConfiguration",
					"org.eclipse.jetty.plus.webapp.PlusConfiguration");
			classList.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
					"org.eclipse.jetty.annotations.AnnotationConfiguration");
		}

		return server;
	}

	private Handler createHandlers() {
		final ServletContextHandler servletContext = new ServletContextHandler();

		// Special Listener to set-up the environment for Spring
		servletContext.addEventListener(this.getCustomServletListener());
		servletContext.addEventListener(new ContextLoaderListener());
		servletContext.setErrorHandler(new JsonErrorHandler(CommonStarter.TIME_PROVIDER));

		final HandlerCollection handlers = new HandlerCollection();
		handlers.setHandlers(new org.eclipse.jetty.server.Handler[]{
				servletContext
		});
		return handlers;
	}

	/**
	 * Creates a server connector.
	 *
	 * @param server The server.
	 * @return The server connector.
	 */
	protected abstract ServerConnector createConnector(final Server server);

	/**
	 * Gets the (optional) custom servlet listener.
	 *
	 * @return The custom servlet listener.
	 */
	protected abstract ServletContextListener getCustomServletListener();
}
