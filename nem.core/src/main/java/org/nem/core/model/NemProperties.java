package org.nem.core.model;

import org.nem.core.utils.StringUtils;

import java.util.Properties;

/**
 * A strongly typed NEM property bag.
 */
public class NemProperties {
	private final Properties properties;

	/**
	 * Creates a new property bag.
	 *
	 * @param properties The java properties.
	 */
	public NemProperties(final Properties properties) {
		this.properties = properties;
	}

	/**
	 * Gets the value of a required string property.
	 *
	 * @param name The property name.
	 * @return The property value.
	 */
	public String getString(final String name) {
		final String value = this.properties.getProperty(name);
		if (null == value) {
			throw new RuntimeException(String.format("property %s must not be null", name));
		}

		return value;
	}

	/**
	 * Gets the value of a required integer property.
	 *
	 * @param name The property name.
	 * @return The property value.
	 */
	public int getInteger(final String name) {
		return Integer.valueOf(this.getString(name));
	}

	/**
	 * Gets the value of a required long property.
	 *
	 * @param name The property name.
	 * @return The property value.
	 */
	public long getLong(final String name) {
		return Long.valueOf(this.getString(name));
	}

	/**
	 * Gets the value of a required boolean property.
	 *
	 * @param name The property name.
	 * @return The property value.
	 */
	public boolean getBoolean(final String name) {
		return parseBoolean(this.getString(name));
	}

	/**
	 * Gets the value of an optional string property.
	 *
	 * @param name The property name.
	 * @param defaultValue The default value to use in case there is no property value.
	 * @return The property value.
	 */
	public String getOptionalString(final String name, final String defaultValue) {
		final String value = this.properties.getProperty(name);
		return null == value ? defaultValue : value;
	}

	/**
	 * Gets the value of an optional integer property.
	 *
	 * @param name The property name.
	 * @param defaultValue The default value to use in case there is no property value.
	 * @return The property value.
	 */
	public int getOptionalInteger(final String name, final Integer defaultValue) {
		final String value = this.properties.getProperty(name);
		return null == value ? defaultValue : Integer.valueOf(value);
	}

	/**
	 * Gets the value of an optional long property.
	 *
	 * @param name The property name.
	 * @param defaultValue The default value to use in case there is no property value.
	 * @return The property value.
	 */
	public long getOptionalLong(final String name, final Long defaultValue) {
		final String value = this.properties.getProperty(name);
		return null == value ? defaultValue : Long.valueOf(value);
	}

	/**
	 * Gets the value of an optional boolean property.
	 *
	 * @param name The property name.
	 * @param defaultValue The default value to use in case there is no property value.
	 * @return The property value.
	 */
	public boolean getOptionalBoolean(final String name, final Boolean defaultValue) {
		final String value = this.properties.getProperty(name);
		return null == value ? defaultValue : parseBoolean(value);
	}

	/**
	 * Gets the value of an optional string array property.
	 *
	 * @param name The property name.
	 * @param defaultValue The default value to use in case there is no property value.
	 * @return The property value.
	 */
	public String[] getOptionalStringArray(final String name, final String defaultValue) {
		final String stringArray = this.getOptionalString(name, defaultValue);
		return StringUtils.isNullOrWhitespace(stringArray) ? new String[] {} : stringArray.split("\\|");
	}

	private static boolean parseBoolean(final String s) {
		// constrain false values to "false"
		if (s.equalsIgnoreCase("true")) {
			return true;
		} else if (s.equalsIgnoreCase("false")) {
			return false;
		}

		throw new RuntimeException(new NumberFormatException("string must be either true or false"));
	}
}
