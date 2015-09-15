package org.nem.deploy;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.thread.*;
import org.eclipse.jetty.webapp.Configuration;
import org.nem.core.metadata.*;
import org.nem.core.time.*;
import org.nem.core.utils.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.logging.*;

/**
 * Simple jetty bootstrapper using the Servlet API 3.x with programmatic configuration.
 */
public class CommonStarter {
	private static final Logger LOGGER = Logger.getLogger(CommonStarter.class.getName());

	/**
	 * The publicly available system time provider.
	 */
	public static final TimeProvider TIME_PROVIDER;

	/**
	 * The meta data (name, version,...) for this application.
	 */
	public static final ApplicationMetaData META_DATA;

	/**
	 * The common starter.
	 */
	public static final CommonStarter INSTANCE = new CommonStarter();

	private static final int IDLE_TIMEOUT = 30000;
	private static final int HTTPS_HEADER_SIZE = 8192;
	private static final int HTTPS_BUFFER_SIZE = 32768;
	private static final long ASYNC_SHUTDOWN_DELAY = 200;

	private static final Closeable FILE_LOCK_HANDLE;

	private AnnotationConfigApplicationContext appCtx;
	private NemConfigurationPolicy configurationPolicy;

	private CommonConfiguration configuration = new CommonConfiguration();
	private Server server;
	private Server websockServer;

	@WebListener
	private class ServiceContextListener implements ServletContextListener {
		@Override
		public void contextDestroyed(final ServletContextEvent event) {
			// nothing
		}

		@Override
		public void contextInitialized(final ServletContextEvent event) {
			// This is the replacement for the web.xml (new with Servlet 3.0)
			try {
				final AnnotationConfigWebApplicationContext webCtx = new AnnotationConfigWebApplicationContext();

				webCtx.register(configurationPolicy.getWebAppInitializerClass());
				webCtx.setParent(appCtx);

				final ServletContext context = event.getServletContext();

				final ServletRegistration.Dynamic dispatcher = context.addServlet("Spring MVC Dispatcher Servlet", new DispatcherServlet(webCtx));
				dispatcher.setLoadOnStartup(1);
				dispatcher.addMapping(String.format("%s%s", configuration.getApiContext(), "/*"));

				context.setInitParameter("contextClass", "org.springframework.web.context.support.AnnotationConfigWebApplicationContext");

				if (configuration.isNcc()) {
					createServlets(context);
				}

				if (configuration.useDosFilter()) {
					createDosFilter(context);
				}
			} catch (final Exception e) {
				throw new RuntimeException(String.format("Exception in contextInitialized: %s", e.toString()), e);
			}
		}
	}

	@WebListener
	private class WebsocketContextListener implements ServletContextListener {
		@Override
		public void contextInitialized(ServletContextEvent event) {
			try {
				final AnnotationConfigWebApplicationContext webCtx = new AnnotationConfigWebApplicationContext();

				webCtx.register(configurationPolicy.getWebAppWebsockInitializerClass());
				webCtx.setParent(appCtx);

				final ServletContext context = event.getServletContext();

				final ServletRegistration.Dynamic servlet = context.addServlet("FileServlet", configurationPolicy.getJarFileServletClass());
				servlet.setInitParameter("maxCacheSize", "0");
				servlet.addMapping("/static/*");
				servlet.setLoadOnStartup(1);

				final ServletRegistration.Dynamic dispatcher = context.addServlet("Spring Websocket Dispatcher Servlet", new DispatcherServlet(webCtx));
				dispatcher.addMapping(String.format("%s%s", "/w", "/*"));
				dispatcher.setLoadOnStartup(1);

				context.setInitParameter("contextClass", "org.springframework.web.context.support.AnnotationConfigWebApplicationContext");

				if (configuration.useDosFilter()) {
					createDosFilter(context);
				}
			} catch (final Exception e) {
				throw new RuntimeException(String.format("Exception in contextInitialized: %s", e.toString()), e);
			}
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
		}
	}

	static {
		// initialize logging before anything is logged; otherwise not all
		// settings will take effect
		final CommonConfiguration configuration = new CommonConfiguration();
		LoggingBootstrapper.bootstrap(configuration.getNemFolder());

		final File lockFile = Paths.get(
				configuration.getNemFolder(),
				configuration.getShortServerName().toLowerCase() + ".lock").toFile();
		FILE_LOCK_HANDLE = tryAcquireLock(lockFile);
		if (null == FILE_LOCK_HANDLE) {
			LOGGER.warning("Could not acquire exclusive lock to lock file");
		}

		// make sure to initialize these after bootstrapping logging as some of them write to the log
		TIME_PROVIDER = new SystemTimeProvider();
		META_DATA = MetaDataFactory.loadApplicationMetaData(CommonStarter.class, TIME_PROVIDER);
	}

