package org.nem.core.deploy;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.*;
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
import java.util.*;
import java.util.logging.*;

/**
 * Simple jetty bootstrapper using the Servlet API 3.x with programmatic configuration.
 */
@WebListener
public class CommonStarter implements ServletContextListener {
	private static final Logger LOGGER = Logger.getLogger(CommonStarter.class.getName());

	/**
	 * The publicly available system time provider.
	 */
	public static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();

	/**
	 * The meta data (name, version,...) for this application.
	 */
	public static final ApplicationMetaData META_DATA = MetaDataFactory.loadApplicationMetaData(CommonStarter.class, TIME_PROVIDER);

	/**
	 * The common starter.
	 */
	public static CommonStarter INSTANCE = new CommonStarter();

	private static final int IDLE_TIMEOUT = 30000;
	private static final int HTTPS_HEADER_SIZE = 8192;
	private static final int HTTPS_BUFFER_SIZE = 32768;

	private static AnnotationConfigApplicationContext appCtx;
	private static NemConfigurationPolicy configurationPolicy;
	private static CommonConfiguration configuration;
	private Server server;

	static {
		// initialize logging before anything is logged; otherwise not all
		// settings will take effect
		loadCommonConfiguration();
		initializeLogging();
	}

	private static void loadCommonConfiguration() {
		ExceptionUtils.propagate(() -> {
			configuration = new CommonConfiguration();
			return configuration;
		}, IllegalStateException::new);
	}

