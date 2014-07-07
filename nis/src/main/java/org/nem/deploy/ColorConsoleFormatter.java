package org.nem.deploy;

import java.util.*;
import java.util.logging.*;

/**
 * A custom formatter that enables colored console output on *nix platforms.
 */
public class ColorConsoleFormatter extends SimpleFormatter {

	private final MessageFormatter impl;

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

	private static interface MessageFormatter {
		public String format(final LogRecord record);
	}

	private static class NixMessageFormatter implements MessageFormatter {
		private static final String POSTFIX = "\u001b[0m";
		private static final Map<Level, String> LEVEL_TO_PREFIX_MAP = new HashMap<Level, String>() {
			{
				put(Level.SEVERE, "\u001b[0;31m");
				put(Level.WARNING, "[0;33m");
				put(Level.INFO, "[0;32m");
				put(Level.CONFIG, "[0;37m");
				put(Level.FINE, "[0;34m");
				put(Level.FINER, "[0;34m");
				put(Level.FINEST, "[0;34m");
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
