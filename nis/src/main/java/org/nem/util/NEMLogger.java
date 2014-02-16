package org.nem.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Just for convenience, for a better access to the configured logger.
 * 
 * @author Thies1965
 * 
 */
public class NEMLogger {
	static public Logger LOG;

	static public void initializeLogger(String name) {
		try {
			String strLogPath = "logging.properties";
			LogManager.getLogManager().readConfiguration(NEMLogger.class.getClassLoader().getResourceAsStream(strLogPath));
//			File fileLog = new File(strLogPath);
//			String str = fileLog.getAbsolutePath();
//			LogManager.getLogManager().readConfiguration(new FileInputStream(fileLog));

		} catch (Exception e) {
			e.printStackTrace();
		}
		LOG = Logger.getLogger(name);
	}
}