	private static Closeable tryAcquireLock(final File lockFile) {
		LOGGER.info(String.format("Acquiring exclusive lock to lock file: %s", lockFile));
		return LockFile.tryAcquireLock(lockFile);
	}

	public static void main(final String[] args) {
		LOGGER.info("Starting embedded Jetty Server.");
		try {
			INSTANCE.boot(args);
			//INSTANCE.server.join();
			INSTANCE.websockServer.join();
		} catch (final InterruptedException e) {
			LOGGER.log(Level.INFO, "Received signal to shutdown.");
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Stopping Jetty Server.", e);
		} finally {
			// Last chance to save configuration
			LOGGER.info(String.format("%s %s shutdown...", META_DATA.getAppName(), META_DATA.getVersion()));
		}

		System.exit(1);
	}

	private void initializeConfigurationPolicy() {
		this.appCtx = new AnnotationConfigApplicationContext("org.nem.specific.deploy.appconfig");
		this.configurationPolicy = this.appCtx.getBean(NemConfigurationPolicy.class);
	}

	private org.eclipse.jetty.server.Handler createHandlers() {
		final HandlerCollection handlers = new HandlerCollection();
		final ServletContextHandler servletContext = new ServletContextHandler();

		// Special Listener to set-up the environment for Spring
		servletContext.addEventListener(new ServiceContextListener());
		servletContext.addEventListener(new ContextLoaderListener());
		servletContext.setErrorHandler(new JsonErrorHandler(TIME_PROVIDER));

		handlers.setHandlers(new org.eclipse.jetty.server.Handler[] { servletContext });

		return handlers;
	}

	private Server createServer() {
		// Taken from Jetty doc
		final QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(this.configuration.getMaxThreads());
		final Server server = new Server(threadPool);
		server.addBean(new ScheduledExecutorScheduler());

		if (this.configuration.isNcc()) {
			final Configuration.ClassList classList = Configuration.ClassList.setServerDefault(server);
			classList.addAfter(
					"org.eclipse.jetty.webapp.FragmentConfiguration",
					"org.eclipse.jetty.plus.webapp.EnvConfiguration",
					"org.eclipse.jetty.plus.webapp.PlusConfiguration");
			classList.addBefore(
					"org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
					"org.eclipse.jetty.annotations.AnnotationConfiguration");
		}

		return server;
	}

	private Connector createConnector(final Server server) {
		final HttpConfiguration http_config = new HttpConfiguration();
		http_config.setSecureScheme("https");
		http_config.setSecurePort(this.configuration.getHttpsPort());
		http_config.setOutputBufferSize(HTTPS_BUFFER_SIZE);
		http_config.setRequestHeaderSize(HTTPS_HEADER_SIZE);
		http_config.setResponseHeaderSize(HTTPS_HEADER_SIZE);
		http_config.setSendServerVersion(true);
		http_config.setSendDateHeader(false);

		final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
		http.setPort(this.configuration.getHttpPort());
		http.setIdleTimeout(IDLE_TIMEOUT);
		return http;
	}

	private org.eclipse.jetty.server.Handler createWebsocketHandlers() {
		final HandlerCollection handlers = new HandlerCollection();
		final ServletContextHandler servletContext = new ServletContextHandler();

		// Special Listener to set-up the environment for Spring
		servletContext.addEventListener(new WebsocketContextListener());
		servletContext.addEventListener(new ContextLoaderListener());
		servletContext.setErrorHandler(new JsonErrorHandler(TIME_PROVIDER));

		handlers.setHandlers(new org.eclipse.jetty.server.Handler[] { servletContext });

		return handlers;
	}

	private Connector createWebsocketConnector(final Server server) {
		final ServerConnector http = new ServerConnector(server);
		http.setPort(7777);
		http.setIdleTimeout(IDLE_TIMEOUT);
		return http;
	}

	private void startServer(final Server server, final URL stopURL) throws Exception {
		try {
			server.start();
		} catch (final MultiException e) {
			final long bindExceptions = e.getThrowables().stream().filter(t -> t instanceof BindException).count();

			if (bindExceptions > 0) {
				LOGGER.log(Level.WARNING, "Port already used, trying to shutdown other instance");
				// We assume it is already running?
				// Kill the old one
				this.stopOtherInstance(stopURL);

				// One more try
				LOGGER.log(Level.WARNING, "Re-trying to start server.");
				server.start();
			} else {
				LOGGER.log(Level.SEVERE, "Could not start server.", e);
				return;
			}
		}
		LOGGER.info(String.format("%s is ready to serve. URL is \"%s\".", CommonStarter.META_DATA.getAppName(), server.getURI()));
	}

