package org.nem.core.model.mosaic;

import org.nem.core.model.NemProperties;
import org.nem.core.model.namespace.NamespaceId;

import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Class holding properties of a mosaic.
 */
public class MosaicPropertiesImpl implements MosaicProperties {
	private static final Pattern IsValidPattern = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9 '_-]*");

	private final NemProperties properties;

	/**
	 * Creates a new mosaic properties bag
	 *
	 * @param properties The properties.
	 */
	public MosaicPropertiesImpl(final Properties properties) {
		if (null == properties) {
			throw new IllegalArgumentException("mosaic properties cannot be null");
		}

		this.properties = new NemProperties(properties);
		this.validateProperties();
	}

	@Override
	public String getDescription() {
		return this.properties.getOptionalString("description", "No description available");
	}

	@Override
	public int getDivisibility() {
		return this.properties.getOptionalInteger("divisibility", 0);
	}

	@Override
	public boolean isQuantityMutable() {
		return this.properties.getOptionalBoolean("mutableQuantity", false);
	}

	@Override
	public String getName() {
		return this.properties.getString("name");
	}

	@Override
	public NamespaceId getNamespaceId() {
		return new NamespaceId(this.properties.getString("namespace"));
	}

	@Override
	public boolean isTransferable() {
		return this.properties.getOptionalBoolean("transferable", true);
	}

	private void validateProperties() {
		// TODO 20150629 BR -> all: what limits should we use?
		final int maxDescriptionLength = 128;
		final int maxDivisibility = 6;
		final int maxNameLength = 32;
		if (maxDescriptionLength < this.getDescription().length()) {
			throw new IllegalArgumentException(String.format("description exceeds max length of %d characters", maxDescriptionLength));
		}

		final int divisibility = this.getDivisibility();
		if (0 > divisibility || maxDivisibility < divisibility) {
			throw new IllegalArgumentException(String.format("max divisibility %d exceeded", maxDivisibility));
		}

		final String name = this.getName();
		if (!IsValidPattern.matcher(name).matches() || maxNameLength < name.length() || name.isEmpty()) {
			throw new IllegalArgumentException(String.format("'%s' is not a valid mosaic name", name));
		}

		// NamespaceId ctor does the checks
		this.getNamespaceId();
	}
}