	public static void main(final String[] args) {
		LOGGER.info("Starting embedded Jetty Server.");
		initializeConfigurationPolicy();
		try {
			INSTANCE.boot();
			INSTANCE.server.join();
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

	private static void initializeConfigurationPolicy() {
		appCtx = new AnnotationConfigApplicationContext("org.nem.deploy.appconfig");
		configurationPolicy = appCtx.getBean(NemConfigurationPolicy.class);
	}

	private static void initializeLogging() {
		try (final InputStream inputStream = CommonStarter.class.getClassLoader().getResourceAsStream("logalpha.properties");
			 final InputStream inputStringStream = adaptFileLocation(inputStream)) {
			final LogManager logManager = LogManager.getLogManager();
			logManager.readConfiguration(inputStringStream);
			final File logFile = new File(logManager.getProperty("java.util.logging.FileHandler.pattern"));
			final File logDirectory = new File(logFile.getParent());
			if (!logDirectory.exists() && !logDirectory.mkdirs()) {
				throw new IOException(String.format("unable to create log directory %s", logDirectory));
			}
		} catch (final IOException e) {
			LOGGER.severe("Could not load default logging properties file");
			LOGGER.severe(e.toString());
		}
	}

	/**
	 * log configuration may include a placeholder for the nem folder The method replaces the pattern ${nemFolder} with the value defined within the
	 * NisConfiguration Only for "java.util.logging.FileHandler.pattern" value
	 *
	 * @param inputStream stream of the logging properties
	 * @return new stream which contains the replaced FileHandler.pattern
	 * @throws IOException
	 */
	private static InputStream adaptFileLocation(final InputStream inputStream) throws IOException {
		final Properties props = new Properties();
		props.load(inputStream);
		String tmpStr = props.getProperty("java.util.logging.FileHandler.pattern");
		final String nemFolder = configuration.getNemFolder();
		tmpStr = tmpStr.replace("${nemFolder}", nemFolder);
		props.setProperty("java.util.logging.FileHandler.pattern", tmpStr);
		final StringWriter stringWriter = new StringWriter();
		props.store(stringWriter, null);
		return new ByteArrayInputStream(StringEncoder.getBytes(stringWriter.toString()));
	}

	private org.eclipse.jetty.server.Handler createHandlers() {
		final HandlerCollection handlers = new HandlerCollection();
		final ServletContextHandler servletContext = new ServletContextHandler();

		// Special Listener to set-up the environment for Spring
		servletContext.addEventListener(this);
		servletContext.addEventListener(new ContextLoaderListener());
		servletContext.setErrorHandler(new JsonErrorHandler(TIME_PROVIDER));

		handlers.setHandlers(new org.eclipse.jetty.server.Handler[] { servletContext });

		return handlers;
	}

	private Server createServer() {
		// Taken from Jetty doc
		final QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(configuration.getMaxThreads());
		final Server server = new Server(threadPool);
		server.addBean(new ScheduledExecutorScheduler());

		if (configuration.isNcc()) {
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
		http_config.setSecurePort(configuration.getHttpsPort());
		http_config.setOutputBufferSize(HTTPS_BUFFER_SIZE);
		http_config.setRequestHeaderSize(HTTPS_HEADER_SIZE);
		http_config.setResponseHeaderSize(HTTPS_HEADER_SIZE);
		http_config.setSendServerVersion(true);
		http_config.setSendDateHeader(false);

		final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
		http.setPort(configuration.getHttpPort());
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
		LOGGER.info(String.format("%s is ready to serve. URL is \"%s\".",
				CommonStarter.META_DATA.getAppName(),
				configuration.getBaseUrl()));
	}

	public void stopServer() {
		try {
			this.server.stop();
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Can't stop server.", e);
		}
	}

	private void stopOtherInstance(final URL stopURL) throws Exception {
		final HttpClient httpClient = new HttpClient();
		try {
			httpClient.start();
			LOGGER.info("Send shutdown to other instance...");
			final ContentResponse response = httpClient.GET(stopURL.toURI());
			if (response.getStatus() != HttpStatus.OK_200) {
				LOGGER.info(String.format("Other instance returned %d: %s", response.getStatus(), response.getContentAsString()));
			} else {
				LOGGER.info("Pause 2 seconds");
				Thread.sleep(2000);
			}
		} finally {
			httpClient.stop();
		}
	}

	private void boot() throws Exception {
		this.server = this.createServer();
		this.server.addBean(new ScheduledExecutorScheduler());
		this.server.addConnector(this.createConnector(this.server));
		this.server.setHandler(this.createHandlers());
		this.server.setDumpAfterStart(false);
		this.server.setDumpBeforeStop(false);
		this.server.setStopAtShutdown(true);

		if (configuration.isNcc()) {
			this.startWebApplication(this.server);
		}

		LOGGER.info("Calling start().");
		this.startServer(this.server, new URL(configuration.getShutdownUrl()));

		if (configuration.isNcc()) {
			configurationPolicy.openWebBrowser(configuration.getHomeUrl());

			if (configuration.isWebStart()) {
				configurationPolicy.startNisViaWebStart(configuration.getNisJnlpUrl());
			}
		}
	}

	private void startWebApplication(final Server server) {
		final HandlerCollection handlers = new HandlerCollection();
		final ServletContextHandler servletContext = new ServletContextHandler();

		// Special Listener to set-up the environment for Spring
		servletContext.addEventListener(this);
		servletContext.addEventListener(new ContextLoaderListener());
		servletContext.setErrorHandler(new JsonErrorHandler(TIME_PROVIDER));

		handlers.setHandlers(new Handler[] { servletContext });
		server.setHandler(handlers);
	}

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
				this.createServlets(context);
			}

			if (configuration.useDosFilter()) {
				this.createDosFilter(context);
			}
		} catch (final Exception e) {
			throw new RuntimeException(String.format("Exception in contextInitialized: %s", e.toString()), e);
		}
	}

	private void createServlets(final ServletContext context) {
		ServletRegistration.Dynamic servlet = context.addServlet("FileServlet", configurationPolicy.getJarFileServletClass());
		servlet.setInitParameter("maxCacheSize", "0");
		servlet.addMapping(String.format("%s%s", configuration.getWebContext(), "/*"));
		servlet.setLoadOnStartup(1);

		servlet = context.addServlet("DefaultServlet", configurationPolicy.getDefaultServletClass());
		servlet.addMapping("/");
		servlet.setLoadOnStartup(1);
	}

	private void createDosFilter(final ServletContext context) {
		javax.servlet.FilterRegistration.Dynamic dosFilter = context.addFilter("DoSFilter", "org.eclipse.jetty.servlets.DoSFilter");
		dosFilter.setInitParameter("delayMs", "1000");
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
