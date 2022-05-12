package org.nem.deploy.server;

import org.eclipse.jetty.server.*;
import org.nem.deploy.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;

/**
 * A bootstrapper for booting a nem websock server.
 */
public class NemWebsockServerBootstrapper extends AbstractServerBootstrapper {
	private final AnnotationConfigApplicationContext appCtx;
	private final NemConfigurationPolicy configurationPolicy;

	/**
	 * Creates a bootstrapper.
	 *
	 * @param appCtx The app context.
	 * @param configuration The configuration.
	 * @param configurationPolicy The configuration policy.
	 */
	public NemWebsockServerBootstrapper(final AnnotationConfigApplicationContext appCtx, final CommonConfiguration configuration,
			final NemConfigurationPolicy configurationPolicy) {
		super(configuration);
		this.appCtx = appCtx;
		this.configurationPolicy = configurationPolicy;
	}

	@Override
	protected ServerConnector createConnector(final Server server) {
		final ServerConnector http = new ServerConnector(server);
		http.setPort(this.getConfiguration().getWebsocketPort());
		return http;
	}

	@Override
	protected ServletContextListener getCustomServletListener() {
		return new WebsocketContextListener(this.appCtx, this.getConfiguration(), this.configurationPolicy);
	}

	@WebListener
	private static class WebsocketContextListener extends AbstractNemServletContextListener {
		private final NemConfigurationPolicy configurationPolicy;

		public WebsocketContextListener(final AnnotationConfigApplicationContext appCtx, final CommonConfiguration configuration,
				final NemConfigurationPolicy configurationPolicy) {
			super(appCtx, configurationPolicy.getWebAppWebsockInitializerClass(), configuration.useDosFilter());
			this.configurationPolicy = configurationPolicy;
		}

		@Override
		protected void initialize(final AnnotationConfigWebApplicationContext webCtx, final ServletContext context) {
			final ServletRegistration.Dynamic dispatcher = context.addServlet("Spring Websocket Dispatcher Servlet",
					new DispatcherServlet(webCtx));
			dispatcher.addMapping(String.format("%s%s", "/w", "/*"));
			dispatcher.setLoadOnStartup(1);
		}
	}
}
