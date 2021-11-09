package org.nem.deploy.server;

import org.eclipse.jetty.http.MimeTypes;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.EnumSet;

/**
 * Abstract servlet context listener used to implement nem listeners.
 */
public abstract class AbstractNemServletContextListener implements ServletContextListener {
	private final AnnotationConfigApplicationContext appCtx;
	private final Class<?> webAppInitializerClass;
	private final boolean useDosFilter;

	/**
	 * Creates a listener.
	 *
	 * @param appCtx The app context.
	 * @param webAppInitializerClass The web initializer class.
	 * @param useDosFilter true if a DOS filter should be used, false otherwise.
	 */
	public AbstractNemServletContextListener(final AnnotationConfigApplicationContext appCtx, final Class<?> webAppInitializerClass,
			final boolean useDosFilter) {
		this.appCtx = appCtx;
		this.webAppInitializerClass = webAppInitializerClass;
		this.useDosFilter = useDosFilter;
	}

	// region ServletContextListener

	@Override
	public void contextInitialized(final ServletContextEvent event) {
		try {
			final AnnotationConfigWebApplicationContext webCtx = new AnnotationConfigWebApplicationContext();

			webCtx.register(this.webAppInitializerClass);
			webCtx.setParent(this.appCtx);

			final ServletContext context = event.getServletContext();
			this.initialize(webCtx, context);

			context.setInitParameter("contextClass", "org.springframework.web.context.support.AnnotationConfigWebApplicationContext");

			if (this.useDosFilter) {
				addDosFilter(context);
			}

			addGzipFilter(context);
			addCorsFilter(context);
		} catch (final Exception e) {
			throw new RuntimeException(String.format("Exception in contextInitialized: %s", e.toString()), e);
		}
	}

	// endregion

	/**
	 * Allows the derived class to customize the server in a custom way.
	 *
	 * @param webCtx The web context.
	 * @param context The servlet context.
	 */
	protected abstract void initialize(final AnnotationConfigWebApplicationContext webCtx, final ServletContext context);

	@Override
	public void contextDestroyed(final ServletContextEvent event) {
	}

	// region add servlet

	/**
	 * Adds a file serving servlet. TODO 20150918 J-J: this should be moved to the derived class when NIS no longer is serving files.
	 *
	 * @param context The servlet context.
	 * @param fileServletClass The file servlet class.
	 * @param mappings The file mappings.
	 */
	protected static void addFileServlet(final ServletContext context, final Class<? extends HttpServlet> fileServletClass,
			final String... mappings) {
		final ServletRegistration.Dynamic servlet = context.addServlet("FileServlet", fileServletClass);
		servlet.setInitParameter("maxCacheSize", "0");
		servlet.addMapping(mappings);
		servlet.setLoadOnStartup(1);
	}

	// endregion

	// region add filter

	private static void addDosFilter(final ServletContext context) {
		final javax.servlet.FilterRegistration.Dynamic filter = context.addFilter("DoSFilter", "org.eclipse.jetty.servlets.DoSFilter");
		filter.setInitParameter("maxRequestsPerSec", "50");
		filter.setInitParameter("delayMs", "-1");
		filter.setInitParameter("trackSessions", "false");
		filter.setInitParameter("maxRequestMs", "120000");
		filter.setInitParameter("ipWhitelist", "127.0.0.1");
		filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
	}

	private static void addGzipFilter(final ServletContext context) {
		final javax.servlet.FilterRegistration.Dynamic filter = context.addFilter("GzipFilter", "org.eclipse.jetty.servlets.GzipFilter");
		filter.setInitParameter("mimeTypes", MimeTypes.Type.APPLICATION_JSON.asString()); // only zip json
		filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
	}

	private static void addCorsFilter(final ServletContext context) {
		final javax.servlet.FilterRegistration.Dynamic filter = context.addFilter("cors filter", new Filter() {
			@Override
			public void init(final FilterConfig filterConfig) {
			}

			@Override
			public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
					throws IOException, ServletException {
				final HttpServletResponse httpResponse = (HttpServletResponse) response;
				httpResponse.setHeader("Access-Control-Allow-Origin", "*");
				httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type");
				chain.doFilter(request, httpResponse);
			}

			@Override
			public void destroy() {
			}
		});

		filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
	}

	// endregion
}
