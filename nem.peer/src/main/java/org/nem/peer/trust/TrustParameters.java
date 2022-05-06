package org.nem.peer.trust;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Property bag that can contain arbitrary trust parameters.
 */
public class TrustParameters {

	private final ConcurrentHashMap<String, String> params = new ConcurrentHashMap<>();

	/**
	 * Sets a parameter.
	 *
	 * @param name The name of the parameter.
	 * @param value The value of the parameter.
	 */
	public void set(final String name, final String value) {
		this.params.put(name, value);
	}

	/**
	 * Gets a parameter value by its name.
	 *
	 * @param name The name of the parameter.
	 * @return The value of the parameter.
	 */
	public String get(final String name) {
		final String value = this.params.get(name);
		if (null == value) {
			throw new IllegalArgumentException(name + " parameter does not exist");
		}

		return value;
	}

	/**
	 * Gets a parameter value by its name or a default value.
	 *
	 * @param name The name of the parameter.
	 * @param defaultValue The default value.
	 * @return The value of the parameter.
	 */
	public String get(final String name, final String defaultValue) {
		final String value = this.params.get(name);
		return null == value ? defaultValue : value;
	}

	/**
	 * Gets an int parameter by its name.
	 *
	 * @param name The name of the parameter.
	 * @return The value of the parameter.
	 */
	public int getAsInteger(final String name) {
		return Integer.parseInt(this.get(name));
	}

	/**
	 * Gets an int parameter by its name.
	 *
	 * @param name The name of the parameter.
	 * @param defaultValue The default value.
	 * @return The value of the parameter.
	 */
	public int getAsInteger(final String name, final int defaultValue) {
		final String value = this.get(name, null);
		return null == value ? defaultValue : Integer.parseInt(value);
	}

	/**
	 * Gets a double parameter by its name.
	 *
	 * @param name The name of the parameter.
	 * @return The value of the parameter.
	 */
	public double getAsDouble(final String name) {
		return Double.parseDouble(this.get(name));
	}

	/**
	 * Gets a double parameter by its name.
	 *
	 * @param name The name of the parameter.
	 * @param defaultValue The default value.
	 * @return The value of the parameter.
	 */
	public double getAsDouble(final String name, final double defaultValue) {
		final String value = this.get(name, null);
		return null == value ? defaultValue : Double.parseDouble(value);
	}
}
