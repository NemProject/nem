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
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.logging.*;

/**
 * Simple jetty bootstrapper using the Servlet API 3.x with programmatic configuration.
 *
 * TODO-CR 20140811 - is spring able to inject dependencies into the CommonStarter (instance) constructor?
 * if so, we can inject a NIS / NCC policy class (that exposes the classes for instance) and avoid string -> class transformations
 * if not, what is here is fine
 */
@WebListener
public class CommonStarter implements ServletContextListener {
	private static final Logger LOGGER = Logger.getLogger(CommonStarter.class.getName());

	// TODO-CR 20140811: comment publics
	public static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();
	public static final ApplicationMetaData META_DATA = MetaDataFactory.loadApplicationMetaData(CommonStarter.class, TIME_PROVIDER);
	public static final CommonStarter INSTANCE = new CommonStarter();

	private static final int IDLE_TIMEOUT = 30000;
	private static final int HTTPS_HEADER_SIZE = 8192;
	private static final int HTTPS_BUFFER_SIZE = 32768;

	private Server server;
	private static CommonConfiguration configuration;

	static {
		// initialize logging before anything is logged; otherwise not all
		// settings will take effect
		loadConfigurationProperties();
		initializeLogging();
	}

	private static void loadConfigurationProperties() {
		ExceptionUtils.propagate(() -> {
			// TODO-CR 20140811: don't access static member via instance
			INSTANCE.configuration = new CommonConfiguration();
			return configuration;
		}, IllegalStateException::new);
	}

	public static void main(final String[] args) {
		LOGGER.info("Starting embedded Jetty Server.");

		try {
			INSTANCE.boot();
			INSTANCE.server.join();
		} catch (final InterruptedException e) {
			LOGGER.log(Level.INFO, "Received signal to shutdown.");
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Stopping Jetty Server.", e);
		} finally {
			// Last chance to save configuration
			LOGGER.info(String.format("%s %s shutdown...", CommonStarter.META_DATA.getAppName(), CommonStarter.META_DATA.getVersion()));
		}

		System.exit(1);
	}

	private static void initializeLogging() {
		try (final InputStream inputStream = CommonStarter.class.getClassLoader().getResourceAsStream("logalpha.properties")) {

			// TODO-CR - should this stream be in a try block?
			final InputStream inputStringStream = adaptFileLocation(inputStream);
			final LogManager logManager = LogManager.getLogManager();
			logManager.readConfiguration(inputStringStream);

			final File logFile = new File(logManager.getProperty("java.util.logging.FileHandler.pattern"));
			final File logDirectory = new File(logFile.getParent());
			if (!logDirectory.exists() && !logDirectory.mkdirs()) {
				throw new IOException(String.format("unable to create log directory %s", logDirectory));
			}
		} catch (final IOException e) {
			LOGGER.severe("Could not load default logging properties file");
			LOGGER.severe(e.getMessage());
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
		final String nemFolder = INSTANCE.configuration.getNemFolder();
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

	private Connector createConnector(Server server) {
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
				this.configuration.getBaseUrl()));
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
		this.server = createServer();
		this.server.addBean(new ScheduledExecutorScheduler());
		this.server.addConnector(createConnector(server));
		this.server.setHandler(createHandlers());
		this.server.setDumpAfterStart(false);
		this.server.setDumpBeforeStop(false);
		this.server.setStopAtShutdown(true);

		if (this.configuration.isNcc()) {
			this.startWebApplication(this.server);
		}

		LOGGER.info("Calling start().");
		startServer(this.server, new URL(this.configuration.getShutdownUrl()));

		if (this.configuration.isNcc()) {
			this.getMethod("org.nem.deploy.WebStartProxy", "openWebBrowser", new Class[]{ String.class })
					.invoke(null, this.configuration.getHomeUrl());

			if (this.configuration.isWebStart()) {
				this.getMethod("org.nem.ncc.connector.NISController", "startNISViaWebStart", new Class[]{ String.class })
						.invoke(null, this.configuration.getNisJnlpUrl());
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

		handlers.setHandlers(new Handler[]{ servletContext });
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
			final String appConfigClassName = String.format("%s%s%s", "org.nem.deploy.", this.configuration.getShortServerName(), "AppConfig");
			final String webAppInitializerClassName = String.format("%s%s%s", "org.nem.deploy.", this.configuration.getShortServerName(), "WebAppInitializer");
			final Class appConfigClass = getClass(appConfigClassName);
			final Class appWebAppInitializerClass = getClass(webAppInitializerClassName);
			final AnnotationConfigApplicationContext appCtx = new AnnotationConfigApplicationContext(appConfigClass);
			final AnnotationConfigWebApplicationContext webCtx = new AnnotationConfigWebApplicationContext();
			webCtx.register(appWebAppInitializerClass);
			webCtx.setParent(appCtx);

			final ServletContext context = event.getServletContext();
			ServletRegistration.Dynamic dispatcher = context.addServlet("Spring MVC Dispatcher Servlet", new DispatcherServlet(webCtx));
			dispatcher.setLoadOnStartup(1);
			dispatcher.addMapping(String.format("%s%s", this.configuration.getApiContext(), "/*"));

			context.setInitParameter("contextClass", "org.springframework.web.context.support.AnnotationConfigWebApplicationContext");

			// TODO-CR - can you move this to separate function
			if (this.configuration.isNcc()) {
				ServletRegistration.Dynamic servlet = context.addServlet("FileServlet", "org.nem.ncc.web.servlet.JarFileServlet");
				servlet.setInitParameter("maxCacheSize", "0");
				servlet.addMapping(String.format("%s%s", this.configuration.getWebContext(), "/*"));
				servlet.setLoadOnStartup(1);

				servlet = context.addServlet("DefaultServlet", "org.nem.ncc.web.servlet.NccDefaultServlet");
				servlet.addMapping("/");
				servlet.setLoadOnStartup(1);
			}

			// TODO-CR - can you move this to separate function
			if (this.configuration.useDosFilter()) {
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
		} catch (final Exception e) {
			//TODO-CR: pass e to runtime exception
			throw new RuntimeException("Exception in contextInitialized");
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Class getClass(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(String.format("Class %s not found", className));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Method getMethod(String className, String methodName, Class<?>[] parameterTypes) {
		try {
			Class cls = Class.forName(className);
			return cls.getMethod(methodName, parameterTypes);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(String.format("Class %s not found", className));
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(String.format("Method %s not found", methodName));
		}
	}
}