	/**
	 * Stops the server synchronously on the current thread.
	 */
	public void stopServer() {
		try {
			this.server.stop();
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Can't stop server.", e);
		}
	}

	/**
	 * Stops the server asynchronously on a different thread.
	 *
	 * @return A future that will evaluate to true on success and false on failure.
	 */
	public CompletableFuture<Boolean> stopServerAsync() {
		LOGGER.info(String.format("Async shut-down initiated in %d msec.", ASYNC_SHUTDOWN_DELAY));

		final CompletableFuture<Boolean> future = new CompletableFuture<>();
		final Thread thread = new Thread(() -> {
			try {
				ExceptionUtils.propagateVoid(() -> Thread.sleep(ASYNC_SHUTDOWN_DELAY));
				this.stopServer();
				future.complete(true);
			} finally {
				future.complete(false);
			}
		});
		thread.start();
		return future;
	}

	private void stopOtherInstance(final URL stopURL) throws Exception {
		final HttpClient httpClient = new HttpClient();
		try {
			httpClient.start();
			LOGGER.info("Send shutdown to other instance...");
			final ContentResponse response = httpClient.GET(stopURL.toURI());
			if (response.getStatus() != HttpStatus.OK.value()) {
				LOGGER.info(String.format("Other instance returned %d: %s", response.getStatus(), response.getContentAsString()));
			} else {
				LOGGER.info("Pause 2 seconds");
				Thread.sleep(2000);
			}
		} finally {
			httpClient.stop();
		}
	}

	private void boot(final String[] args) throws Exception {
		this.initializeConfigurationPolicy();
		this.configuration = this.configurationPolicy.loadConfig(args);

		this.server = this.createServer();
		this.server.addBean(new ScheduledExecutorScheduler());
		this.server.addConnector(this.createConnector(this.server));
		this.server.setHandler(this.createHandlers());
		this.server.setDumpAfterStart(false);
		this.server.setDumpBeforeStop(false);
		this.server.setStopAtShutdown(true);

		if (this.configuration.isNcc()) {
			this.startWebApplication(this.server);
		}

		this.websockServer = this.createServer();
		this.websockServer.addBean(new ScheduledExecutorScheduler());
		this.websockServer.addConnector(this.createWebsocketConnector(this.websockServer));
		this.websockServer.setHandler(this.createWebsocketHandlers());
		this.websockServer.setDumpAfterStart(false);
		this.websockServer.setDumpBeforeStop(false);
		this.websockServer.setStopAtShutdown(true);

		LOGGER.info("Calling websocket start().");
		this.startServer(this.websockServer, new URL(this.configuration.getShutdownUrl()));

		LOGGER.info("Calling start().");
		this.startServer(this.server, new URL(this.configuration.getShutdownUrl()));
	}

	private void startWebApplication(final Server server) {
		final HandlerCollection handlers = new HandlerCollection();
		final ServletContextHandler servletContext = new ServletContextHandler();

		// Special Listener to set-up the environment for Spring
		// TODO 2015/09/13 G-*: I'm not sure why the first one (one with this) is included here...
		//servletContext.addEventListener(this);
		servletContext.addEventListener(new ContextLoaderListener());
		servletContext.setErrorHandler(new JsonErrorHandler(TIME_PROVIDER));

		handlers.setHandlers(new Handler[] { servletContext });
		server.setHandler(handlers);
	}

	private void createServlets(final ServletContext context) {
		ServletRegistration.Dynamic servlet = context.addServlet("FileServlet", this.configurationPolicy.getJarFileServletClass());
		servlet.setInitParameter("maxCacheSize", "0");
		servlet.addMapping(String.format("%s%s", this.configuration.getWebContext(), "/*"));
		servlet.setLoadOnStartup(1);

		servlet = context.addServlet("DefaultServlet", this.configurationPolicy.getDefaultServletClass());
		servlet.addMapping("/");
		servlet.setLoadOnStartup(1);
	}

	private void createDosFilter(final ServletContext context) {
		javax.servlet.FilterRegistration.Dynamic dosFilter = context.addFilter("DoSFilter", "org.eclipse.jetty.servlets.DoSFilter");
		dosFilter.setInitParameter("maxRequestsPerSec", "50");
		dosFilter.setInitParameter("delayMs", "-1");
		dosFilter.setInitParameter("trackSessions", "false");
		dosFilter.setInitParameter("maxRequestMs", "120000");
		dosFilter.setInitParameter("ipWhitelist", "127.0.0.1");
		dosFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

		// GZIP filter
		dosFilter = context.addFilter("GzipFilter", "org.eclipse.jetty.servlets.GzipFilter");
		// Zipping following MimeTypes
		dosFilter.setInitParameter("mimeTypes", MimeTypes.Type.APPLICATION_JSON.asString());
		dosFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
	}
}
