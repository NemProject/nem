package org.nem.deploy;

import org.nem.core.utils.*;

import java.io.*;
import java.util.Properties;
import java.util.logging.*;

/**
 * Static class that is used to bootstrap the logging subsystem.
 */
public class LoggingBootstrapper {
	private static final Logger LOGGER = Logger.getLogger(LoggingBootstrapper.class.getName());

	/**
	 * Bootstraps the logging subsystem.
	 *
	 * @param nemFolder The NEM folder location.
	 */
	public static void bootstrap(final String nemFolder) {
		try (final InputStream inputStream = LoggingBootstrapper.class.getClassLoader().getResourceAsStream("logalpha.properties");
				final InputStream inputStringStream = adaptFileLocation(inputStream, nemFolder)) {
			final LogManager logManager = LogManager.getLogManager();
			logManager.readConfiguration(inputStringStream);
			final File logFile = new File(logManager.getProperty("java.util.logging.FileHandler.pattern"));
			final File logDirectory = new File(logFile.getParent());
			if (!logDirectory.exists() && !logDirectory.mkdirs()) {
				throw new IOException(String.format("unable to create log directory %s", logDirectory));
			}

			LOGGER.info("NEM logging has been bootstrapped!");
		} catch (final IOException e) {
			LOGGER.severe("Could not load default logging properties file");
			LOGGER.severe(e.toString());
		}
	}

	/**
	 * log configuration may include a placeholder for the nem folder The method replaces the pattern ${nem.folder} with the value defined
	 * within the NisConfiguration Only for "java.util.logging.FileHandler.pattern" value
	 *
	 * @param inputStream stream of the logging properties.
	 * @param nemFolder The NEM folder location.
	 * @return new stream which contains the replaced FileHandler.pattern
	 * @throws IOException
	 */
	private static InputStream adaptFileLocation(final InputStream inputStream, final String nemFolder) throws IOException {
		final Properties props = new Properties();
		props.load(inputStream);
		final String tmpStr = props.getProperty("java.util.logging.FileHandler.pattern");
		props.setProperty("java.util.logging.FileHandler.pattern", StringUtils.replaceVariable(tmpStr, "nem.folder", nemFolder));
		final StringWriter stringWriter = new StringWriter();
		props.store(stringWriter, null);
		return new ByteArrayInputStream(StringEncoder.getBytes(stringWriter.toString()));
	}
}
