package org.nem.core.deploy;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.server.*;
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
 * Did not find a better way of launching Jetty in combination with WebStart. The physical location of the downloaded files is not pre-known, so passing a WAR
 * file to the Jetty runner does not work.
 * <p/>
 * I had to switch to the Servlet API 3.x with programmatic configuration.
 */

/**
 * Simple jetty bootstrapper
 */
@WebListener
public class CommonStarter implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(CommonStarter.class.getName());

    private static final int IDLE_TIMEOUT = 30000;
    private static final int HTTPS_HEADER_SIZE = 8192;
    private static final int HTTPS_BUFFER_SIZE = 32768;

    public static final TimeProvider TIME_PROVIDER = new SystemTimeProvider();
    public static final ApplicationMetaData META_DATA = MetaDataFactory.loadApplicationMetaData(CommonStarter.class, TIME_PROVIDER);
    public static final CommonStarter INSTANCE = new CommonStarter();
    private Server server;
    private static Properties configProps;
    private long startTime;

    static {
        // initialize logging before anything is logged; otherwise not all
        // settings will take effect
        loadConfigurationProperties();
        initializeLogging();
    }

    private static  String getDefaultFolder() {
        return System.getProperty("user.home");
    }

    private static void loadConfigurationProperties() {
        ExceptionUtils.propagate(() -> {
            try (final InputStream inputStream = org.nem.core.deploy.CommonStarter.class.getClassLoader().getResourceAsStream("config.properties")) {
                INSTANCE.configProps = new Properties();
                INSTANCE.configProps.load(inputStream);
                return configProps;
            }
        }, IllegalStateException::new);
    }

    public static void main(String[] args) {
        LOGGER.info("Starting embedded Jetty Server.");

        try {
            INSTANCE.boot();
            INSTANCE.server.join();
        } catch (final InterruptedException e) {
            LOGGER.log(Level.INFO, "Received signal to shutdown.");
        } catch (Exception e) {
            //
            LOGGER.log(Level.SEVERE, "Stopping Jetty Server.", e);
        } finally {
            // Last chance to save configuration
            LOGGER.info(String.format("%s %s shutdown...", CommonStarter.META_DATA.getAppName(), CommonStarter.META_DATA.getVersion()));
        }

        System.exit(1);
    }

    private static void initializeLogging() {
        try (final InputStream inputStream = CommonStarter.class.getClassLoader().getResourceAsStream("logalpha.properties")) {

            final InputStream inputStringStream = adaptFileLocation(inputStream);
            final LogManager logManager = LogManager.getLogManager();
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
        final String nemFolder = INSTANCE.configProps.getProperty("nem.folder", getDefaultFolder()).replace("%h", getDefaultFolder());
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

    protected Server createServer() {
        // Taken from Jetty doc
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(Integer.valueOf(configProps.getProperty("nem.maxThreads")));
        final Server server = new Server(threadPool);
        server.addBean(new ScheduledExecutorScheduler());

        if (configProps.getProperty("nem.webApp").equals("1")) {
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

    public static Connector createConnector(Server server) {
        final HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(Integer.valueOf(configProps.getProperty("nem.httpsPort")));
        http_config.setOutputBufferSize(HTTPS_BUFFER_SIZE);
        http_config.setRequestHeaderSize(HTTPS_HEADER_SIZE);
        http_config.setResponseHeaderSize(HTTPS_HEADER_SIZE);
        http_config.setSendServerVersion(true);
        http_config.setSendDateHeader(false);

        final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
        http.setPort(Integer.valueOf(configProps.getProperty("nem.httpPort")));
        http.setIdleTimeout(IDLE_TIMEOUT);
        return http;
    }

    private void startServer(final Server server, final URL stopURL) throws Exception {
        try {
            server.start();
        } catch (final MultiException e) {
            long bindExceptions = e.getThrowables().stream().filter(t -> t instanceof BindException).count();

            if (bindExceptions > 0) {
                LOGGER.log(Level.WARNING, "Port already used, trying to shutdown other instance");
                // We assume it is already running?
                // Kill the old one
                stopOtherInstance(stopURL);

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
                                  configProps.getProperty("nem.url")));
        this.startTime = System.currentTimeMillis();
    }

    public void stopServer() {
        try {
            server.stop();
        } catch (final Exception e) {
            //
            LOGGER.log(Level.SEVERE, "Can't stop server.", e);
        }
    }

    private void stopOtherInstance(final URL stopURL) throws Exception {
        final HttpClient httpClient2 = new HttpClient();
        try {
            httpClient2.start();
            LOGGER.info("Send shutdown to other instance...");
            final ContentResponse response = httpClient2.GET(stopURL.toURI());
            if (response.getStatus() != HttpStatus.OK_200) {
                LOGGER.info(String.format("Other instance returned %d: %s", response.getStatus(), response.getContentAsString()));
            } else {
                LOGGER.info("Pause 2 seconds");
                Thread.sleep(2000);
            }
        } finally {
            httpClient2.stop();
        }
    }

    private void boot() throws Exception {
        server = createServer();
        server.addBean(new ScheduledExecutorScheduler());
        server.addConnector(createConnector(server));
        server.setHandler(createHandlers());
        server.setDumpAfterStart(false);
        server.setDumpBeforeStop(false);
        server.setStopAtShutdown(true);

		/*
		TODO: no time left to get this working :/
		this.startWebApplication(this.jettyServer);
		WebStartProxy.openWebBrowser(NCC_HOME_URL);
		NISController.startNISViaWebStart(this.nisJnlpUrl);
		*/

        LOGGER.info("Calling start().");
        final StringBuilder builder = new StringBuilder();
        builder.append(configProps.getProperty("nem.url"))
                .append(":")
                .append(configProps.getProperty("nem.httpPort"))
                .append(configProps.getProperty("nem.webContext"))
                .append(configProps.getProperty("nem.stopPath"));
        startServer(server, new URL(builder.toString()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        // nothing
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // This is the replacement for the web.xml
        // New with Servlet 3.0
        String appConfigClassName = String.format("%s%s%s", "org.nem.deploy.", configProps.getProperty("nem.shortServerName"), "AppConfig");
        String webAppInitializerClassName = String.format("%s%s%s", "org.nem.deploy.", configProps.getProperty("nem.shortServerName"), "WebAppInitializer");
        try {
            Class appConfigClass = Class.forName(appConfigClassName);
            Class appWebAppInitializerClass = Class.forName(webAppInitializerClassName);
            AnnotationConfigApplicationContext appCtx = new AnnotationConfigApplicationContext(appConfigClass);
            AnnotationConfigWebApplicationContext webCtx = new AnnotationConfigWebApplicationContext();
            webCtx.register(appWebAppInitializerClass);
            webCtx.setParent(appCtx);

            ServletContext context = event.getServletContext();
            ServletRegistration.Dynamic dispatcher = context.addServlet("Spring MVC Dispatcher Servlet", new DispatcherServlet(webCtx));
            dispatcher.setLoadOnStartup(1);
            dispatcher.addMapping(String.format("%s%s", configProps.getProperty("nem.apiContext"), "*"));

            context.setInitParameter("contextClass", "org.springframework.web.context.support.AnnotationConfigWebApplicationContext");

            if (configProps.getProperty("nem.shortServerName").toUpperCase().equals("NCC")) {
                ServletRegistration.Dynamic servlet = context.addServlet("FileServlet", "JarFileServlet");
                servlet.setInitParameter("maxCacheSize", "0");
                servlet.addMapping(String.format("%s%s", configProps.getProperty("nem.webContext"), "*"));
                servlet.setLoadOnStartup(1);

                servlet = context.addServlet("DefaultServlet","NccDefaultServlet");
                servlet.addMapping("/");
                servlet.setLoadOnStartup(1);
            }

            if (configProps.getProperty("nem.dosFilter").equals("1")) {
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
        } catch (Exception e) {
            throw new RuntimeException("Exception in contextInitialized");
        }
    }
}
