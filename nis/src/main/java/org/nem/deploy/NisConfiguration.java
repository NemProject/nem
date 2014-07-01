package org.nem.deploy;

import org.nem.core.utils.HexEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

// TODO: should we allow "on-line" changes in config file?
public class NisConfiguration {
	private static final Logger LOGGER = Logger.getLogger(NisConfiguration.class.getName());

	private final boolean autoBoot;
	private final Integer nodeLimit;

	private byte[] bootKey;

	public NisConfiguration() {
		final InputStream inputStream = CommonStarter.class.getClassLoader().getResourceAsStream("config.properties");
		final Properties properties = new Properties();
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			LOGGER.severe(e.toString());
		}
		this.autoBoot = Boolean.valueOf(properties.getProperty("nis.autoboot"));
		this.bootKey = HexEncoder.getBytes(properties.getProperty("nis.bootkey"));
		this.nodeLimit = Integer.valueOf(properties.getProperty("nis.nodelimit"));
	}

	public void flushKeyPass() {
		for (int i = 0; i < bootKey.length; ++i) {
			bootKey[i] = 0;
		}
	}

	public boolean isAutoBoot() {
		return autoBoot;
	}

	public Integer getNodeLimit() {
		return nodeLimit;
	}

	public byte[] getBootKey() {
		return bootKey;
	}
}
