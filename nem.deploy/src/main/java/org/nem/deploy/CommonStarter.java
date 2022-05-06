package org.nem.deploy;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.MultiException;
import org.nem.core.metadata.*;
import org.nem.core.time.*;
import org.nem.core.utils.*;
import org.nem.deploy.server.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;
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

	private static final long ASYNC_SHUTDOWN_DELAY = 200;

	private static final Closeable FILE_LOCK_HANDLE;

	private AnnotationConfigApplicationContext appCtx;
	private NemConfigurationPolicy configurationPolicy;

	private final Collection<Server> servers = new ArrayList<>();

	static {
		// initialize logging before anything is logged; otherwise not all
		// settings will take effect
		final CommonConfiguration configuration = new CommonConfiguration();
		LoggingBootstrapper.bootstrap(configuration.getNemFolder());

		final File lockFile = Paths.get(configuration.getNemFolder(), configuration.getShortServerName().toLowerCase() + ".lock").toFile();
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
		INSTANCE.bootAndWait(args);
		System.exit(1);
	}

	private void bootAndWait(final String[] args) {
		try {
			this.boot(args);
			for (final Server server : this.servers) {
				server.join();
			}
		} catch (final InterruptedException e) {
			LOGGER.log(Level.INFO, "Received signal to shutdown.");
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Stopping Jetty Server.", e);
		} finally {
			// Last chance to save configuration
			LOGGER.info(String.format("%s %s shutdown...", META_DATA.getAppName(), META_DATA.getVersion()));
		}
	}

	private void initializeConfigurationPolicy() {
		this.appCtx = new AnnotationConfigApplicationContext("org.nem.specific.deploy.appconfig");
		this.configurationPolicy = this.appCtx.getBean(NemConfigurationPolicy.class);
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
			for (final Server server : this.servers) {
				server.stop();
			}
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

		final CommonConfiguration configuration = this.configurationPolicy.loadConfig(args);

		if (null != this.configurationPolicy.getWebAppWebsockInitializerClass()) {
			LOGGER.info("Calling websocket start().");
			final Server server = new NemWebsockServerBootstrapper(this.appCtx, configuration, this.configurationPolicy).boot();
			this.start(server, configuration);
		}

		if (null != this.configurationPolicy.getWebAppInitializerClass()) {
			LOGGER.info("Calling start().");
			final Server server = new NemServerBootstrapper(this.appCtx, configuration, this.configurationPolicy).boot();
			this.start(server, configuration);
		}
	}

	private void start(final Server server, final CommonConfiguration configuration) throws Exception {
		this.startServer(server, new URL(configuration.getShutdownUrl()));
		this.servers.add(server);
	}
}
