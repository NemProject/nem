package org.nem.deploy;

import java.io.*;
import java.util.EnumSet;
import java.util.Properties;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.nem.core.metadata.*;
import org.nem.core.time.*;
import org.nem.core.utils.StringEncoder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Did not find a better way of launching Jetty in combination with WebStart.
 * The physical location of the downloaded files is not pre-known, so passing a
 * WAR file to the Jetty runner does not work.
 * <p/>
 * I had to switch to the Servlet API 3.x with programmatic configuration.
 */

@WebListener
public class CommonStarter implements ServletContextListener {
	private static final Logger LOGGER = Logger.getLogger(CommonStarter.class.getName());

	static {
		// initialize logging before anything is logged; otherwise not all
		// settings will take effect
		initializeLogging();
	}

	/**
	 * The time provider.
	 */
	public static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();

	/**
	 * The application meta data.
	 */
	public static final ApplicationMetaData META_DATA = MetaDataFactory.loadApplicationMetaData(CommonStarter.class, TIME_PROVIDER);

	public static void main(String[] args) throws Exception {
		LOGGER.info("Starting embedded Jetty Server.");

		// https://code.google.com/p/json-smart/wiki/ParserConfiguration
		// JSONParser.DEFAULT_PERMISSIVE_MODE = JSONParser.MODE_JSON_SIMPLE;

		// Taken from Jetty doc
		Server server = new Server(createThreadPool());
		server.addBean(new ScheduledExecutorScheduler());

		server.addConnector(createConnector(server));
		server.setHandler(createHandlers());

		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		server.setStopAtShutdown(true);

		NisApp.INSTANCE.setServer(server);
		NisApp.INSTANCE.showWindow();

		LOGGER.info("Calling start().");
		server.start();
		server.join();

		LOGGER.info(String.format("%s %s shutdown...", CommonStarter.META_DATA.getAppName(), CommonStarter.META_DATA.getVersion()));

		System.exit(1);
	}

	private static void initializeLogging() {
		try (final InputStream inputStream = CommonStarter.class.getClassLoader().getResourceAsStream("logalpha.properties")) {

			final LogManager logManager = LogManager.getLogManager();
			InputStream inputStringStream = adaptFileLocation(inputStream);
			logManager.readConfiguration(inputStringStream);

			final File logFile = new File(logManager.getProperty("java.util.logging.FileHandler.pattern"));
			final File logDirectory = new File(logFile.getParent());
			if (!logDirectory.exists() && !logDirectory.mkdirs())
				throw new IOException(String.format("unable to create log directory %s", logDirectory));
		} catch (final IOException e) {
			LOGGER.severe("Could not load default logging properties file");
			LOGGER.severe(e.getMessage());
		}
	}

	/**
	 * log configuration may include a placeholder for the nem folder
	 * The method replaces the pattern ${nemFolder} with the value defined within the
	 * NisConfiguration
	 * Only for "java.util.logging.FileHandler.pattern" value
	 *  
	 * @param inputStream stream of the logging properties
	 * @return new stream which contains the replaced FileHandler.pattern
	 * @throws IOException
	 */
	private static InputStream adaptFileLocation(InputStream inputStream) throws IOException {
		final Properties props = new Properties();
		final NisConfiguration configuration = new NisConfiguration();
		props.load(inputStream);
		String tmpStr = props.getProperty("java.util.logging.FileHandler.pattern");
		tmpStr = tmpStr.replace("${nemFolder}", configuration.getNemFolder());
		props.setProperty("java.util.logging.FileHandler.pattern", tmpStr);
		StringWriter strWriter = new StringWriter();
		props.store(strWriter, null);

		return new ByteArrayInputStream(StringEncoder.getBytes(strWriter.toString()));
	}

	private static Handler createHandlers() {
		HandlerCollection handlers = new HandlerCollection();
		ServletContextHandler servletContext = new ServletContextHandler();

		// Special Listener to set-up the environment for Spring
		servletContext.addEventListener(new CommonStarter());
		servletContext.addEventListener(new ContextLoaderListener());
		servletContext.setErrorHandler(new JsonErrorHandler());

		handlers.setHandlers(new Handler[] { servletContext });

		return handlers;
	}

	public static Connector createConnector(Server server) {
		HttpConfiguration http_config = new HttpConfiguration();
		http_config.setSecureScheme("https");
		// PORT
		http_config.setSecurePort(7891);
		http_config.setOutputBufferSize(32768);
		http_config.setRequestHeaderSize(8192);
		http_config.setResponseHeaderSize(8192);
		http_config.setSendServerVersion(true);
		http_config.setSendDateHeader(false);

		ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
		http.setPort(7890);
		http.setIdleTimeout(30000);
		return http;
	}

	public static QueuedThreadPool createThreadPool() {
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(500);
		return threadPool;
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// nothing
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		// This is the replacement for the web.xml
		// New with Servlet 3.0
		AnnotationConfigApplicationContext appCtx = new AnnotationConfigApplicationContext(NisAppConfig.class);

		AnnotationConfigWebApplicationContext webCtx = new AnnotationConfigWebApplicationContext();
		webCtx.register(NisWebAppInitializer.class);
		webCtx.setParent(appCtx);

		ServletContext context = event.getServletContext();
		ServletRegistration.Dynamic dispatcher = context.addServlet("Spring MVC Dispatcher Servlet", new DispatcherServlet(webCtx));
		dispatcher.setLoadOnStartup(1);
		dispatcher.addMapping("/");

		context.setInitParameter("contextClass", "org.springframework.web.context.support.AnnotationConfigWebApplicationContext");

		// Denial of server Filter
		javax.servlet.FilterRegistration.Dynamic dosFilter = context.addFilter("DoSFilter", "org.eclipse.jetty.servlets.DoSFilter");
		dosFilter.setInitParameter("delayMs", "1000");
		dosFilter.setInitParameter("trackSessions", "false");
		dosFilter.setInitParameter("maxRequestMs", "120000"); // 2 minutes
		dosFilter.setInitParameter("ipWhitelist", "127.0.0.1");
		dosFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

		// GZIP filter
		dosFilter = context.addFilter("GzipFilter", "org.eclipse.jetty.servlets.GzipFilter");
		// Zipping following MimeTypes
		dosFilter.setInitParameter("mimeTypes", MimeTypes.Type.APPLICATION_JSON.asString());
		dosFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
	}
}
