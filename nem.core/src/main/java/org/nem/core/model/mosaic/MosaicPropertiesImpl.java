package org.nem.core.model.mosaic;

import org.nem.core.model.*;
import org.nem.core.model.namespace.NamespaceId;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Class holding properties of a mosaic.
 */
public class MosaicPropertiesImpl implements MosaicProperties {
	private final NemProperties properties;

	/**
	 * Creates a new mosaic properties bag.
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

	/**
	 * Creates a new mosaic properties bag.
	 *
	 * @param properties The properties.
	 */
	public MosaicPropertiesImpl(final NemProperties properties) {
		if (null == properties) {
			throw new IllegalArgumentException("mosaic properties cannot be null");
		}

		this.properties = properties;
		this.validateProperties();
	}

	/**
	 * Creates a new mosaic properties bag.
	 *
	 * @param properties The list of nem property objects.
	 */
	public MosaicPropertiesImpl(final Collection<NemProperty> properties) {
		final Properties props = new Properties();
		properties.stream().forEach(p -> props.put(p.getName(), p.getValue()));
		this.properties = new NemProperties(props);
		this.validateProperties();
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
	public boolean isTransferable() {
		return this.properties.getOptionalBoolean("transferable", true);
	}

	@Override
	public Collection<NemProperty> asCollection() {
		return this.properties.asCollection();
	}

	private void validateProperties() {
		final int maxDivisibility = 6;
		final int divisibility = this.getDivisibility();
		if (0 > divisibility || maxDivisibility < divisibility) {
			throw new IllegalArgumentException(String.format("divisibility %d is out of range", maxDivisibility));
		}
	}
}
