package org.nem.deploy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.annotation.WebListener;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.springframework.web.context.ContextLoaderListener;

/**
 * Did not find a better way of launching Jetty in combination with WebStart. The
 * physical location of the downloaded files is not pre-known, so passing a WAR
 * file to the Jetty runner does not work.
 * <p/>
 * I had to switch to the Servlet API 3.x with programmatic configuration.
 *
 * @author Thies1965
 */

@WebListener
public class CommonStarter implements ServletContextListener {
	private static final Logger LOGGER = Logger.getLogger(CommonStarter.class.getName());


	private static ErrorHandler createErrorHandler() {
		ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
		errorHandler.addErrorPage(404, "/errors/404.html");
		return errorHandler;
	}

	public static void main(String[] args) throws Exception {
		LOGGER.info("Starting embedded Jetty Server.");

		// https://code.google.com/p/json-smart/wiki/ParserConfiguration
		//JSONParser.DEFAULT_PERMISSIVE_MODE = JSONParser.MODE_JSON_SIMPLE;

		//Taken from Jetty doc 
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(500);
		Server server = new Server(threadPool);
		server.addBean(new ScheduledExecutorScheduler());
		HttpConfiguration http_config = new HttpConfiguration();
		http_config.setSecureScheme("https");
		//PORT
		http_config.setSecurePort(7891);
		http_config.setOutputBufferSize(32768);
		http_config.setRequestHeaderSize(8192);
		http_config.setResponseHeaderSize(8192);
		http_config.setSendServerVersion(true);
		http_config.setSendDateHeader(false);

		HandlerCollection handlers = new HandlerCollection();
		ServletContextHandler servletContext = new ServletContextHandler();

		//Special Listener to set-up the environment for Spring
		servletContext.addEventListener(new CommonStarter());
		servletContext.addEventListener(new ContextLoaderListener());
		servletContext.setErrorHandler(createErrorHandler());

		handlers.setHandlers(new Handler[] { servletContext });
		server.setHandler(handlers);
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		server.setStopAtShutdown(true);
		ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
		http.setPort(7890);
		http.setIdleTimeout(30000);
		server.addConnector(http);

		LOGGER.info("Calling start().");
		server.start();

		openStartPage();
		server.join();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean openStartPage() {
		boolean result = false;
		// Let the show start without binding statically to JNLP
		//We first try to get the JNLP Service Manager
		Class jnlpServiceManager;
		Class jnlpBasicService;
		try {
			jnlpServiceManager = Class.forName("javax.jnlp.ServiceManager");
			jnlpBasicService = Class.forName("javax.jnlp.BasicService");

			Method lookup = jnlpServiceManager.getMethod("lookup", new Class[] { String.class });
			Method showDocument = jnlpBasicService.getMethod("showDocument", new Class[] { URL.class });

			Object basicService = lookup.invoke(jnlpServiceManager, "javax.jnlp.BasicService");
			URL homeURL = new URL("http://127.0.0.1:7890/peer");
			showDocument.invoke(basicService, homeURL);

			result = true;
		} catch (ClassNotFoundException | NoClassDefFoundError ex) {
			// handle exception case
			LOGGER.info("JNLP not available, not started via WebStart. Assuming headless run.");
		} catch (InvocationTargetException e) {
			LOGGER.log(Level.INFO, "WebStart services failed: <" + e.getCause().getMessage() + ">. Not started via WebStart. Assuming headless run.");
		} catch (NoSuchMethodException | IllegalArgumentException | IllegalAccessException | SecurityException e) {
			LOGGER.log(Level.SEVERE, "Method reflection failed.", e);
		} catch (MalformedURLException e) {
			LOGGER.log(Level.SEVERE, "home URL incorrect", e);
		}

		return result;
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// nothing
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		// This is the replacement for the web.xml
		// New with Servlet 3.0
		ServletContext context = event.getServletContext();
		//context.addListener(org.springframework.web.context.ContextLoaderListener.class);
		//context.addListener("org.springframework.web.context.ContextLoaderListener");
		context.setInitParameter("contextConfigLocation", "classpath:application-context.xml");
		Dynamic springServlet = context.addServlet("Spring MVC Dispatcher Servlet", "org.springframework.web.servlet.DispatcherServlet");
		springServlet.setInitParameter("contextConfigLocation", "classpath:web-context.xml");
		springServlet.addMapping("/");

		// Denial of server Filter
		javax.servlet.FilterRegistration.Dynamic dosFilter = context.addFilter("DoSFilter", "org.eclipse.jetty.servlets.DoSFilter");
		dosFilter.setInitParameter("delayMs", "1000");
		dosFilter.setInitParameter("trackSessions", "false");
		dosFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

		// GZIP filter
		dosFilter = context.addFilter("GzipFilter", "org.eclipse.jetty.servlets.GzipFilter");
		dosFilter.setInitParameter("mimeTypes",
				"text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,image/svg+xml");
		dosFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
	}
}
