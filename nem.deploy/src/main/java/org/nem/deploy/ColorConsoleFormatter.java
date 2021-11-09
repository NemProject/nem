package org.nem.deploy;

import java.util.*;
import java.util.logging.*;

/**
 * A custom formatter that enables colored console output on *nix platforms.
 */
public class ColorConsoleFormatter extends NemFormatter {

	private final MessageFormatter impl;

	/**
	 * Creates a new formatter.
	 */
	public ColorConsoleFormatter() {
		this.impl = isWindows() ? new WindowsMessageFormatter() : new NixMessageFormatter();
	}

	private static boolean isWindows() {
		final String os = System.getProperty("os.name");
		return null != os && os.startsWith("Windows");
	}

	@Override
	public String formatMessage(final LogRecord record) {
		return this.impl.format(record);
	}

	private interface MessageFormatter {
		String format(final LogRecord record);
	}

	private static class NixMessageFormatter implements MessageFormatter {
		private static final String POSTFIX = "\u001b[0m";
		@SuppressWarnings("serial")
		private static final Map<Level, String> LEVEL_TO_PREFIX_MAP = new HashMap<Level, String>() {
			{
				this.put(Level.SEVERE, "\u001b[0;31m");
				this.put(Level.WARNING, "[0;33m");
				this.put(Level.INFO, "[0;32m");
				this.put(Level.CONFIG, "[0;37m");
				this.put(Level.FINE, "[0;34m");
				this.put(Level.FINER, "[0;34m");
				this.put(Level.FINEST, "[0;34m");
			}
		};
		private static final String DEFAULT_COLOR_PREFIX = LEVEL_TO_PREFIX_MAP.get(Level.SEVERE);

		@Override
		public String format(final LogRecord record) {
			final String prefix = LEVEL_TO_PREFIX_MAP.getOrDefault(record.getLevel(), DEFAULT_COLOR_PREFIX);
			return prefix + record.getMessage() + POSTFIX;
		}
	}

	private static class WindowsMessageFormatter implements MessageFormatter {
		@Override
		public String format(final LogRecord record) {
			return record.getMessage();
		}
	}
}
