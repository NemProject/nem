package org.nem.deploy;

import java.util.*;
import java.util.logging.*;

/**
 * A custom ConsoleHandler implementation that outputs different level messages
 * as different colors.
 *
 * Note that this will not work on Windows.
 */
public class ColorConsoleHandler extends ConsoleHandler {
	private static final String POSTFIX = "\u001b[0m";
	private static final Map<Level, String> LEVEL_TO_PREFIX_MAP = createLevelToPrefixMap();
	private static final String DEFAULT_COLOR_PREFIX = LEVEL_TO_PREFIX_MAP.get(Level.SEVERE);

	@Override
	public void publish(final LogRecord record) {
		final String prefix = createLevelToPrefixMap().getOrDefault(record.getLevel(), DEFAULT_COLOR_PREFIX);
		record.setMessage(prefix + record.getMessage() + POSTFIX);
		super.publish(record);
	}

	private static Map<Level, String> createLevelToPrefixMap() {
		final Map<Level, String> map = new HashMap<>();
		map.put(Level.SEVERE, "\u001b[0;31m");
		map.put(Level.WARNING, "\u001b[0;33m");
		map.put(Level.INFO, "\u001b[0;32m");
		map.put(Level.CONFIG, "\u001b[0;37m");
		map.put(Level.FINE, "\u001b[0;34m");
		map.put(Level.FINER, "\u001b[0;34m");
		map.put(Level.FINEST, "\u001b[0;34m");
		return map;
	}
}