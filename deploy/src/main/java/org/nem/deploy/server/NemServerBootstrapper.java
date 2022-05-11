package org.nem.deploy.server;

import org.eclipse.jetty.server.*;
import org.nem.deploy.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServlet;

/**
 * A bootstrapper for booting a nem server.
 */
public class NemServerBootstrapper extends AbstractServerBootstrapper {
	private static final int HTTPS_HEADER_SIZE = 8192;
	private static final int HTTPS_BUFFER_SIZE = 32768;

	private final AnnotationConfigApplicationContext appCtx;
	private final NemConfigurationPolicy configurationPolicy;

	/**
	 * Creates a bootstrapper.
	 *
	 * @param appCtx The app context.
	 * @param configuration The configuration.
	 * @param configurationPolicy The configuration policy.
	 */
	public NemServerBootstrapper(final AnnotationConfigApplicationContext appCtx, final CommonConfiguration configuration,
			final NemConfigurationPolicy configurationPolicy) {
		super(configuration);
		this.appCtx = appCtx;
		this.configurationPolicy = configurationPolicy;
	}

	@Override
	protected ServerConnector createConnector(final Server server) {
		final HttpConfiguration http_config = new HttpConfiguration();
		http_config.setSecureScheme("https");
		http_config.setSecurePort(this.getConfiguration().getHttpsPort());
		http_config.setOutputBufferSize(HTTPS_BUFFER_SIZE);
		http_config.setRequestHeaderSize(HTTPS_HEADER_SIZE);
		http_config.setResponseHeaderSize(HTTPS_HEADER_SIZE);
		http_config.setSendServerVersion(true);
		http_config.setSendDateHeader(false);

		final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
		http.setPort(this.getConfiguration().getHttpPort());
		return http;
	}

	@Override
	protected ServletContextListener getCustomServletListener() {
		return new ServiceContextListener(this.appCtx, this.getConfiguration(), this.configurationPolicy);
	}

	@WebListener
	private static class ServiceContextListener extends AbstractNemServletContextListener {
		private final CommonConfiguration configuration;
		private final NemConfigurationPolicy configurationPolicy;

		public ServiceContextListener(final AnnotationConfigApplicationContext appCtx, final CommonConfiguration configuration,
				final NemConfigurationPolicy configurationPolicy) {
			super(appCtx, configurationPolicy.getWebAppInitializerClass(), configuration.useDosFilter());
			this.configuration = configuration;
			this.configurationPolicy = configurationPolicy;
		}

		@Override
		protected void initialize(final AnnotationConfigWebApplicationContext webCtx, final ServletContext context) {
			final ServletRegistration.Dynamic dispatcher = context.addServlet("Spring MVC Dispatcher Servlet",
					new DispatcherServlet(webCtx));
			dispatcher.setLoadOnStartup(1);
			dispatcher.addMapping(String.format("%s%s", this.configuration.getApiContext(), "/*"));

			if (this.configuration.isNcc()) {
				final String contextMapping = String.format("%s/*", this.configuration.getWebContext());
				addFileServlet(context, this.configurationPolicy.getJarFileServletClass(), contextMapping);
				addRootServlet(context, this.configurationPolicy.getRootServletClass());
			}
		}

		private static void addRootServlet(final ServletContext context, final Class<? extends HttpServlet> defaultServletClass) {
			final ServletRegistration.Dynamic servlet = context.addServlet("RootServlet", defaultServletClass);
			servlet.addMapping("/");
			servlet.setLoadOnStartup(1);
		}
	}
}
