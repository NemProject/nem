package org.nem.core.test;

import org.nem.core.model.NemProperties;

import java.util.*;

/**
 * Mock NemProperties that captures the required and optional property names.
 */
public class MockNemProperties extends NemProperties {
	private final Set<String> requiredPropertyNames = new HashSet<>();
	private final Set<String> optionalPropertyNames = new HashSet<>();

	/**
	 * Creates a new property bag.
	 *
	 * @param properties The java properties.
	 */
	public MockNemProperties(final Properties properties) {
		super(properties);
	}

	/**
	 * Gets the required property names.
	 *
	 * @return The required property names.
	 */
	public Collection<String> getRequiredPropertyNames() {
		return this.requiredPropertyNames;
	}

	/**
	 * Gets the optional property names.
	 *
	 * @return The optional property names.
	 */
	public Collection<String> getOptionalPropertyNames() {
		return this.optionalPropertyNames;
	}

	@Override
	public String getString(final String name) {
		this.requiredPropertyNames.add(name);
		return super.getString(name);
	}

	@Override
	public int getInteger(final String name) {
		this.requiredPropertyNames.add(name);
		return super.getInteger(name);
	}

	@Override
	public String getOptionalString(final String name, final String defaultValue) {
		this.optionalPropertyNames.add(name);
		return super.getOptionalString(name, defaultValue);
	}

	@Override
	public int getOptionalInteger(final String name, final Integer defaultValue) {
		this.optionalPropertyNames.add(name);
		return super.getOptionalInteger(name, defaultValue);
	}

	@Override
	public boolean getOptionalBoolean(final String name, final Boolean defaultValue) {
		this.optionalPropertyNames.add(name);
		return super.getOptionalBoolean(name, defaultValue);
	}

	@Override
	public String[] getOptionalStringArray(final String name, final String defaultValue) {
		this.optionalPropertyNames.add(name);
		return super.getOptionalStringArray(name, defaultValue);
	}
}
