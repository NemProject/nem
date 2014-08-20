package org.nem.core.deploy;

import org.apache.commons.cli.*;

import java.util.List;
import java.util.logging.Logger;

/**
 * Represents the command line parameters passed to common starters main method.
 */
public class NemCommandLine {
	private static final Logger LOGGER = Logger.getLogger(NemCommandLine.class.getName());

	private final Options options = new Options();
	private CommandLine commandLine;

	/**
	 * Creates a new nem command line.
	 *
	 * @param parameters The list of possible parameters.
	 */
	public NemCommandLine(final List<Option> parameters) {
		parameters.stream().forEach(this.options::addOption);
	}

	/**
	 * Gets the number of options.
	 *
	 * @return The number of options.
	 */
	public int optionsSize() {
		return options.getOptions().size();
	}

	/**
	 * Parses the command line arguments.
	 *
	 * @param parameters The command line arguments.
	 */
	public boolean parse(final String[] parameters) {
		final CommandLineParser parser = new BasicParser();
		try {
			commandLine = parser.parse(options, parameters);
			return true;
		} catch (final ParseException ex) {
			LOGGER.warning(String.format("parameters could not be parsed: %s", ex.toString()));
			return false;
		}
	}

	/**
	 * Indicates if the given parameter is available.
	 *
	 * @param paramName The parameter name.
	 * @return true if the parameter is available, false otherwise.
	 */
	public boolean hasParameter(final String paramName) {
		return commandLine.hasOption(paramName);
	}

	/**
	 * Gets the parameter with the given name.
	 *
	 * @param paramName The parameter name.
	 * @return The parameter if available, null otherwise.
	 */
	public String getParameter(final String paramName) {
		return commandLine.getOptionValue(paramName);
	}
}
